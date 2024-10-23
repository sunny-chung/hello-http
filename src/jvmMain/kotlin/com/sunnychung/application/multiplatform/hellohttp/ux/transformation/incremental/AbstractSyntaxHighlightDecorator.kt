package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.VisitScope
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.toPoint
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEventType
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import io.github.treesitter.ktreesitter.InputEdit
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Point
import io.github.treesitter.ktreesitter.Tree

abstract class AbstractSyntaxHighlightDecorator(language: Language) : BigTextDecorator {
    protected val parser: Parser = Parser(language)
    protected lateinit var ast: Tree

    override fun initialize(text: BigText) {
        val s = text.buildString()

//        val singleByteCharSequence = s.map { // buggy
//            if (it.code > 255) {
//                'X'
//            } else {
//                it
//            }
//        }.joinToString()
//        ast = parser.parse(singleByteCharSequence)

        ast = parser.parse { byte, point ->
            if (byte in 0u until text.length.toUInt()) {
                s.substring(byte.toInt() ..byte.toInt()).let {
                    val codePoints = it.codePoints().toArray()
                    if (codePoints.size > 1 || codePoints.first() > 255) {
                        "X" // replace multibyte char as single-byte char
                    } else {
                        it
                    }
                }
            } else {
                "" // the doc is wrong. null would result in crash
            }/*.also {
                println("parse $byte = '$it'")
            }*/
        }
    }

    protected fun createInputEdit(event: BigTextChangeEvent, startOffset: Int, oldEndOffset: Int, newEndOffset: Int): InputEdit {
        fun toPoint(offset: Int): Point {
            return event.bigText.findLineAndColumnFromRenderPosition(offset)
                .also {
                    require(it.first >= 0 && it.second >= 0) {
                        (event.bigText as BigTextImpl).printDebug("[ERROR]")
                        "convert out of range. i=$offset, lc=$it, s = |${event.bigText.buildString()}|"
                    }
                }
                .toPoint()
        }

        return InputEdit(
            startOffset.toUInt(),
            oldEndOffset.toUInt(),
            newEndOffset.toUInt(),
            toPoint(startOffset),
            toPoint(oldEndOffset),
            toPoint(newEndOffset),
        ).also {
            log.d { "AST InputEdit ${it.startByte} ${it.oldEndByte} ${it.newEndByte} ${it.startPoint} ${it.oldEndPoint} ${it.newEndPoint}" }
        }
    }

    protected fun createAnnotatedRange(style: SpanStyle, astNode: Node, bigTextRange: IntRange): AnnotatedString.Range<SpanStyle>? {
        val startCharIndex = maxOf(0, astNode.startByte.toInt() - bigTextRange.start)
        val endCharIndex = minOf(bigTextRange.length, maxOf(0, astNode.endByte.toInt() - bigTextRange.start))
        if (startCharIndex >= endCharIndex) {
            return null
        }
        return AnnotatedString.Range(style, startCharIndex, endCharIndex)
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        val oldAst = ast

        when (change.eventType) {
            BigTextChangeEventType.Insert -> {
                ast.edit(
                    createInputEdit(
                        change,
                        change.changeStartIndex,
                        change.changeStartIndex,
                        change.changeEndExclusiveIndex,
                    )
                )
            }

            BigTextChangeEventType.Delete -> {
                ast.edit(
                    createInputEdit(
                        change,
                        change.changeStartIndex,
                        change.changeEndExclusiveIndex,
                        change.changeStartIndex,
                    )
                )
            }
        }

        ast = parser.parse(oldAst) { byte, point ->
            if (byte in 0u until change.bigText.length.toUInt()) {
                change.bigText.substring(byte.toInt() ..byte.toInt()).let {
                    val codePoints = it.codePoints().toArray()
                    if (codePoints.size > 1 || codePoints.first() > 255) {
                        "X" // replace multibyte char as single-byte char
                    } else {
                        it
                    }
                }
            } else {
                "" // the doc is wrong. null would result in crash
            }.also {
                println("parse $byte = '$it'")
            }
        }

        log.v { "AST change sexp = ${ast.rootNode.sexp()}" }
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        log.v { "syntax hi onApplyDecoration ${originalRange}" }
        return highlight(text, originalRange) {
            it.children.forEach { c ->
                if ((c.startByte.toInt() until c.endByte.toInt()) hasIntersectWith originalRange) {
                    visit(c)
                }
            }
        }
    }

    protected abstract fun highlight(text: CharSequence, range: IntRange, visitChildrensFunction: VisitScope.(Node) -> Unit): AnnotatedString
}

fun Node.childrensByGrammarType(type: String): List<Node> {
    return children.filter { it.grammarType == type }
}

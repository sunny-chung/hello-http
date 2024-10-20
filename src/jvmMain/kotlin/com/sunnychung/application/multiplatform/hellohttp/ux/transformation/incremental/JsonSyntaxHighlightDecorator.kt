package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.VisitScope
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.toPoint
import com.sunnychung.application.multiplatform.hellohttp.util.visit
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEventType
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformer
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.IncrementalTextTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.github.treesitter.ktreesitter.InputEdit
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Point
import io.github.treesitter.ktreesitter.Range
import io.github.treesitter.ktreesitter.Tree
import io.github.treesitter.ktreesitter.json.TreeSitterJson

class JsonSyntaxHighlightDecorator(val colours: AppColor) : BigTextDecorator {
    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    val parser: Parser
    lateinit var ast: Tree

    init {
        val language = Language(TreeSitterJson.language())
        parser = Parser(language)
    }

    override fun initialize(text: BigText) {
        fun Point.toCharIndex(): Int {
            val charIndex = text.findRenderCharIndexByLineAndColumn(row.toInt(), column.toInt())
            return charIndex
        }

        // Tree Sitter incremental approach
        val s = text.buildString()

        // In Tree-sitter, multibyte utf8 characters would occupy multiple positions and have no workaround
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

//        ast.rootNode.children.forEach {
//            log.d { "AST init parse ${it.range} type=${it.type} grammarType=${it.grammarType} p=${it.parent}" }
//        }
//        log.d { "AST init sexp = ${ast.rootNode.sexp()}" }
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
        log.v { "json sh onApplyDecoration ${originalRange}" }
        return highlight(text, originalRange) {
            it.children.forEach { c ->
                if ((c.startByte.toInt() until c.endByte.toInt()) hasIntersectWith originalRange) {
                    visit(c)
                }
            }
        }
    }

    protected fun highlight(text: CharSequence, range: IntRange, visitChildrensFunction: VisitScope.(Node) -> Unit): AnnotatedString {
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        fun applyStyle(style: SpanStyle, node: Node) {
            createAnnotatedRange(style, node, range)?.let {
                spans += it
            }
        }

        ast.rootNode.visit {
            log.v { "AST visit change ${it.startByte} ..< ${it.endByte} = ${it.type}" }

            fun visitChildrens() {
                visitChildrensFunction(it)
            }

            when (it.type) {
                "pair" -> {
                    val keyChild = it.childByFieldName("key")!!
                    applyStyle(objectKeyStyle, keyChild)
//                        log.v { "AST change highlight ${keyChild.startByte} ..< ${keyChild.endByte} = key" }
//                    log.v { "AST highlight ${keyChild.startPoint.toCharIndex()} ..< ${keyChild.endPoint.toCharIndex()} = key" }
                    it.childByFieldName("value")?.let {
                        visit(it)
                    }
                }
                "null" -> {
                    applyStyle(nothingLiteralStyle, it)
                }
                "number" -> {
                    applyStyle(numberLiteralStyle, it)
                }
                "string" -> {
                    applyStyle(stringLiteralStyle, it)
                }
                "false" -> {
                    applyStyle(booleanFalseLiteralStyle, it)
                }
                "true" -> {
                    applyStyle(booleanTrueLiteralStyle, it)
                }
                else -> {
                    visitChildrens()
                }
            }
            log.v { "AST finish visit change ${it.startByte} ..< ${it.endByte}" }
        }

        return AnnotatedString(text.toString(), spans)
    }

    fun createInputEdit(event: BigTextChangeEvent, startOffset: Int, oldEndOffset: Int, newEndOffset: Int): InputEdit {
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

    fun createAnnotatedRange(style: SpanStyle, astNode: Node, bigTextRange: IntRange): AnnotatedString.Range<SpanStyle>? {
        val startCharIndex = maxOf(0, astNode.startByte.toInt() - bigTextRange.start)
        val endCharIndex = minOf(bigTextRange.length, maxOf(0, astNode.endByte.toInt() - bigTextRange.start))
//        val startCharIndex = text.findRenderCharIndexByLineAndColumn(astNode.startPoint.row.toInt(), astNode.startPoint.column.toInt())
//        val endCharIndex = text.findRenderCharIndexByLineAndColumn(astNode.endPoint.row.toInt(), astNode.endPoint.column.toInt())
        if (startCharIndex >= endCharIndex) {
            return null
        }
        return AnnotatedString.Range(style, startCharIndex, endCharIndex)
    }
}

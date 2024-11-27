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

class JsonSyntaxHighlightDecorator(val colours: AppColor) : AbstractSyntaxHighlightDecorator(Language(TreeSitterJson.language())) {
    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    override fun highlight(text: CharSequence, range: IntRange, visitChildrensFunction: VisitScope.(Node) -> Unit): AnnotatedString {
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
                "null" -> applyStyle(nothingLiteralStyle, it)
                "number" -> applyStyle(numberLiteralStyle, it)
                "string" -> applyStyle(stringLiteralStyle, it)
                "false" -> applyStyle(booleanFalseLiteralStyle, it)
                "true" -> applyStyle(booleanTrueLiteralStyle, it)
                else -> {
                    visitChildrens()
                }
            }
            log.v { "AST finish visit change ${it.startByte} ..< ${it.endByte}" }
        }

        return AnnotatedString(text.toString(), spans)
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        println("afterTextChange ${change.eventType} ${change.changeStartIndex} ..< ${change.changeEndExclusiveIndex}")
        super.afterTextChange(change)
        printDebug()
    }

    fun printDebug() {
        fun visit(node: Node, depth: Int) {
            println("${"-".repeat(depth)} ${node.type} ${node.range.startByte} ..< ${node.range.endByte}")
            node.children.forEach { visit(it, depth + 1) }
        }
        println("=====")
        visit(ast.rootNode, 1)
        println("=====")
        println()
    }
}

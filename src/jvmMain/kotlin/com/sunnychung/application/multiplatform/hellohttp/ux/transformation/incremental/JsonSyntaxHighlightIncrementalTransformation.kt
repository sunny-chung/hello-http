package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.sunnychung.application.multiplatform.hellohttp.extension.hasIntersectWith
import com.sunnychung.application.multiplatform.hellohttp.util.VisitScope
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.toPoint
import com.sunnychung.application.multiplatform.hellohttp.util.visit
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEventType
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
import kotlin.streams.toList

private val TOKEN_REGEX = "(?<!\\\\)(\".*?(?<!\\\\)\"(?:\\s*:)?)|(?<=[,\\[\\]{}:])\\s*([^,\\s\"\\[\\]{}]+?)\\s*(?=[,\\[\\]{}:])".toRegex()

private val OBJECT_KEY_REGEX = "(\".*?(?<!\\\\)\")\\s*:".toRegex()
private val STRING_LITERAL_REGEX = "\".*?(?<!\\\\)\"".toRegex()
private val NUMBER_LITERAL_REGEX = "-?\\d+(?:\\.\\d+)?".toRegex()
private val BOOLEAN_TRUE_LITERAL_REGEX = "true".toRegex()
private val BOOLEAN_FALSE_LITERAL_REGEX = "false".toRegex()
private val NOTHING_LITERAL_REGEX = "null|undefined".toRegex()

@Deprecated(message = "Use JsonSyntaxHighlightDecorator instead.", replaceWith = ReplaceWith("JsonSyntaxHighlightDecorator"))
class JsonSyntaxHighlightIncrementalTransformation(val colours: AppColor) : IncrementalTextTransformation<Unit> {
    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    val subPatterns = listOf(
        OBJECT_KEY_REGEX to objectKeyStyle,
        STRING_LITERAL_REGEX to stringLiteralStyle,
        NUMBER_LITERAL_REGEX to numberLiteralStyle,
        BOOLEAN_TRUE_LITERAL_REGEX to booleanTrueLiteralStyle,
        BOOLEAN_FALSE_LITERAL_REGEX to booleanFalseLiteralStyle,
        NOTHING_LITERAL_REGEX to nothingLiteralStyle,
    )

    val parser: Parser
    lateinit var ast: Tree

    init {
        val language = Language(TreeSitterJson.language())
        parser = Parser(language)
    }

    override fun initialize(text: BigText, transformer: BigTextTransformer) {
        /*val s = text.buildString()
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        TOKEN_REGEX.findAll(s).forEach { m ->
            val match = (m.groups[1] ?: m.groups[2])!!
            subPatterns.firstOrNull { (pattern, style) ->
                val subMatch = pattern.matchEntire(match.value)
                if (subMatch != null) {
                    val range = if (subMatch.groups.size > 1) {
                        subMatch.groups[1]!!.range
                            .let { it.start + match.range.start .. it.endInclusive + match.range.start }
                    } else {
                        match.range
                    }
                    spans += AnnotatedString.Range(style, range.start, range.endInclusive + 1)
                    true
                } else {
                    false
                }
            }
        }

        if (spans.isNotEmpty()) {
            transformer.replace(
                range = 0 until text.length,
                text = AnnotatedString(s, spans),
                offsetMapping = BigTextTransformOffsetMapping.Incremental
            )
        }*/

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
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        ast.rootNode.visit {
            log.v { "AST visit ${it.startByte} ..< ${it.endByte} = ${it.type}" }
//            log.v { "AST visit ${it.startPoint.toCharIndex()} ..< ${it.endPoint.toCharIndex()} = ${it.type}" }
            when (it.type) {
                "pair" -> {
                    val keyChild = it.childByFieldName("key")!!
                    spans += createAnnotatedRange(text, objectKeyStyle, keyChild)
                    log.v { "AST highlight ${keyChild.startByte} ..< ${keyChild.endByte} = key" }
//                    log.v { "AST highlight ${keyChild.startPoint.toCharIndex()} ..< ${keyChild.endPoint.toCharIndex()} = key" }
                    it.childByFieldName("value")?.let {
                        visit(it)
                    }
                }
                "null" -> {
                    spans += createAnnotatedRange(text, nothingLiteralStyle, it)
                }
                "number" -> {
                    spans += createAnnotatedRange(text, numberLiteralStyle, it)
                }
                "string" -> {
                    spans += createAnnotatedRange(text, stringLiteralStyle, it)
                }
                "false" -> {
                    spans += createAnnotatedRange(text, booleanFalseLiteralStyle, it)
                }
                "true" -> {
                    spans += createAnnotatedRange(text, booleanTrueLiteralStyle, it)
                }
                else -> {
                    visitChildrens()
                }
            }
        }
        if (spans.isNotEmpty()) {
            transformer.replace(
                range = 0 until text.length,
                text = AnnotatedString(s, spans),
                offsetMapping = BigTextTransformOffsetMapping.Incremental
            )
        }
    }

    override fun afterTextChange(change: BigTextChangeEvent, transformer: BigTextTransformer, context: Unit) {
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

        log.d { "AST change sexp = ${ast.rootNode.sexp()}" }

        val changedRanges = if (change.eventType == BigTextChangeEventType.Insert) { // if there is no structural change, `changedRanges` returns an empty list. but we need to update the display styles
            listOf(Range(Point(0u, 0u), Point(0u, 0u), change.changeStartIndex.toUInt(), change.changeEndExclusiveIndex.toUInt()))
        } else {
            emptyList()
        } +
            oldAst.changedRanges(ast)
        log.d { "AST changes (${changedRanges.size}) = ${changedRanges}" }

        changedRanges.forEach {
            transformer.restoreToOriginal(
                minOf(change.bigText.length - 1, it.startByte.toInt())
                        until
                        minOf(change.bigText.length, it.endByte.toInt())
            )
        }

        changedRanges.forEach { cr ->
//            ast.rootNode.descendant(it.startByte, it.endByte)?.let {
//                log.d { "AST change ${it.range} type=${it.type} grammarType=${it.grammarType} p=${it.parent} sexp=${it.sexp()}" }
//            }

            val cr = cr.startByte until cr.endByte

            highlight(change.bigText, transformer) {
                it.children.forEach { c ->
                    if ((c.startByte until c.endByte) hasIntersectWith cr) {
                        visit(c)
                    }
                }
            }
        }

        log.d { "AST change sexp after = ${ast.rootNode.sexp()}" }

    }

    override fun onReapplyTransform(text: BigText, originalRange: IntRange, transformer: BigTextTransformer, context: Unit) {
        log.d { "json sh onReapplyTransform ${originalRange}" }
//        transformer.layoutTransaction {
            highlight(text, transformer) {
                it.children.forEach { c ->
                    if ((c.startByte.toInt() until c.endByte.toInt()) hasIntersectWith originalRange) {
                        visit(c)
                    }
                }
            }
//            layout(originalRange.start, originalRange.endInclusive + 1)
//        }
        log.d { "no. of nodes = ${(transformer as BigTextTransformerImpl).tree.size()}" }
    }

    fun applyStyle(bigText: BigText, transformer: BigTextTransformer, style: SpanStyle, node: Node) {
        val startCharIndex: Int = node.startByte.toInt()
        val endCharIndexExclusive: Int = node.endByte.toInt()
//                val ar = AnnotatedString.Range(style, startCharIndex, endCharIndexExclusive)
        val startInstant = KInstant.now()
        transformer.replace(startCharIndex until endCharIndexExclusive, AnnotatedString(bigText.substring(startCharIndex until endCharIndexExclusive).toString(), style), BigTextTransformOffsetMapping.Incremental)
        log.d { "AST change highlight -- $startCharIndex ..< $endCharIndexExclusive -- ${KInstant.now() - startInstant}" }
    }

    protected fun highlight(bigText: BigText, transformer: BigTextTransformer, visitChildrensFunction: VisitScope.(Node) -> Unit) {
        ast.rootNode.visit {
            log.d { "AST visit change ${it.startByte} ..< ${it.endByte} = ${it.type}" }

            fun visitChildrens() {
                visitChildrensFunction(it)
            }

            when (it.type) {
                "pair" -> {
                    val keyChild = it.childByFieldName("key")!!
                    applyStyle(bigText, transformer, objectKeyStyle, keyChild)
//                        log.v { "AST change highlight ${keyChild.startByte} ..< ${keyChild.endByte} = key" }
//                    log.v { "AST highlight ${keyChild.startPoint.toCharIndex()} ..< ${keyChild.endPoint.toCharIndex()} = key" }
                    it.childByFieldName("value")?.let {
                        visit(it)
                    }
                }
                "null" -> {
                    applyStyle(bigText, transformer, nothingLiteralStyle, it)
                }
                "number" -> {
                    applyStyle(bigText, transformer, numberLiteralStyle, it)
                }
                "string" -> {
                    applyStyle(bigText, transformer, stringLiteralStyle, it)
                }
                "false" -> {
                    applyStyle(bigText, transformer, booleanFalseLiteralStyle, it)
                }
                "true" -> {
                    applyStyle(bigText, transformer, booleanTrueLiteralStyle, it)
                }
                else -> {
                    visitChildrens()
                }
            }
            log.d { "AST finish visit change ${it.startByte} ..< ${it.endByte}" }
        }
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

    fun createAnnotatedRange(text: BigText, style: SpanStyle, astNode: Node): AnnotatedString.Range<SpanStyle> {
        val startCharIndex = astNode.startByte.toInt()
        val endCharIndex = astNode.endByte.toInt()
//        val startCharIndex = text.findRenderCharIndexByLineAndColumn(astNode.startPoint.row.toInt(), astNode.startPoint.column.toInt())
//        val endCharIndex = text.findRenderCharIndexByLineAndColumn(astNode.endPoint.row.toInt(), astNode.endPoint.column.toInt())
        return AnnotatedString.Range(style, startCharIndex, endCharIndex)
    }


}

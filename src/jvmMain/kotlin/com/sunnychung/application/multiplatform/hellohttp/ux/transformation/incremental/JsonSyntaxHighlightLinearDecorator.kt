package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import co.touchlab.kermit.Severity
import com.dslplatform.json.DslJson
import com.sunnychung.application.multiplatform.hellohttp.error.ParseException
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.parser.JsonLexer
import com.sunnychung.application.multiplatform.hellohttp.parser.JsonNodeType
import com.sunnychung.application.multiplatform.hellohttp.parser.JsonParser3
import com.sunnychung.application.multiplatform.hellohttp.parser.TokenWithoutValue
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.string
import com.sunnychung.application.multiplatform.hellohttp.util.timeAndLog
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextChangeEvent
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.CacheableBigTextDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import java.io.IOException

class JsonSyntaxHighlightLinearDecorator(colours: AppColor) : CacheableBigTextDecorator() {
    private val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    private val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    private val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    private val booleanTrueLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    private val booleanFalseLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    private val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    private var tokens: List<TokenWithoutValue<JsonNodeType>> = emptyList()

    private val parser = DslJson<Any?>()

    fun parse(text: String) {
//        val doc = timeAndLog(Severity.Info, "parse linear json") {
//            try {
//                JsonParser2(text).document()
//            } catch (e: ParseException) {
//                log.w { "Parsing JSON failed: ${e.message}" }
//            }
//        }

//        timeAndLog(Severity.Info, "parse dsl json with overhead") {
//            val bytes = text.toByteArray()
//            timeAndLog(Severity.Info, "parse dsl json") {
//                try {
//                    val reader = parser.newReader().process(bytes, bytes.size)
//                    reader.nextToken
//                    ObjectConverter.deserializeObject(reader)
//                } catch (e: ParseException) {
//                    log.w { "Parsing JSON failed: ${e.message}" }
//                }
//            }
//        }

        timeAndLog(Severity.Info, "parse linear json 3") {
            val bytes = ByteArray(text.length)
            text.forEachIndexed { i, c -> bytes[i] = c.code.coerceIn(0, 126).toByte() }
            try {
                val reader = parser.newReader().process(bytes, bytes.size)
                reader.nextToken
                tokens = JsonParser3(reader).parse()
            } catch (e: IOException) {
                log.w { "Parsing JSON failed: ${e.message}" }
            }
        }

//        tokens.subList(0, 100).forEach {
//            log.i { "tokens: ${it.range} ${it.type}" }
//        }
    }

    override fun doInitialize(text: BigText) {
        log.v {
            try {
                "tokens = ${JsonLexer(text.buildString()).readAllTokens()}"
            } catch (e: ParseException) {
                "ERROR: ${e.message}"
            }
        }

        parse(text.buildString())
    }

    override fun afterTextChange(change: BigTextChangeEvent) {
        parse(change.bigText.buildString())
    }

    override fun onApplyDecorationOnOriginal(text: CharSequence, originalRange: IntRange): CharSequence {
        val startIndex = binarySearchForMinIndexOfValueAtLeast(tokens.indices, originalRange.start) { tokens[it].range.endInclusive }
        val endIndex = minOf(tokens.lastIndex, binarySearchForMaxIndexOfValueAtMost(tokens.indices, originalRange.endInclusive) { tokens[it].range.start })
        val spanStylesSubList = tokens.subList(startIndex, endIndex + 1)
            .map { AnnotatedString.Range(
                start = maxOf(0, it.range.start - originalRange.start),
                end = minOf(originalRange.length, it.range.endInclusive + 1 - originalRange.start),
                item = when (it.type) {
                    JsonNodeType.OBJECT_KEY -> objectKeyStyle
                    JsonNodeType.STRING -> stringLiteralStyle
                    JsonNodeType.NUMBER -> numberLiteralStyle
                    JsonNodeType.TRUE -> booleanTrueLiteralStyle
                    JsonNodeType.FALSE -> booleanFalseLiteralStyle
                    JsonNodeType.NOTHING -> nothingLiteralStyle
                    else -> throw IllegalStateException("Unexpected token type during highlight: ${it.type}")
                }
            ) }
        return AnnotatedString(text.string(), spanStylesSubList)
    }
}



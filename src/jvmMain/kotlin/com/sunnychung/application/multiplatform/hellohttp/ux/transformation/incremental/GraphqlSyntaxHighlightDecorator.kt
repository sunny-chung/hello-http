package com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.sunnychung.application.multiplatform.hellohttp.util.VisitScope
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.visit
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor
import io.github.dralletje.ktreesitter.graphql.TreeSitterGraphql
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node

class GraphqlSyntaxHighlightDecorator(colours: AppColor) : AbstractSyntaxHighlightDecorator(Language(TreeSitterGraphql.language())) {
    val COMMENT_STYLE = SpanStyle(color = colours.syntaxColor.comment, fontStyle = FontStyle.Italic)
    val STRING_LITERAL_STYLE = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val OPERATION_OR_FRAGMENT_KEYWORD_STYLE = SpanStyle(color = colours.syntaxColor.keyword, fontWeight = FontWeight.Bold)
    val OPERATION_OR_FRAGMENT_NAME_STYLE = SpanStyle(color = colours.syntaxColor.otherName)
    val VARIABLE_NAME_STYLE = SpanStyle(color = colours.syntaxColor.variable)
    val VARIABLE_TYPE_STYLE = SpanStyle(color = colours.syntaxColor.type)
    val FRAGMENT_REFERENCE_STYLE = SpanStyle(color = colours.syntaxColor.otherName)
    val VARIABLE_STYLE = SpanStyle(color = colours.syntaxColor.variable)
    val DIRECTIVE_STYLE = SpanStyle(color = colours.syntaxColor.directive, fontStyle = FontStyle.Italic)
    val OBJECT_KEY_STYLE = SpanStyle(color = colours.syntaxColor.objectKey)
    val NUMBER_LITERAL_STYLE = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val BOOLEAN_TRUE_LITERAL_STYLE = SpanStyle(color = colours.syntaxColor.booleanTrueLiteral)
    val BOOLEAN_FALSE_LITERAL_STYLE = SpanStyle(color = colours.syntaxColor.booleanFalseLiteral)
    val NOTHING_LITERAL_STYLE = SpanStyle(color = colours.syntaxColor.nothingLiteral)
    val FIELD_STYLE = SpanStyle(color = colours.syntaxColor.field)

    override fun doInitialize(text: BigText) {
        super.doInitialize(text)
        log.d { "Graphql sexp = ${ast.rootNode.sexp()}" }
    }

    override fun highlight(
        text: CharSequence,
        range: IntRange,
        visitChildrensFunction: VisitScope.(Node) -> Unit
    ): AnnotatedString {
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
                "comment" -> applyStyle(COMMENT_STYLE, it)
                "StringValue" -> applyStyle(STRING_LITERAL_STYLE, it)
                "OperationType" -> applyStyle(OPERATION_OR_FRAGMENT_KEYWORD_STYLE, it)
                "fragment" -> applyStyle(OPERATION_OR_FRAGMENT_KEYWORD_STYLE, it)
                "OperationDefinition" -> {
                    visitChildrens()
                    it.childrensByGrammarType("Name").forEach {
                        applyStyle(OPERATION_OR_FRAGMENT_NAME_STYLE, it)
                    }
                }
                "FragmentName" -> applyStyle(OPERATION_OR_FRAGMENT_NAME_STYLE, it)
                "Variable" -> applyStyle(VARIABLE_NAME_STYLE, it)
                "Type" -> applyStyle(VARIABLE_TYPE_STYLE, it)
//                "FragmentName" -> applyStyle(FRAGMENT_REFERENCE_STYLE, it)
//                "Variable" -> applyStyle(VARIABLE_STYLE, it)
                "Directive" -> applyStyle(DIRECTIVE_STYLE, it)
                "Name" -> applyStyle(OBJECT_KEY_STYLE, it)
                "IntValue", "FloatValue" -> applyStyle(NUMBER_LITERAL_STYLE, it)
                "true" -> applyStyle(BOOLEAN_TRUE_LITERAL_STYLE, it)
                "false" -> applyStyle(BOOLEAN_FALSE_LITERAL_STYLE, it)
                "NullValue" -> applyStyle(NOTHING_LITERAL_STYLE, it)
                "Field" -> {
                    visitChildrens()
                    it.childrensByGrammarType("Name").forEach {
                        applyStyle(FIELD_STYLE, it)
                    }
                }
                else -> visitChildrens()
            }
            log.v { "AST finish visit change ${it.startByte} ..< ${it.endByte}" }
        }

        return AnnotatedString(text.toString(), spans)
    }
}

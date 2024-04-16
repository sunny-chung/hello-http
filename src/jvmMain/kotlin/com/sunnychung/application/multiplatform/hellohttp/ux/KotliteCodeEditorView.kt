package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.rememberLast
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.SyntaxHighlighterTransformation
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

@Composable
fun KotliteCodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    isEnabled: Boolean = true,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    cacheKey: String = "",
    transformations: List<VisualTransformation> = emptyList(),
) {
    val colours = LocalColor.current
    val textColor: Color = if (isEnabled) {
        colours.text
    } else {
        colours.disabled
    }
    val syntaxHighlighter = //rememberLast(cacheKey) { // incremental syntax highlighting is extremely buggy and unusable
        Highlights.Builder()
            .language(SyntaxLanguage.KOTLIN)
            .theme(SyntaxThemes.darcula(darkMode = colours.isDark))
            .code(text)
            .build()
    //}
    syntaxHighlighter.setCode(text)
    CodeEditorView(
        modifier = modifier,
        isReadOnly = isReadOnly,
        text = text,
        onTextChange = onTextChange,
        textColor = textColor,
        transformations = transformations +
            if (isEnabled) {
                listOf(SyntaxHighlighterTransformation(syntaxHighlighter))
            } else {
                emptyList()
            },
    )
}

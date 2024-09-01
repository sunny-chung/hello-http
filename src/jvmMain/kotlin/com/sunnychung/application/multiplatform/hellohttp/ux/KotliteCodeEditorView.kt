package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.KotlinSyntaxHighlightTransformation

@Composable
fun KotliteCodeEditorView(
    modifier: Modifier = Modifier,
    cacheKey: String,
    isReadOnly: Boolean = false,
    isEnabled: Boolean = true,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    transformations: List<VisualTransformation> = emptyList(),
    testTag: String? = null,
) {
    val textColor: Color = if (isEnabled) {
        LocalColor.current.text
    } else {
        LocalColor.current.disabled
    }
    CodeEditorView(
        modifier = modifier,
        cacheKey = cacheKey,
        isReadOnly = isReadOnly,
        text = text,
        onTextChange = onTextChange,
        textColor = textColor,
        transformations = transformations + if (isEnabled) {
            listOf(KotlinSyntaxHighlightTransformation(LocalColor.current))
        } else {
            emptyList()
        },
        testTag = testTag,
    )
}

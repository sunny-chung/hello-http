package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun KotliteCodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    isEnabled: Boolean = true,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    transformations: List<VisualTransformation> = emptyList(),
) {
    val textColor: Color = if (isEnabled) {
        LocalColor.current.text
    } else {
        LocalColor.current.disabled
    }
    CodeEditorView(
        modifier = modifier,
        isReadOnly = isReadOnly,
        text = text,
        onTextChange = onTextChange,
        textColor = textColor,
        transformations = transformations,
    )
}

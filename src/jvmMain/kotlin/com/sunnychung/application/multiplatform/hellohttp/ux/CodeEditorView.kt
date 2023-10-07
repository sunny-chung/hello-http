package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    text: String
    ) {
    AppTextField(
        value = text,
        onValueChange = { /* TODO */ },
        readOnly = isReadOnly,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier
    )
}

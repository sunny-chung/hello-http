package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.jetpack.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.jetpack.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    textColor: Color = LocalColor.current.text,
    isEnableVariables: Boolean = false,
    knownVariables: Set<String> = setOf(),
    ) {
    val colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor
    )

    AppTextField(
        value = text,
        onValueChange = { onTextChange?.invoke(it) },
        visualTransformation = if (isEnableVariables) {
                EnvironmentVariableTransformation(
                    themeColors = LocalColor.current,
                    knownVariables = knownVariables
                )
            } else {
                VisualTransformation.None
            },
        readOnly = isReadOnly,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        colors = colors,
        modifier = modifier
    )
}

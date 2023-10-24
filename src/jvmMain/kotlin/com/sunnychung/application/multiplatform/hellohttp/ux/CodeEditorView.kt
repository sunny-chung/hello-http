package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.MultipleVisualTransformation

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    textColor: Color = LocalColor.current.text,
    transformations: List<VisualTransformation> = emptyList(),
    isEnableVariables: Boolean = false,
    knownVariables: Set<String> = setOf(),
    ) {
    val colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField
    )

    Box(modifier = modifier) {
        val scrollState = rememberScrollState()
//        log.v { "CodeEditorView text=$text" }
        AppTextField(
            value = text,
            onValueChange = { onTextChange?.invoke(it) },
            visualTransformation = (transformations + if (isEnableVariables) {
                listOf(
                    EnvironmentVariableTransformation(
                        themeColors = LocalColor.current,
                        knownVariables = knownVariables
                    )
                )
            } else {
                emptyList()
            }).let {
                if (it.size > 1) {
                    MultipleVisualTransformation(it)
                } else if (it.size == 1) {
                    it.first()
                } else {
                    VisualTransformation.None
                }
            },
            readOnly = isReadOnly,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            colors = colors,
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
        )
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

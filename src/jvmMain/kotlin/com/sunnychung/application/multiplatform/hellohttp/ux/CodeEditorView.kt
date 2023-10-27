package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.FunctionTransformation
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

    val themeColours = LocalColor.current

    var textValue by remember { mutableStateOf(TextFieldValue(text = text)) }
    if (textValue.text != text) {
        textValue = textValue.copy(text = text)
    }

    fun onPressEnterAddIndent() {
        val cursorPos = textValue.selection.min
        assert(textValue.selection.length == 0)

        log.d { "onPressEnterAddIndent" }

        var numNewLines = 0
        var lastLineStart = 0
        for (i in (cursorPos - 1) downTo 0) {
            if (text[i] == '\n') {
                lastLineStart = i + 1
                break
            }
        }
        var spacesMatch = "^(\\s+)".toRegex().matchAt(text.substring(lastLineStart, cursorPos), 0)
        val newSpaces = "\n" + (spacesMatch?.groups?.get(1)?.value ?: "")
        log.d { "onPressEnterAddIndent add ${newSpaces.length} spaces" }
        onTextChange?.invoke(text.insert(cursorPos, newSpaces))
        textValue = textValue.copy(selection = TextRange(cursorPos + newSpaces.length))
    }

    Box(modifier = modifier) {
        val scrollState = rememberScrollState()
//        log.v { "CodeEditorView text=$text" }
        AppTextField(
            value = textValue,
            onValueChange = {
                onTextChange?.invoke(it.text)
                textValue = it
            },
            visualTransformation = (transformations +
                    if (isEnableVariables) {
                        listOf(
                            EnvironmentVariableTransformation(
                                themeColors = themeColours,
                                knownVariables = knownVariables
                            ),
                            FunctionTransformation(themeColours),
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
                .run {
                    if (!isReadOnly) {
                        this.onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown) {
                                when (it.key) {
                                    Key.Enter -> {
                                        onPressEnterAddIndent()
                                        true
                                    }

                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                    } else {
                        this
                    }
                }
        )
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

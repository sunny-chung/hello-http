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
import androidx.compose.ui.input.key.isShiftPressed
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
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextField
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

    // Replace "\r\n" by "\n" because to workaround the issue:
    // https://github.com/JetBrains/compose-multiplatform/issues/3877
    var textValue by remember(text) { mutableStateOf(TextFieldValue(text = text.replace("\r\n", "\n"))) }
    log.d { "CodeEditorView recompose" }

    fun onPressEnterAddIndent() {
        val cursorPos = textValue.selection.min
        assert(textValue.selection.length == 0)

        log.d { "onPressEnterAddIndent" }

        val text = textValue.text
        var lastLineStart = getLineStart(text, cursorPos)
        var spacesMatch = "^(\\s+)".toRegex().matchAt(text.substring(lastLineStart, cursorPos), 0)
        val newSpaces = "\n" + (spacesMatch?.groups?.get(1)?.value ?: "")
        log.d { "onPressEnterAddIndent add ${newSpaces.length} spaces" }
        textValue = textValue.copy(selection = TextRange(cursorPos + newSpaces.length))
        onTextChange?.invoke(text.insert(cursorPos, newSpaces))
    }

    log.v { "cursor at ${textValue.selection}" }
    fun onPressTab(isShiftPressed: Boolean) {
        val selection = textValue.selection
        val text = textValue.text
        if (selection.length == 0) {
            val cursorPos = selection.min
            val newSpaces = " ".repeat(4)
            textValue = textValue.copy(selection = TextRange(cursorPos + newSpaces.length))
            onTextChange?.invoke(text.insert(cursorPos, newSpaces))
        } else if (!isShiftPressed) { // select text and press tab to insert 1-level indent to lines
            val lineStarts = getAllLineStartsInRegion(
                text = text,
                from = selection.min,
                to = selection.max - 1,
            )
            log.v { "lineStarts = $lineStarts" }
            val newSpaces = " ".repeat(4)
            var s = text
            for (i in lineStarts.size - 1 downTo 0) {
                val it = lineStarts[i]
                s = s.insert(it, newSpaces)
            }

            val (minOffset, maxOffset) = Pair(newSpaces.length, newSpaces.length * lineStarts.size)
            log.d { "off = $minOffset, $maxOffset" }
            textValue = textValue.copy(
                text = s,
                selection = TextRange(
                    start = selection.start + if (!selection.reversed) minOffset else maxOffset,
                    end = selection.end + if (!selection.reversed) maxOffset else minOffset,
                )
            )

            onTextChange?.invoke(s)
        } else { // select text and press shift+tab to remove 1-level indent from lines
            val lineStarts = getAllLineStartsInRegion(
                text = text,
                from = selection.min,
                to = selection.max - 1,
            )
            log.v { "lineStarts R = $lineStarts" }
            var s = text
            var firstLineSpaces = 0
            var numSpaces = 0
            for (i in lineStarts.size - 1 downTo 0) {
                val it = lineStarts[i]
                // at most remove 4 spaces
                val spaceRange = "^ {1,4}".toRegex().matchAt(s.substring(it, minOf(it + 4, s.length)), 0)?.range
                if (spaceRange != null) {
                    s = s.removeRange(it + spaceRange.start..it + spaceRange.endInclusive)
                    val spaceLength = spaceRange.endInclusive + 1 - spaceRange.start
                    numSpaces += spaceLength
                    if (i == 0) firstLineSpaces = spaceLength
                }
            }

            val (minOffset, maxOffset) = Pair(- firstLineSpaces, - numSpaces)
            log.d { "off = $minOffset, $maxOffset" }
            textValue = textValue.copy(
                text = s,
                selection = TextRange(
                    start = maxOf(0, selection.start + if (!selection.reversed) minOffset else maxOffset),
                    end = selection.end + if (!selection.reversed) maxOffset else minOffset,
                )
            )

            onTextChange?.invoke(s)
        }
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
                                    Key.Tab -> {
                                        onPressTab(it.isShiftPressed)
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

fun getLineStart(text: String, position: Int): Int {
    for (i in (position - 1) downTo 0) {
        if (text[i] == '\n') {
            return i + 1
        }
    }
    return 0
}

fun getAllLineStartsInRegion(text: String, from: Int, to: Int): List<Int> {
    return listOf(getLineStart(text, from)) +
            "\n".toRegex().findAll(text.substring(from, to + 1), 0)
                .map { from + it.range.endInclusive + 1 }
}

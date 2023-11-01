package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.FunctionTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.MultipleVisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import java.io.File

@Composable
fun KeyValueEditorView(
    modifier: Modifier = Modifier,
    keyValues: List<UserKeyValuePair>,
    keyPlaceholder: String = "Key",
    valuePlaceholder: String = "Value",
    isInheritedView: Boolean,
    disabledIds: Set<String>,
    isSupportFileValue: Boolean = false,
    isSupportVariables: Boolean = false,
    knownVariables: Set<String> = emptySet(),
    onItemChange: (index: Int, item: UserKeyValuePair) -> Unit,
    onItemAddLast: (item: UserKeyValuePair) -> Unit,
    onItemDelete: (index: Int) -> Unit,
    onDisableChange: (Set<String>) -> Unit,
) {
    val colors = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    var fileDialogRequest by remember { mutableStateOf<Int?>(null) }
    val fileDialogState = rememberFileDialogState()

    if (isShowFileDialog) {
        FileDialog(state = fileDialogState) {
            println("File Dialog result = $it")
            if (it != null) {
                val index = fileDialogRequest!!
                onItemChange(index, keyValues[index].copy(value = it.firstOrNull()?.absolutePath ?: ""))
            }
            isShowFileDialog = false
        }
    }

    Column(modifier) {
        if (false && !isInheritedView) { // always disable
            Row(modifier = Modifier.padding(8.dp)) {
                AppTextButton(text = "Switch to Raw Input", onClick = { /* TODO */ })
            }
        }
        val focusManager = LocalFocusManager.current

        fun Modifier.onKeyDownTabMoveFocus(position: FocusPosition): Modifier {
            return this.onPreviewKeyEvent {
                when {
                    it.key == Key.Tab && it.type == KeyEventType.KeyDown -> {
                        if (it.isShiftPressed && position != FocusPosition.Start) {
                            focusManager.moveFocus(FocusDirection.Previous)
                            true
                        } else if (!it.isShiftPressed && position != FocusPosition.End) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else {
                            // consume the event and do nothing, so that "\t" is not inserted
                            true
                        }
                    }

                    else -> false
                }
            }
        }

        LazyColumn {
            items(count = keyValues.size + if (!isInheritedView) 1 else 0) { index ->
                val it = if (index < keyValues.size) keyValues[index] else null
                val isEnabled = it?.let { if (!isInheritedView) it.isEnabled else !disabledIds.contains(it.id) } ?: true
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = keyPlaceholder, color = colors.placeholder) },
                        value = it?.key ?: "",
                        onValueChange = { v ->
                            if (it != null) {
                                onItemChange(index, it.copy(key = v))
                            } else if (v.isNotEmpty()) {
                                onItemAddLast(
                                    UserKeyValuePair(
                                        id = uuidString(),
                                        key = v,
                                        value = "",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        visualTransformation = if (isSupportVariables) {
                            MultipleVisualTransformation(listOf(
                                EnvironmentVariableTransformation(
                                    themeColors = colors,
                                    knownVariables = knownVariables
                                ),
                                FunctionTransformation(themeColors = colors),
                            ))
                        } else {
                            VisualTransformation.None
                        },
                        readOnly = isInheritedView,
                        textColor = if (isEnabled) colors.primary else colors.disabled,
                        hasIndicatorLine = !isInheritedView,
                        modifier = Modifier.weight(0.4f)
                            .onKeyDownTabMoveFocus(if (index == 0) FocusPosition.Start else FocusPosition.Middle),
                    )
                    if (it?.valueType == FieldValueType.String || it == null) {
                        AppTextFieldWithPlaceholder(
                            placeholder = { AppText(text = valuePlaceholder, color = colors.placeholder) },
                            value = it?.value ?: "",
                            onValueChange = { v ->
                                if (it != null) {
                                    onItemChange(index, it.copy(value = v))
                                } else if (v.isNotEmpty()) {
                                    onItemAddLast(
                                        UserKeyValuePair(
                                            id = uuidString(),
                                            key = "",
                                            value = v,
                                            valueType = FieldValueType.String,
                                            isEnabled = true
                                        )
                                    )
                                }
                            },
                            visualTransformation = if (isSupportVariables) {
                                MultipleVisualTransformation(listOf(
                                    EnvironmentVariableTransformation(
                                        themeColors = colors,
                                        knownVariables = knownVariables
                                    ),
                                    FunctionTransformation(themeColors = colors),
                                ))
                            } else {
                                VisualTransformation.None
                            },
                            readOnly = isInheritedView,
                            textColor = if (isEnabled) colors.primary else colors.disabled,
                            hasIndicatorLine = !isInheritedView,
                            modifier = Modifier.weight(0.6f)
                                .onKeyDownTabMoveFocus(if (index == keyValues.size) FocusPosition.End else FocusPosition.Middle),
                        )
                    } else {
                        val file = if (it.value.isNotEmpty()) File(it.value) else null
                        AppTextButton(
                            text = file?.name ?: "Choose a File",
                            color = if (isEnabled) colors.primary else colors.disabled,
                            onClick = if (!isInheritedView) {
                                { log.d {"onClick file"}; fileDialogRequest = index; isShowFileDialog = true }
                            } else null,
                            modifier = Modifier.weight(0.6f).border(width = 1.dp, color = colors.placeholder)
                        )
                    }
                    if (it != null) {
                        if (isSupportFileValue && !isInheritedView) {
                            DropDownView(
                                items = ValueType.values().toList(),
                                isShowLabel = false,
                                onClickItem = { v ->
                                    val valueType = when (v) {
                                        ValueType.Text -> FieldValueType.String
                                        ValueType.File -> FieldValueType.File
                                    }
                                    onItemChange(index, it.copy(valueType = valueType))
                                    true
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        AppCheckbox(
                            checked = isEnabled,
                            onCheckedChange = { v ->
                                if (!isInheritedView) {
                                    onItemChange(index, it.copy(isEnabled = v))
                                } else {
                                    val newSet = if (!v) {
                                        disabledIds + it.id
                                    } else {
                                        disabledIds - it.id
                                    }
                                    onDisableChange(newSet)
                                }
                            },
                            size = 16.dp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                                .focusProperties { canFocus = false }
                        )
                        if (!isInheritedView) {
                            AppDeleteButton(
                                onClickDelete = { onItemDelete(index) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width((4.dp + 16.dp + 4.dp) * (if (isSupportFileValue) 3 else 2)))
                    }
                }
            }
        }
    }
}

private enum class ValueType(override val displayText: String) : DropDownable {
    Text("Text"),
    File("File")
}

private enum class FocusPosition {
    Start, Middle, End
}

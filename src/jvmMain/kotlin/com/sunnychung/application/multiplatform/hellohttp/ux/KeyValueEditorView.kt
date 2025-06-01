package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import com.sunnychung.lib.multiplatform.bigtext.ux.compose.debugConstraints
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberConcurrentLargeAnnotatedBigTextFieldState
import java.io.File

@Composable
fun KeyValueEditorView(
    modifier: Modifier = Modifier,
    key: String,
    keyValues: List<UserKeyValuePair>,
    keyPlaceholder: String = "Key",
    valuePlaceholder: String = "Value",
    isInheritedView: Boolean,
    disabledIds: Set<String>,
    isSupportFileValue: Boolean = false,
    isSupportVariables: Boolean = false,
    isSupportVariablesInValuesOnly: Boolean = false,
    isSupportDisable: Boolean = true,
    knownVariables: Map<String, String> = emptyMap(),
    onItemChange: (index: Int, item: UserKeyValuePair) -> Unit,
    onItemAddLast: (item: UserKeyValuePair) -> Unit,
    onItemDelete: (index: Int) -> Unit,
    onDisableChange: (Set<String>) -> Unit,
    testTagPart1: TestTagPart? = null,
    testTagPart2: TestTagPart? = null,
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

    val hiddenFocusRequester = remember { FocusRequester() }

    Column(modifier) {
        val hiddenTextState by rememberConcurrentLargeAnnotatedBigTextFieldState(cacheKeys = arrayOf(key))
        AppTextField(
            textState = hiddenTextState,
            onValueChange = { _ -> },
            modifier = Modifier.height(0.dp).fillMaxWidth()
                .focusRequester(hiddenFocusRequester)
                .onFocusChanged { log.w { "hid onFocusChanged $it" } }
                .focusProperties { canFocus = false }
        )

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

        Column {
            var retainFocus by remember { mutableStateOf<Pair<Int, String>?>(null) }
            (0 until keyValues.size + if (!isInheritedView) 1 else 0).forEach { index ->
                val it = if (index < keyValues.size) keyValues[index] else null
                val isEnabled = it?.let { if (!isInheritedView) it.isEnabled else it.isEnabled && !disabledIds.contains(it.id) } ?: true
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.debugConstraints("row $index")) {
                    val keyFocusRequester = remember { FocusRequester() }
                    val valueFocusRequester = remember { FocusRequester() }
                    AppTextFieldWithVariables(
                        key = "$key/${it?.id ?: "New"}/Key",
                        placeholder = { AppText(text = keyPlaceholder, color = colors.placeholder) },
                        value = it?.key ?: "",
                        onValueChange = { v ->
                            if (it != null) {
                                onItemChange(index, it.copy(key = v))
                            } else if (v.isNotEmpty()) {
                                retainFocus = index to "Key"
                                onItemAddLast(
                                    UserKeyValuePair(
                                        id = uuidString(),
                                        key = v,
                                        value = "",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                )
                                hiddenFocusRequester.requestFocus()
                            }
                        },
                        isSupportVariables = isSupportVariables && !isSupportVariablesInValuesOnly,
                        variables = knownVariables,
                        readOnly = isInheritedView,
                        textColor = if (isEnabled) colors.primary else colors.disabled,
                        hasIndicatorLine = !isInheritedView,
                        onFinishInit = { textState ->
                            retainFocus?.takeIf {
                                it.first == index && it.second == "Key"
                            }?.run {
                                if (hiddenTextState.text.isNotEmpty) {
                                    textState.text.append(hiddenTextState.text.buildCharSequence())
                                    hiddenTextState.viewState.setCursorIndex(0)
                                    hiddenTextState.text.delete(0, hiddenTextState.text.length)
                                }
                                textState.viewState.setCursorIndex(textState.text.length)
                                keyFocusRequester.requestFocus()
                                retainFocus = null
                            }
                        },
                        modifier = Modifier.weight(0.4f)
                            .focusRequester(keyFocusRequester)
                            .onKeyDownTabMoveFocus(if (index == 0) FocusPosition.Start else FocusPosition.Middle)
                            .run {
                                buildTestTag(testTagPart1, testTagPart2, TestTagPart.Key, index)?.let {
                                    testTag(it)
                                } ?: this
                            },
                    )
                    if (it?.valueType == FieldValueType.String || it == null) {
                        AppTextFieldWithVariables(
                            key = "$key/${it?.id ?: "New"}/Value",
                            placeholder = { AppText(text = valuePlaceholder, color = colors.placeholder) },
                            value = it?.value ?: "",
                            onValueChange = { v ->
                                if (it != null) {
                                    onItemChange(index, it.copy(value = v))
                                } else if (v.isNotEmpty()) {
                                    retainFocus = index to "Value"
                                    onItemAddLast(
                                        UserKeyValuePair(
                                            id = uuidString(),
                                            key = "",
                                            value = v,
                                            valueType = FieldValueType.String,
                                            isEnabled = true
                                        )
                                    )
                                    hiddenFocusRequester.requestFocus()
                                }
                            },
                            isSupportVariables = isSupportVariables,
                            variables = knownVariables,
                            readOnly = isInheritedView,
                            textColor = if (isEnabled) colors.primary else colors.disabled,
                            hasIndicatorLine = !isInheritedView,
                            onFinishInit = { textState ->
                                retainFocus?.takeIf {
                                    it.first == index && it.second == "Value"
                                }?.run {
                                    if (hiddenTextState.text.isNotEmpty) {
                                        textState.text.append(hiddenTextState.text.buildCharSequence())
                                        hiddenTextState.viewState.setCursorIndex(0)
                                        hiddenTextState.text.delete(0, hiddenTextState.text.length)
                                    }
                                    textState.viewState.setCursorIndex(textState.text.length)
                                    valueFocusRequester.requestFocus()
                                    retainFocus = null
                                }
                            },
                            modifier = Modifier.weight(0.6f)
                                .focusRequester(valueFocusRequester)
                                .onKeyDownTabMoveFocus(if (index == keyValues.size) FocusPosition.End else FocusPosition.Middle)
                                .run {
                                    buildTestTag(testTagPart1, testTagPart2, TestTagPart.Value, index)?.let {
                                        testTag(it)
                                    } ?: this
                                },
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
                                .run {
                                    buildTestTag(testTagPart1, testTagPart2, index, TestTagPart.FileButton)?.let {
                                        testTag(it)
                                    } ?: this
                                },
                        )
                    }
                    if (it != null) {
                        if (isSupportFileValue && !isInheritedView) {
                            DropDownView(
                                items = ValueType.values().map { DropDownKeyValue(it, it.displayText) }.toList(),
                                isShowLabel = false,
                                onClickItem = { v ->
                                    val valueType = when (v.key) {
                                        ValueType.Text -> FieldValueType.String
                                        ValueType.File -> FieldValueType.File
                                    }
                                    onItemChange(index, it.copy(valueType = valueType))
                                    true
                                },
                                testTagParts = arrayOf(testTagPart1, testTagPart2, index, TestTagPart.ValueTypeDropdown),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        if (isSupportDisable) {
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
                                isFocusable = false,
                                size = 16.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                                    .focusProperties { canFocus = false }
                            )
                        }
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

private enum class ValueType(val displayText: String) {
    Text(displayText = "Text"),
    File(displayText = "File")
}

private enum class FocusPosition {
    Start, Middle, End
}

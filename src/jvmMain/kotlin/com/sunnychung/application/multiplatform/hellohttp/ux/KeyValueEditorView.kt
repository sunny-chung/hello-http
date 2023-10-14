package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import java.io.File

@Composable
fun KeyValueEditorView(
    modifier: Modifier = Modifier,
    keyValues: List<UserKeyValuePair>,
    isSupportFileValue: Boolean = false,
    onItemChange: (index: Int, item: UserKeyValuePair) -> Unit,
    onItemAddLast: (item: UserKeyValuePair) -> Unit,
    onItemDelete: (index: Int) -> Unit,
) {
    val colors = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    var fileDialogRequest by remember { mutableStateOf<Int?>(null) }

    if (isShowFileDialog) {
        FileDialog {
            println("File Dialog result = $it")
            val index = fileDialogRequest!!
            onItemChange(index, keyValues[index].copy(value = it.firstOrNull()?.absolutePath ?: ""))
            isShowFileDialog = false
        }
    }

    Column(modifier) {
        Row(modifier = Modifier.padding(8.dp)) {
            AppTextButton(text = "Switch to Raw Input", onClick = { /* TODO */ })
        }
        LazyColumn {
            itemsIndexed(items = keyValues) { index, it ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Key", color = colors.placeholder) },
                        value = it.key,
                        onValueChange = { v -> onItemChange(index, it.copy(key = v)) },
                        hasIndicatorLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    if (it.valueType == FieldValueType.String) {
                        AppTextFieldWithPlaceholder(
                            placeholder = { AppText(text = "Value", color = colors.placeholder) },
                            value = it.value,
                            onValueChange = { v -> onItemChange(index, it.copy(value = v)) },
                            hasIndicatorLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    } else {
                        val file = if (it.value.isNotEmpty()) File(it.value) else null
                        AppTextButton(text = file?.name ?: "Choose a File", onClick = { fileDialogRequest = index; isShowFileDialog = true }, modifier = Modifier.weight(0.6f).border(width = 1.dp, color = colors.placeholder))
                    }
                    if (isSupportFileValue) {
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
                    AppCheckbox(checked = it.isEnabled, onCheckedChange = { v -> onItemChange(index, it.copy(isEnabled = v)) }, size = 16.dp, modifier = Modifier.padding(horizontal = 4.dp))
                    AppDeleteButton(onClickDelete = { onItemDelete(index) }, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            item {
                Row {
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Key", color = colors.placeholder) },
                        value = "",
                        onValueChange = { v ->
                            if (v.isNotEmpty()) {
                                onItemAddLast(
                                    UserKeyValuePair(
                                        key = v,
                                        value = "",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        hasIndicatorLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Value", color = colors.placeholder) },
                        value = "",
                        onValueChange = { v ->
                            if (v.isNotEmpty()) {
                                onItemAddLast(
                                    UserKeyValuePair(
                                        key = "",
                                        value = v,
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        hasIndicatorLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                    Spacer(modifier = Modifier.width((4.dp + 16.dp + 4.dp) * (if (isSupportFileValue) 3 else 2)))
                }
            }
        }
    }
}

private enum class ValueType(override val displayText: String) : DropDownable {
    Text("Text"),
    File("File")
}

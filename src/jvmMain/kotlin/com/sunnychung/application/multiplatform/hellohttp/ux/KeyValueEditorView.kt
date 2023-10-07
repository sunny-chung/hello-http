package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun KeyValueEditorView(modifier: Modifier = Modifier, keyValues: List<UserKeyValuePair>, isSupportFileValue: Boolean = false) {
    val colors = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    var fileDialogRequest by remember { mutableStateOf<UserKeyValuePair?>(null) }
    var chosenFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    if (isShowFileDialog) {
        FileDialog {
            println("File Dialog result = $it")
            chosenFiles = it
            isShowFileDialog = false
        }
    }

    Column(modifier) {
        Row(modifier = Modifier.padding(8.dp)) {
//            AppTextButton(text = "Add", onClick = { /* TODO */ })
            AppTextButton(text = "Switch to Raw Input", onClick = { /* TODO */ })
        }
        LazyColumn {
            items(items = keyValues) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Key", color = colors.placeholder) },
                        value = it.key,
                        onValueChange = { /* TODO */ },
                        hasIndicatorLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    if (it.valueType == FieldValueType.String) {
                        AppTextFieldWithPlaceholder(
                            placeholder = { AppText(text = "Value", color = colors.placeholder) },
                            value = it.value,
                            onValueChange = { /* TODO */ },
                            hasIndicatorLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    } else {
                        AppTextButton(text = "Choose a File", onClick = { fileDialogRequest = it; isShowFileDialog = true }, modifier = Modifier.weight(0.6f).border(width = 1.dp, color = colors.placeholder))
                    }
                    if (isSupportFileValue) {
                        DropDownView(items = ValueType.values().toList(), isShowLabel = false, onClickItem = { /* TODO */ true }, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    AppCheckbox(checked = it.isEnabled, onCheckedChange = { /* TODO */ }, size = 16.dp, modifier = Modifier.padding(horizontal = 4.dp))
                    AppDeleteButton(onClickDelete = { /* TODO */ }, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            item {
                Row {
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Key", color = colors.placeholder) },
                        value = "",
                        onValueChange = { /* TODO */ },
                        hasIndicatorLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                    AppTextFieldWithPlaceholder(
                        placeholder = { AppText(text = "Value", color = colors.placeholder) },
                        value = "",
                        onValueChange = { /* TODO */ },
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

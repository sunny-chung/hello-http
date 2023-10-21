package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithIndexedChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemoval
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun SubprojectEnvironmentsEditorView(
    modifier: Modifier = Modifier,
    subproject: Subproject,
    initialEnvironment: Environment?,
    onSubprojectUpdate: (Subproject) -> Unit,
) {
    val colors = LocalColor.current

    var selectedEnvironmentId by remember { mutableStateOf(initialEnvironment?.id) }
    var isFocusOnEnvNameField by remember { mutableStateOf(false) }

    val selectedEnvironment = selectedEnvironmentId?.let { id -> subproject.environments.firstOrNull { it.id == id } }

    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(0.3f).defaultMinSize(minWidth = 160.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                AppText(text = "Environments:", modifier = Modifier.weight(1f))
                AppImageButton(resource = "add.svg", size = 24.dp, onClick = {
                    val newEnvironment = Environment(
                        id = uuidString(),
                        name = "New Environment",
                        variables = listOf()
                    )
                    onSubprojectUpdate(
                        subproject.copy(
                            environments = subproject.environments.toMutableList().apply { this += newEnvironment }
                        )
                    )
                    selectedEnvironmentId = newEnvironment.id
                    isFocusOnEnvNameField = true
                })
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(color = colors.backgroundInputField)) {
                items(items = subproject.environments) {
                    AppText(
                        text = it.name,
                        color = if (selectedEnvironmentId == it.id) colors.highlight else colors.primary,
                        modifier = Modifier.padding(6.dp).defaultMinSize(minHeight = 24.dp).clickable {
                            selectedEnvironmentId = it.id
                            isFocusOnEnvNameField = false
                        }
                    )
                }
            }
        }
        val remainModifier = Modifier.padding(start = 10.dp).weight(0.7f)
        selectedEnvironment?.let { env ->
            EnvironmentEditorView(
                environment = env,
                onEnvironmentUpdate = { newEnv ->
                    val index = subproject.environments.indexOfFirst { it.id == newEnv.id }
                    onSubprojectUpdate(subproject.copy(
                        environments = subproject.environments.copyWithIndexedChange(index, newEnv)
                    ))
                },
                onDeleteEnvironment = { env ->
                    onSubprojectUpdate(subproject.copy(
                        environments = subproject.environments.copyWithRemoval { it.id == env.id }
                    ))
                },
                isFocusOnEnvNameField = isFocusOnEnvNameField,
                modifier = remainModifier,
            )
        } ?: Box(modifier = remainModifier) {

        }
    }
}

@Composable
fun EnvironmentEditorView(
    modifier: Modifier = Modifier,
    environment: Environment,
    onEnvironmentUpdate: (Environment) -> Unit,
    onDeleteEnvironment: (Environment) -> Unit,
    isFocusOnEnvNameField: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    var envName by remember { mutableStateOf(TextFieldValue(text = environment.name)) }
    envName = envName.copy(text = environment.name)

    val updateEnvVariable = { update: (List<UserKeyValuePair>) -> List<UserKeyValuePair> ->
        onEnvironmentUpdate(
            environment.run {
                copy(variables = update(variables))
            }
        )
    }

    Column(modifier = modifier.fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextFieldWithPlaceholder(
                value = envName,
                onValueChange = {
                    onEnvironmentUpdate(environment.copy(name = it.text))
                    envName = it // copy selection range
                },
                placeholder = { AppText(text = "Environment Name", color = LocalColor.current.placeholder) },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                hasIndicatorLine = true,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .background(color = LocalColor.current.backgroundInputField)
                    .focusRequester(focusRequester),
            )
            AppDeleteButton(size = 24.dp) {
                onDeleteEnvironment(environment)
            }
        }

        KeyValueEditorView(
            keyValues = environment.variables,
            isInheritedView = false,
            isSupportFileValue = false,
            disabledIds = emptySet(),
            onItemAddLast = {
                updateEnvVariable { v ->
                    v.toMutableList().apply { add(it) }
                }
            },
            onItemChange = { index, it ->
                updateEnvVariable { v ->
                    v.copyWithIndexedChange(index, it)
                }
            },
            onItemDelete = { index ->
                updateEnvVariable { v ->
                    v.copyWithRemovedIndex(index)
                }
            },
            onDisableChange = {_ ->},
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    if (isFocusOnEnvNameField) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            envName = envName.copy(selection = TextRange(0, envName.text.length))
        }
    }
}

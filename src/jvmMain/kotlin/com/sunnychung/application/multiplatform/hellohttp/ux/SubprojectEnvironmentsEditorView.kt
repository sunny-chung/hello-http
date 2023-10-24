package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
                AppText(text = "Environments", modifier = Modifier.weight(1f))
                AppImageButton(resource = "add.svg", size = 24.dp, onClick = {
                    val newEnvironment = Environment(
                        id = uuidString(),
                        name = "New Environment",
                        variables = mutableListOf()
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
                        modifier = Modifier.padding(6.dp).defaultMinSize(minHeight = 24.dp).fillMaxWidth().clickable {
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
                onUpdateEnvironment = { newEnv ->
                    val index = subproject.environments.indexOfFirst { it.id == newEnv.id }
                    onSubprojectUpdate(subproject.copy(
                        environments = subproject.environments.copyWithIndexedChange(index, newEnv).toMutableList()
                    ))
                },
                onDeleteEnvironment = { env ->
                    onSubprojectUpdate(subproject.copy(
                        environments = subproject.environments.copyWithRemoval { it.id == env.id }.toMutableList()
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
    onUpdateEnvironment: (Environment) -> Unit,
    onDeleteEnvironment: (Environment) -> Unit,
    isFocusOnEnvNameField: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    var envName by remember { mutableStateOf(TextFieldValue(text = environment.name)) }
    envName = envName.copy(text = environment.name)
    var selectedTabIndex by remember { mutableStateOf(0) }

    val updateEnvVariable = { update: (List<UserKeyValuePair>) -> List<UserKeyValuePair> ->
        onUpdateEnvironment(
            environment.run {
                copy(variables = update(variables).toMutableList())
            }
        )
    }

    Column(modifier = modifier.fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextFieldWithPlaceholder(
                value = envName,
                onValueChange = {
                    onUpdateEnvironment(environment.copy(name = it.text))
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

        TabsView(
            selectedIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it },
            contents = EnvironmentEditorTab.values().map { { AppTabLabel(text = it.name) } },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        val modifier = Modifier.fillMaxWidth().weight(1f)
        when (EnvironmentEditorTab.values()[selectedTabIndex]) {
            EnvironmentEditorTab.Variables -> EnvironmentVariableTabContent(
                environment = environment,
                updateEnvVariable = updateEnvVariable,
                modifier = modifier,
            )

            EnvironmentEditorTab.SSL -> EnvironmentSslTabContent(
                environment = environment,
                onUpdateEnvironment = onUpdateEnvironment,
                modifier = modifier,
            )
        }
    }

    if (isFocusOnEnvNameField) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            envName = envName.copy(selection = TextRange(0, envName.text.length))
        }
    }
}

@Composable
fun EnvironmentVariableTabContent(
    modifier: Modifier = Modifier,
    environment: Environment,
    updateEnvVariable: ((List<UserKeyValuePair>) -> List<UserKeyValuePair>) -> Unit,
) {
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
        onDisableChange = { _ -> },
        modifier = modifier,
    )
}

@Composable
fun EnvironmentSslTabContent(
    modifier: Modifier = Modifier,
    environment: Environment,
    onUpdateEnvironment: (Environment) -> Unit,
) {
    fun Boolean?.toConfigText(): BooleanConfigValueText {
        return when (this) {
            true -> BooleanConfigValueText.Yes
            false -> BooleanConfigValueText.No
            null -> BooleanConfigValueText.Default
        }
    }

    val sslConfig = environment.sslConfig
    val headerColumnWidth = 250.dp

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppText(text = "Disable SSL Verification", modifier = Modifier.width(headerColumnWidth).align(Alignment.CenterStart))
            DropDownView(
                selectedItem = DropDownValue(sslConfig.isInsecure.toConfigText().name),
                items = BooleanConfigValueText.values().map { DropDownValue(it.name) },
                onClickItem = {
                    onUpdateEnvironment(
                        environment.copy(sslConfig = sslConfig.copy(
                            isInsecure = BooleanConfigValueText.valueOf(it.displayText).value
                        ))
                    )
                    true
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

private enum class EnvironmentEditorTab {
    Variables, SSL
}

private enum class BooleanConfigValueText(val value: Boolean?) {
    Yes(true), No(false), Default(null)


}

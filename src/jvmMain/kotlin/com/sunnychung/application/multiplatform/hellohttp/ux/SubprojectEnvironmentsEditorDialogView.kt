package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithIndexedChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemoval
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithout
import com.sunnychung.application.multiplatform.hellohttp.util.formatByteSize
import com.sunnychung.application.multiplatform.hellohttp.util.importCaCertificates
import com.sunnychung.application.multiplatform.hellohttp.util.importFrom
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.io.File

@Composable
fun SubprojectEnvironmentsEditorDialogView(
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
        Column(modifier = Modifier.weight(0.25f).defaultMinSize(minWidth = 160.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                AppText(text = "Environments", modifier = Modifier.weight(1f))
                AppImageButton(resource = "add.svg", size = 24.dp, onClick = {
                    log.d { "New Environment button clicked" }
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
                }, modifier = Modifier.testTag(TestTag.EnvironmentDialogCreateButton.name))
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
        val remainModifier = Modifier.padding(start = 10.dp).weight(0.75f)
        selectedEnvironment?.let { env ->
            EnvironmentEditorView(
                environment = env,
                onUpdateEnvironment = { newEnv ->
                    val index = subproject.environments.indexOfFirst { it.id == newEnv.id }
                    onSubprojectUpdate(subproject.copy(
                        environments = subproject.environments.copyWithIndexedChange(index, newEnv).toMutableList()
                    ))
                },
                onDuplicateEnvironment = { env ->
                    val copiedEnv = env.deepCopyWithNewId()
                    onSubprojectUpdate(subproject.copy(
                        environments = (subproject.environments + copiedEnv).toMutableList()
                    ))
                    selectedEnvironmentId = copiedEnv.id
                    isFocusOnEnvNameField = true
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
    onDuplicateEnvironment: (Environment) -> Unit,
    onDeleteEnvironment: (Environment) -> Unit,
    isFocusOnEnvNameField: Boolean,
) {
    val colors = LocalColor.current

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
                    .focusRequester(focusRequester)
                    .testTag(TestTag.EnvironmentDialogEnvNameTextField.name)
            )
            AppTooltipArea(tooltipText = "Duplicate") {
                AppImageButton(resource = "duplicate.svg", size = 24.dp, color = colors.placeholder) {
                    onDuplicateEnvironment(environment)
                }
            }
            AppDeleteButton(size = 24.dp) {
                onDeleteEnvironment(environment)
            }
        }

        TabsView(
            selectedIndex = selectedTabIndex,
            onSelectTab = { selectedTabIndex = it },
            contents = EnvironmentEditorTab.values().map { { AppTabLabel(text = it.name) } },
            testTag = TestTag.EnvironmentEditorTab.name,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        val modifier = Modifier.fillMaxWidth().weight(1f)
        when (EnvironmentEditorTab.values()[selectedTabIndex]) {
            EnvironmentEditorTab.Variables -> EnvironmentVariableTabContent(
                environment = environment,
                updateEnvVariable = updateEnvVariable,
                modifier = modifier.verticalScroll(rememberScrollState()),
            )

            EnvironmentEditorTab.HTTP -> EnvironmentHttpTabContent(
                environment = environment,
                onUpdateEnvironment = onUpdateEnvironment,
                modifier = modifier,
            )

            EnvironmentEditorTab.SSL -> EnvironmentSslTabContent(
                environment = environment,
                onUpdateEnvironment = onUpdateEnvironment,
                modifier = modifier.verticalScroll(rememberScrollState()).testTag(TestTag.EnvironmentEditorSslTabContent.name),
            )

            EnvironmentEditorTab.`User Files` -> EnvironmentUserFilesTabContent(
                environment = environment,
                onUpdateEnvironment = onUpdateEnvironment,
                modifier = modifier.verticalScroll(rememberScrollState()),
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
fun EnvironmentHttpTabContent(
    modifier: Modifier = Modifier,
    environment: Environment,
    onUpdateEnvironment: (Environment) -> Unit,
) {

    val httpConfig = environment.httpConfig
    val headerColumnWidth = 250.dp

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppText(text = "HTTP Protocol Version", modifier = Modifier.width(headerColumnWidth).align(Alignment.CenterStart))
            val options = DropDownMap(listOf(
                DropDownKeyValue(HttpConfig.HttpProtocolVersion.Http1Only, "HTTP/1 only"),
                DropDownKeyValue(HttpConfig.HttpProtocolVersion.Http2Only, "HTTP/2 only"),
                DropDownKeyValue(HttpConfig.HttpProtocolVersion.Negotiate, "Prefer HTTP/2"),
                DropDownKeyValue(null, "Default"),
            ))
            DropDownView(
                selectedItem = options[httpConfig.protocolVersion],
                items = options.dropdownables,
                onClickItem = {
                    onUpdateEnvironment(
                        environment.copy(httpConfig = httpConfig.copy(
                            protocolVersion = it.key
                        ))
                    )
                    true
                },
                testTagParts = arrayOf(TestTagPart.EnvironmentHttpProtocolVersionDropdown),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.padding(horizontal = 8.dp)) {
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

        CertificateEditorView(
            title = "Additional Trusted CA Certificates",
            certificates = sslConfig.trustedCaCertificates,
            isShowAddButton = true,
            onAddCertificates = { new ->
                onUpdateEnvironment(
                    environment.copy(sslConfig = sslConfig.copy(
                        trustedCaCertificates = sslConfig.trustedCaCertificates + new
                    ))
                )
            },
            onUpdateCertificate = { update ->
                onUpdateEnvironment(
                    environment.copy(sslConfig = sslConfig.copy(
                        trustedCaCertificates = sslConfig.trustedCaCertificates.copyWithChange(update)
                    ))
                )
            },
            onDeleteCertificate = { delete ->
                onUpdateEnvironment(
                    environment.copy(
                        sslConfig = sslConfig.copy(
                            trustedCaCertificates = sslConfig.trustedCaCertificates.copyWithRemoval { it.id == delete.id }
                        )
                    )
                )
            },
            testTag = TestTagPart.EnvironmentSslTrustedServerCertificates.name,
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            AppText(text = "Disable System CA Certificates", modifier = Modifier.width(headerColumnWidth).align(Alignment.CenterStart))
            DropDownView(
                selectedItem = DropDownValue(sslConfig.isDisableSystemCaCertificates.toConfigText().name),
                items = BooleanConfigValueText.values().map { DropDownValue(it.name) },
                onClickItem = {
                    onUpdateEnvironment(
                        environment.copy(sslConfig = sslConfig.copy(
                            isDisableSystemCaCertificates = BooleanConfigValueText.valueOf(it.displayText).value
                        ))
                    )
                    true
                },
                testTagParts = arrayOf(TestTagPart.EnvironmentDisableSystemCaCertificates),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Column {
            CertificateEditorView(
                title = "Client Certificate",
                certificates = sslConfig.clientCertificateKeyPairs.map { it.certificate },
                isShowAddButton = false,
                onAddCertificates = { throw NotImplementedError("Unimplemented as intended") },
                onUpdateCertificate = { update ->
                    onUpdateEnvironment(
                        environment.copy(
                            sslConfig = sslConfig.copy(
                                clientCertificateKeyPairs = with(sslConfig.clientCertificateKeyPairs) {
                                    map {
                                        if (it.certificate.id == update.id) {
                                            // TODO this data logic should be in data layer
                                            it.copy(
                                                isEnabled = update.isEnabled,
                                                certificate = update,
                                                privateKey = it.privateKey.copy(isEnabled = update.isEnabled)
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                }
                            )
                        )
                    )
                },
                onDeleteCertificate = { delete ->
                    onUpdateEnvironment(
                        environment.copy(
                            sslConfig = sslConfig.copy(
                                clientCertificateKeyPairs = sslConfig.clientCertificateKeyPairs.copyWithRemoval { it.certificate.id == delete.id }
                            )
                        )
                    )
                },
                testTag = TestTagPart.EnvironmentSslClientCertificates.name,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp)) {
                if (sslConfig.clientCertificateKeyPairs.isEmpty()) {
                    CertificateKeyPairImportForm(onAddItem = { new ->
                        onUpdateEnvironment(
                            environment.copy(
                                sslConfig = sslConfig.copy(
                                    clientCertificateKeyPairs = listOf(new) // always only one
                                )
                            )
                        )
                    })
                } else {
                    AppText(text = "Only 1 certificate can be persisted. To add a new one, delete the current one first.")
                }
            }
        }
    }
}

@Composable
fun CertificateEditorView(
    modifier: Modifier = Modifier,
    testTag: String,
    title: String,
    certificates: List<ImportedFile>,
    isShowAddButton: Boolean,
    onAddCertificates: (List<ImportedFile>) -> Unit,
    onUpdateCertificate: (ImportedFile) -> Unit,
    onDeleteCertificate: (ImportedFile) -> Unit,
) {
    val colours = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    val fileDialogState = rememberFileDialogState()

    fun parseAndAddCertificate(path: String) {
        val file = File(path)
        val imports = try {
            importCaCertificates(file)
        } catch (e: Throwable) {
            AppContext.ErrorMessagePromptViewModel.showErrorMessage("Error while reading the certificate -- ${e.message ?: e::class.simpleName}")
            return
        }

        if (imports.isEmpty()) {
            AppContext.ErrorMessagePromptViewModel.showErrorMessage("No certificate is found")
            return
        }

        onAddCertificates(imports)
    }

    Column(modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppText(text = title, modifier = Modifier.align(Alignment.CenterStart).padding(bottom = 6.dp))
            if (isShowAddButton) {
                AppTooltipArea(
                    tooltipText = "Import a certificate in DER/PEM/P7B/CER/CRT format",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                ) {
                    AppImageButton(
                        resource = "add.svg",
                        size = 24.dp,
                        onClick = { isShowFileDialog = true },
                        modifier = Modifier.testTag(buildTestTag(testTag, TestTagPart.CreateButton)!!)
                    )
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                AppText(
                    text = "Certificate",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.7f)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                AppText(
                    text = "Import Time",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.3f)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                Spacer(modifier = Modifier.width(24.dp + 24.dp))
            }
            certificates.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                    AppText(
                        text = it.name,
                        modifier = Modifier.weight(0.7f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                            .padding(all = 8.dp)
                            .testTag(buildTestTag(testTag, TestTagPart.ListItemLabel)!!.also { println(">>> TTag: $it") })
                    )
                    AppText(
                        text = it.createdWhen.atLocalZoneOffset().format("yyyy-MM-dd HH:mm"),
                        modifier = Modifier.weight(0.3f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                            .padding(all = 8.dp)
                    )
                    AppCheckbox(
                        checked = it.isEnabled,
                        onCheckedChange = { v -> onUpdateCertificate(it.copy(isEnabled = v)) },
                        size = 16.dp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    AppDeleteButton(
                        onClickDelete = { onDeleteCertificate(it) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            if (certificates.isEmpty()) {
                Row {
                    AppText(
                        text = "No entry",
                        modifier = Modifier.weight(1f)
                            .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                            .padding(all = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(24.dp + 24.dp))
                }
            }
        }
    }

    if (isShowFileDialog) {
        FileDialog(state = fileDialogState, title = "Choose a DER/PEM/P7B/CER/CRT file") {
            isShowFileDialog = false
            if (it != null && it.isNotEmpty()) {
                parseAndAddCertificate(it.first().absolutePath)
            }
        }
    }
}

@Composable
fun CertificateKeyPairImportForm(modifier: Modifier = Modifier, onAddItem: (ClientCertificateKeyPair) -> Unit) {
    val headerColumnWidth = 160.dp
    val colours = LocalColor.current

    var certFile by remember { mutableStateOf<File?>(null) }
    var keyFile by remember { mutableStateOf<File?>(null) }
    var bundleFile by remember { mutableStateOf<File?>(null) }
    var bundleFilePassword by remember { mutableStateOf("") }
    var keyFilePassword by remember { mutableStateOf("") }
    var fileChooser by remember { mutableStateOf(CertificateKeyPairFileChooserType.None) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Box(modifier = Modifier.height(IntrinsicSize.Max)) {
            val headerColumnWidth = headerColumnWidth - 25.dp
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {}
            Column {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 25.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(text = "Certificate", modifier = Modifier.width(headerColumnWidth))
                        AppTextButton(
                            text = certFile?.name ?: "Choose a File in DER/PEM/P7B/CER/CRT format",
                            onClick = { fileChooser = CertificateKeyPairFileChooserType.Certificate },
                            modifier = Modifier.testTag(
                                buildTestTag(
                                    TestTagPart.EnvironmentSslClientCertificates,
                                    TestTagPart.ClientCertificate,
                                    TestTagPart.FileButton,
                                )!!
                            )
                        )
                        AppDeleteButton(modifier = Modifier.padding(horizontal = 6.dp)) {
                            certFile = null
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(text = "Private Key", modifier = Modifier.width(headerColumnWidth))
                        AppTextButton(
                            text = keyFile?.name ?: "Choose a File in PKCS#1/PKCS#8 DER/PEM format",
                            onClick = { fileChooser = CertificateKeyPairFileChooserType.PrivateKey },
                            modifier = Modifier.testTag(
                                buildTestTag(
                                    TestTagPart.EnvironmentSslClientCertificates,
                                    TestTagPart.PrivateKey,
                                    TestTagPart.FileButton,
                                )!!
                            )
                        )
                        AppDeleteButton(modifier = Modifier.padding(horizontal = 6.dp)) {
                            keyFile = null
                        }
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(colours.placeholder)
                    ) {}
                    AppText("or", modifier = Modifier.background(colours.background).padding(horizontal = 12.dp, vertical = 4.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 25.dp, end = 25.dp, bottom = 25.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(text = "Bundle", modifier = Modifier.width(headerColumnWidth))
                        AppTextButton(
                            text = bundleFile?.name ?: "Choose a File in PKCS#12/P12/PFX format",
                            onClick = { fileChooser = CertificateKeyPairFileChooserType.Bundle },
                            modifier = Modifier.testTag(
                                buildTestTag(
                                    TestTagPart.EnvironmentSslClientCertificates,
                                    TestTagPart.Bundle,
                                    TestTagPart.FileButton,
                                )!!
                            )
                        )
                        AppDeleteButton(modifier = Modifier.padding(horizontal = 6.dp)) {
                            bundleFile = null
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(text = "Key Store Password", modifier = Modifier.width(headerColumnWidth))
                        AppTextField(
                            value = bundleFilePassword,
                            onValueChange = { bundleFilePassword = it },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.defaultMinSize(minWidth = 200.dp)
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Private Key Password", modifier = Modifier.width(headerColumnWidth))
            AppTextField(
                value = keyFilePassword,
                onValueChange = { keyFilePassword = it },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.defaultMinSize(minWidth = 200.dp)
            )
        }
        Row(modifier = Modifier.align(Alignment.End).padding(top = 4.dp, start = 4.dp, end = 4.dp)) {
            AppTextButton(
                text = "Import this Certificate-Key Pair",
                onClick = {
                    val parsed = try {
                        if (bundleFile == null) {
                            if (certFile == null && keyFile != null) throw IllegalArgumentException("Please select a certificate file.")
                            if (keyFile == null && certFile != null) throw IllegalArgumentException("Please select a private key file.")
                            if (keyFile == null && certFile == null) throw IllegalArgumentException("Please select a bundle file or a certificate and private key file.")
                            ClientCertificateKeyPair.importFrom(
                                certFile = certFile!!,
                                keyFile = keyFile!!,
                                keyPassword = keyFilePassword
                            )
                        } else {
                            ClientCertificateKeyPair.importFrom(
                                bundleFile = bundleFile!!,
                                keyStorePassword = bundleFilePassword,
                                keyPassword = keyFilePassword
                            )
                        }
                    } catch (e: Throwable) {
                        log.w(e) { "Cannot import given certificate-key pair" }
                        AppContext.ErrorMessagePromptViewModel.showErrorMessage(e.message ?: e::class.simpleName!!)
                        return@AppTextButton
                    }
                    onAddItem(parsed)
                },
                modifier = Modifier.testTag(buildTestTag(
                    TestTagPart.EnvironmentSslClientCertificates,
                    TestTagPart.CreateButton,
                )!!)
            )
        }
    }

    if (fileChooser != CertificateKeyPairFileChooserType.None) {
        FileDialog(state = rememberFileDialogState(), title = "Choose a file") {
            val currentFileChooser = fileChooser
            fileChooser = CertificateKeyPairFileChooserType.None
            if (!it.isNullOrEmpty()) {
                when (currentFileChooser) {
                    CertificateKeyPairFileChooserType.None -> {
                        log.w { "currentFileChooser is '$currentFileChooser' for result file ${it.first().absolutePath}" }
                    }
                    CertificateKeyPairFileChooserType.Certificate -> {
                        certFile = it.first()
                        bundleFile = null
                    }
                    CertificateKeyPairFileChooserType.PrivateKey -> {
                        keyFile = it.first()
                        bundleFile = null
                    }
                    CertificateKeyPairFileChooserType.Bundle -> {
                        bundleFile = it.first()
                        certFile = null
                        keyFile = null
                    }
                }
            }
        }
    }
}

@Composable
fun EnvironmentUserFilesTabContent(
    modifier: Modifier = Modifier,
    environment: Environment,
    onUpdateEnvironment: (Environment) -> Unit,
) {
    val colours = LocalColor.current

    fun onUpdateImportedFile(entry: ImportedFile) {
        onUpdateEnvironment(
            environment.copy(
                userFiles = environment.userFiles.copyWithChange(entry)
            )
        )
    }

    fun onDeleteImportedFile(entry: ImportedFile) {
        onUpdateEnvironment(
            environment.copy(
                userFiles = environment.userFiles.copyWithout(entry)
            )
        )
    }

    Column(modifier = modifier
        .fillMaxWidth()
        .padding(all = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            AppText(
                text = "Name",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.4f)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                    .padding(all = 8.dp)
            )
            AppText(
                text = "Original Filename",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.4f)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                    .padding(all = 8.dp)
            )
            AppText(
                text = "Import Time",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                    .padding(all = 8.dp)
            )
            AppText(
                text = "Size",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                    .padding(all = 8.dp)
            )
            Spacer(modifier = Modifier.width(24.dp + 24.dp))
        }
        environment.userFiles.forEach { entry ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                AppTextField(
                    value = entry.name,
                    onValueChange = { onUpdateImportedFile(entry.copy(name = it)) },
                    modifier = Modifier.weight(0.4f)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                AppText(
                    text = entry.originalFilename,
                    modifier = Modifier.weight(0.4f)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                AppText(
                    text = entry.createdWhen.atLocalZoneOffset().format("yyyy-MM-dd HH:mm"),
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                AppText(
                    text = formatByteSize(entry.content.size.toLong()),
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                AppCheckbox(
                    checked = entry.isEnabled,
                    onCheckedChange = { v -> onUpdateImportedFile(entry.copy(isEnabled = v)) },
                    size = 16.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                AppDeleteButton(
                    onClickDelete = { onDeleteImportedFile(entry) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        if (environment.userFiles.isEmpty()) {
            Row {
                AppText(
                    text = "No entry",
                    modifier = Modifier.weight(1f)
                        .border(width = 1.dp, color = colours.placeholder, RectangleShape)
                        .padding(all = 8.dp)
                )
                Spacer(modifier = Modifier.width(24.dp + 24.dp))
            }
        }

        ImportUserFileForm(
            onImportFile = {
                onUpdateEnvironment(
                    environment.copy(
                        userFiles = environment.userFiles + it
                    )
                )
            },
            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )
    }
}

@Composable
fun ImportUserFileForm(modifier: Modifier = Modifier, onImportFile: (ImportedFile) -> Unit) {
    val headerColumnWidth = 200.dp

    var name by remember { mutableStateOf("") }
    var isFileDialogVisible by remember { mutableStateOf(false) }
    var chosenFile by remember { mutableStateOf<File?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Name", modifier = Modifier.width(headerColumnWidth))
            AppTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.defaultMinSize(minWidth = 200.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "File", modifier = Modifier.width(headerColumnWidth))
            AppTextButton(
                text = chosenFile?.name ?: "Choose a File to Import",
                onClick = { isFileDialogVisible = true },
            )
        }
        Row(modifier = Modifier.align(Alignment.End)) {
            AppTextButton(
                text = "Import",
                isEnabled = name.isNotBlank() && chosenFile != null,
                onClick = {
                    try {
                        val file = chosenFile ?: return@AppTextButton
                        if (file.length() > 12 * 1024L * 1024L) {
                            throw IllegalArgumentException("The file is too large. Maximum supported file size is around 10 MB.")
                        }
                        val fileBytes = file.readBytes()
                        onImportFile(
                            ImportedFile(
                                id = uuidString(),
                                name = name,
                                originalFilename = file.name,
                                createdWhen = KInstant.now(),
                                isEnabled = true,
                                content = fileBytes
                            )
                        )

                        // reset after successful import
                        name = ""
                        chosenFile = null
                    } catch (e: Throwable) {
                        AppContext.ErrorMessagePromptViewModel.showErrorMessage(e.message ?: e::class.simpleName!!)
                        return@AppTextButton
                    }
                },
            )
        }
    }

    if (isFileDialogVisible) {
        FileDialog(state = rememberFileDialogState(), title = "Choose a file") {
            isFileDialogVisible = false
            if (!it.isNullOrEmpty()) {
                chosenFile = it.first()
            }
        }
    }
}

private enum class CertificateKeyPairFileChooserType {
    None, Certificate, PrivateKey, Bundle
}

private enum class EnvironmentEditorTab {
    Variables, HTTP, SSL, `User Files`
}

private enum class BooleanConfigValueText(val value: Boolean?) {
    Yes(true), No(false), Default(null)


}

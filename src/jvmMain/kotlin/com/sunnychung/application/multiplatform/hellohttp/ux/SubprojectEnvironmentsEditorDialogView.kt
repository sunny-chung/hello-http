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
import com.sunnychung.application.multiplatform.hellohttp.model.importFrom
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithIndexedChange
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemoval
import com.sunnychung.application.multiplatform.hellohttp.util.copyWithRemovedIndex
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.rememberFileDialogState
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

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

            EnvironmentEditorTab.HTTP -> EnvironmentHttpTabContent(
                environment = environment,
                onUpdateEnvironment = onUpdateEnvironment,
                modifier = modifier,
            )

            EnvironmentEditorTab.SSL -> EnvironmentSslTabContent(
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
            onAddCertificate = { new ->
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
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Column {
            CertificateEditorView(
                title = "Client Certificate",
                certificates = sslConfig.clientCertificateKeyPairs.map { it.certificate },
                isShowAddButton = false,
                onAddCertificate = { throw NotImplementedError("Unimplemented as intended") },
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
    title: String,
    certificates: List<ImportedFile>,
    isShowAddButton: Boolean,
    onAddCertificate: (ImportedFile) -> Unit,
    onUpdateCertificate: (ImportedFile) -> Unit,
    onDeleteCertificate: (ImportedFile) -> Unit,
) {
    val colours = LocalColor.current

    var isShowFileDialog by remember { mutableStateOf(false) }
    val fileDialogState = rememberFileDialogState()

    /**
     * TODO: This logic should be in data layer
     */
    fun parseAndAddCertificate(path: String) {
        val file = File(path)
        val content = file.readBytes()
        val cert: X509Certificate = CertificateFactory.getInstance("X.509").generateCertificate(content.inputStream()) as X509Certificate
        log.d { "Loaded cert ${cert}" }

        onAddCertificate(
            ImportedFile(
                id = uuidString(),
                name = cert.subjectX500Principal.getName(X500Principal.RFC1779) +
                    "\nExpiry: ${KZonedInstant(cert.notAfter.time, KZoneOffset.local()).format(KDateTimeFormat.ISO8601_DATETIME.pattern)}" +
                    if (cert.keyUsage?.get(5) != true) "\n⚠️ Not a CA certificate!" else ""
                ,
                originalFilename = file.name,
                createdWhen = KInstant.now(),
                isEnabled = true,
                content = content,
            )
        )
    }

    Column(modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppText(text = title, modifier = Modifier.align(Alignment.CenterStart).padding(bottom = 6.dp))
            if (isShowAddButton) {
                AppTooltipArea(
                    tooltipText = "Import a certificate in DER format",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                ) {
                    AppImageButton(
                        resource = "add.svg",
                        size = 24.dp,
                        onClick = { isShowFileDialog = true },
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
                    )
                    AppText(
                        text = it.createdWhen.atZoneOffset(KZoneOffset.local()).format("yyyy-MM-dd HH:mm"),
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
        FileDialog(state = fileDialogState, title = "Choose a DER file") {
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

    var certFile by remember { mutableStateOf<File?>(null) }
    var keyFile by remember { mutableStateOf<File?>(null) }
    var keyFilePassword by remember { mutableStateOf("") }
    var fileChooser by remember { mutableStateOf(CertificateKeyPairFileChooserType.None) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Certificate", modifier = Modifier.width(headerColumnWidth))
            AppTextButton(
                text = certFile?.name ?: "Choose a File in DER format",
                onClick = { fileChooser = CertificateKeyPairFileChooserType.Certificate },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(text = "Private Key", modifier = Modifier.width(headerColumnWidth))
            AppTextButton(
                text = keyFile?.name ?: "Choose a File in PKCS #8 DER format",
                onClick = { fileChooser = CertificateKeyPairFileChooserType.PrivateKey },
            )
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
        Row {
            Spacer(modifier = Modifier.width(4.dp))
            AppTextButton(
                text = "Import this Certificate-Key Pair",
                onClick = {
                    val parsed = try {
                        if (certFile == null) throw IllegalArgumentException("Please select a certificate file.")
                        if (keyFile == null) throw IllegalArgumentException("Please select a private key file.")
                        ClientCertificateKeyPair.importFrom(
                            certFile = certFile!!,
                            keyFile = keyFile!!,
                            keyPassword = keyFilePassword
                        )
                    } catch (e: Throwable) {
                        AppContext.ErrorMessagePromptViewModel.showErrorMessage(e.message ?: e::class.simpleName!!)
                        return@AppTextButton
                    }
                    onAddItem(parsed)
                },
            )
        }
    }

    if (fileChooser != CertificateKeyPairFileChooserType.None) {
        FileDialog(state = rememberFileDialogState(), title = "Choose a DER file") {
            val currentFileChooser = fileChooser
            fileChooser = CertificateKeyPairFileChooserType.None
            if (!it.isNullOrEmpty()) {
                when (currentFileChooser) {
                    CertificateKeyPairFileChooserType.None -> throw IllegalStateException()
                    CertificateKeyPairFileChooserType.Certificate -> certFile = it.first()
                    CertificateKeyPairFileChooserType.PrivateKey -> keyFile = it.first()
                }
            }
        }
    }
}

private enum class CertificateKeyPairFileChooserType {
    None, Certificate, PrivateKey
}

private enum class EnvironmentEditorTab {
    Variables, HTTP, SSL
}

private enum class BooleanConfigValueText(val value: Boolean?) {
    Yes(true), No(false), Default(null)


}

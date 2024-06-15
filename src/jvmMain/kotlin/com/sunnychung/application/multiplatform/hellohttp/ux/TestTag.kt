package com.sunnychung.application.multiplatform.hellohttp.ux

enum class TestTag {
    ContainerView,
    DialogContainerView,
    DialogCloseButton,
    ProjectNameAndSubprojectNameDialogTextField,
    ProjectNameAndSubprojectNameDialogDoneButton,
    FirstTimeCreateProjectButton,
    FirstTimeCreateSubprojectButton,
    EditEnvironmentsButton,
    EnvironmentDialogCreateButton,
    EnvironmentDialogEnvNameTextField,
    EnvironmentEditorTab,
    EnvironmentEditorSslTabContent,
    CreateRequestOrFolderButton,
    RequestUrlTextField,
    RequestFetchApiSpecButton,
    RequestCancelFetchApiSpecButton,
    RequestParameterTypeTab,
    RequestStringBodyTextField,
    RequestGraphqlDocumentTextField,
    RequestGraphqlVariablesTextField,
    RequestFireOrDisconnectButton,
    RequestSendPayloadButton,
    RequestCompleteStreamButton,
    RequestAddPayloadExampleButton,
    RequestPayloadTextField,
    ResponseStatus,
    ResponseDuration,
    ResponseBody,
    ResponseBodyEmpty,
    ResponseStreamLog,
    ResponseStreamLogItemTime,
}

enum class TestTagPart {
    RequestMethodDropdown,
    RequestHeader,
    RequestQueryParameter,
    RequestBodyTypeDropdown,
    RequestBodyFormUrlEncodedForm,
    RequestBodyMultipartForm,
    RequestBodyFileForm,
    RequestGraphqlOperationName,
    RequestApiSpecDropdown,
    RequestGrpcServiceDropdown,
    RequestGrpcMethodDropdown,
    EnvironmentDropdown,
    EnvironmentSslTrustedServerCertificates,
    EnvironmentSslClientCertificates,
    EnvironmentDisableSystemCaCertificates,
    ClientCertificate,
    PrivateKey,
    CreateButton,
    ListItemLabel,
    Inherited,
    Current,
    Key,
    Value,
    FileButton,
    ValueTypeDropdown,
    DropdownButton,
    DropdownItem,
    DropdownLabel,
}

fun buildTestTag(vararg parts: Any?): String? {
    if (parts.any { it == null }) {
        return null
    }
    return parts.joinToString("/")
}

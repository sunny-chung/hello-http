package com.sunnychung.application.multiplatform.hellohttp.ux

enum class TestTag {
    ContainerView,
    DialogContainerView,
    ProjectNameAndSubprojectNameDialogTextField,
    ProjectNameAndSubprojectNameDialogDoneButton,
    FirstTimeCreateProjectButton,
    FirstTimeCreateSubprojectButton,
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
    ResponseBody,
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
    Inherited,
    Current,
    Key,
    Value,
    FileButton,
    ValueTypeDropdown,
    DropdownButton,
    DropdownItem,
}

fun buildTestTag(vararg parts: Any?): String? {
    if (parts.any { it == null }) {
        return null
    }
    return parts.joinToString("/")
}

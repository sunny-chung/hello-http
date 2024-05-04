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
    RequestParameterTypeTab,
    RequestStringBodyTextField,
    RequestGraphqlDocumentTextField,
    RequestGraphqlVariablesTextField,
    RequestFireOrDisconnectButton,
    ResponseStatus,
    ResponseBody,
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

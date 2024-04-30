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

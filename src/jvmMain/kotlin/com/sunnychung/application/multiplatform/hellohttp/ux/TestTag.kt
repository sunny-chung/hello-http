package com.sunnychung.application.multiplatform.hellohttp.ux

enum class TestTag {
    ContainerView,
    DialogContainerView,
    ProjectNameAndSubprojectNameDialogTextField,
    ProjectNameAndSubprojectNameDialogDoneButton,
    FirstTimeCreateProjectButton,
    FirstTimeCreateSubprojectButton,
    CreateRequestOrFolderButton,
    RequestMethodDropdownButton,
    RequestUrlTextField,
    RequestParameterTypeTab,
    RequestFireOrDisconnectButton,
    ResponseStatus,
    ResponseBody,
}

enum class TestTagPart {
    RequestHeader,
    RequestQueryParameter,
    Inherited,
    Current,
    Key,
    Value,
    FileButton,
    ValueTypeDropdown,
}

fun buildTestTag(vararg parts: Any?): String? {
    if (parts.any { it == null }) {
        return null
    }
    return parts.joinToString("/")
}

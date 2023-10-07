package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable

data class Subproject(
    val id: String,
    val name: String,
    val requests: List<UserRequest>,
) : DropDownable {
    override val displayText: String
        get() = name
}

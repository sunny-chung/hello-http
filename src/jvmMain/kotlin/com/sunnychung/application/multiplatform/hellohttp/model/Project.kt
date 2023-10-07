package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable

data class Project(
    val id: String,
    val name: String,
    val subprojects: List<Subproject>
) : DropDownable {
    override val displayText: String
        get() = name
}

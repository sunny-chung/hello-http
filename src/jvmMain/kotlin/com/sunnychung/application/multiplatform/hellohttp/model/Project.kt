package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class Project(
    val id: String,
    val name: String,
    val subprojects: MutableList<Subproject>
) : DropDownable {

    override val key: String
        get() = id

    override val displayText: String
        get() = name
}

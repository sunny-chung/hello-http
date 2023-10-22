package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class Environment(
    val id: String,
    val name: String,
    val variables: MutableList<UserKeyValuePair>,
) : DropDownable {
    override val displayText: String
        get() = name
}

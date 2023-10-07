package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable

data class Environment(
    val name: String
) : DropDownable {
    override val displayText: String
        get() = name
}

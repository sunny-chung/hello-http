package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class UserPreference(
    var colourTheme: ColourTheme
)

enum class ColourTheme {
    Dark, Light
}

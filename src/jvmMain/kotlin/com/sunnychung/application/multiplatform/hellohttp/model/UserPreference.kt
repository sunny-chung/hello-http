package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import kotlinx.serialization.Serializable

val DEFAULT_BACKUP_RETENTION_DAYS = 15

@Persisted
@Serializable
data class UserPreference(
    var colourTheme: ColourTheme,
    var backupRetentionDays: Int? = null,
)

enum class ColourTheme {
    Dark, Light
}

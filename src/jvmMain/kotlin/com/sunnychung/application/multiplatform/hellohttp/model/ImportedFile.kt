package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.serialization.Serializable

@Persisted
@Serializable
data class ImportedFile(
    override val id: String,
    val name: String,
    val originalFilename: String,
    val createdWhen: KInstant,
    val isEnabled: Boolean,
    val content: ByteArray,
) : Identifiable {
    override fun toString(): String {
        return "ImportedFile(id='$id', name='$name', originalFilename='$originalFilename', createdWhen=$createdWhen, isEnabled=$isEnabled, content={size=${content.size}})"
    }
}

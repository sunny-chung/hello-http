package com.sunnychung.application.multiplatform.hellohttp.repository

import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDocument
import kotlinx.serialization.serializer

class UserPreferenceRepository : BaseCollectionRepository<UserPreferenceDocument, UserPreferenceDI>(serializer()) {
    override fun relativeFilePath(id: UserPreferenceDI): String = "preference.db"
}

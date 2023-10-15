package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.document.Identifiable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TreeObject : Identifiable

@Persisted
@Serializable
@SerialName("Folder")
data class TreeFolder(override val id: String, val name: String, val childs: MutableList<TreeObject>) : TreeObject

@Persisted
@Serializable
@SerialName("Request")
data class TreeRequest(override val id: String) : TreeObject

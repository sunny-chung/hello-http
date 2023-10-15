package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.annotation.Persisted
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.DropDownable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Persisted
@Serializable
data class Subproject(
    val id: String,
    val name: String,
    val treeObjects: MutableList<TreeObject>,
    @Transient var uiVersion: String = "", // for UI use only, to make Subproject.equals() returns false
) : DropDownable {
    override val displayText: String
        get() = name

    fun renewUICache() {
        uiVersion = uuidString()
    }

    fun removeTreeObjectIf(condition: (TreeObject) -> Boolean) {
        fun transverse(childs: MutableList<TreeObject>) {
            childs.removeIf(condition)
            childs.forEach {
                if (it is TreeFolder) {
                    transverse(it.childs)
                }
            }
        }

        transverse(treeObjects)
    }

    fun deepCopy(): Subproject {
        fun transverse(childs: MutableList<TreeObject>): MutableList<TreeObject> {
            return childs.map {
                when(it) {
                    is TreeFolder -> it.copy(childs = transverse(it.childs))
                    is TreeRequest -> it.copy()
                }
            }.toMutableList()
        }
        return copy(treeObjects = transverse(treeObjects)).apply { renewUICache() }
    }
}

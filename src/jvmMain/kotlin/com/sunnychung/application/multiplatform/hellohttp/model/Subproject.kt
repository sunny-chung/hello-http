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

    /**
     * Move {item} into {destination}. The only restriction is cannot move into itself.
     */
    fun move(itemId: String, destination: TreeFolder?): Boolean {
        val (parent, item) = findParentAndItem(itemId)
        if (item == destination) return false
        if (destination != null) findParentAndItem(destination.id) // assert destination is within this subproject
        if (parent is TreeFolder) {
            assert(parent.childs.removeIf { it.id == item.id })
        } else {
            assert(treeObjects.removeIf { it.id == item.id })
        }
        if (destination != null) {
            destination.childs += item
        } else {
            treeObjects += item
        }
        return true
    }

    /**
     * Find item that id = itemId and its parent.
     *
     * @return pair of parent and item
     */
    fun findParentAndItem(itemId: String): Pair<TreeObject?, TreeObject> {
        fun transverse(current: TreeObject, childs: MutableList<TreeObject>): Pair<TreeObject, TreeObject>? {
            childs.forEach {
                if (it.id == itemId) {
                    return Pair(current, it)
                }
                if (it is TreeFolder) {
                    val r = transverse(it, it.childs)
                    if (r != null) {
                        return r
                    }
                }
            }
            return null
        }
        val root = TreeFolder(id = "root", "", mutableListOf())
        val (parent, item) = transverse(root, treeObjects) ?: throw NoSuchElementException()
        return when (parent) {
            root -> Pair(null, item)
            else -> Pair(parent, item)
        }
    }
}

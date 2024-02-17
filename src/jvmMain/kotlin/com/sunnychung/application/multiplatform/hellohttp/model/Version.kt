package com.sunnychung.application.multiplatform.hellohttp.model

class Version(val versionName: String) : Comparable<Version> {
    private val components = versionName.split("[\\.-]+".toRegex())
    override operator fun compareTo(other: Version): Int {
        (0 .. maxOf(components.lastIndex, other.components.lastIndex)).forEach { i ->
            val compareResult = this.numericComponent(i).compareTo(other.numericComponent(i))
            if (compareResult != 0) {
                return compareResult
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Version) return false
        return compareTo(other) == 0
    }

    fun numericComponent(index: Int): Int {
        return if (index > components.lastIndex) {
            0
        } else {
            components[index].toIntOrNull() ?: -1
        }
    }

    override fun toString(): String  = versionName
}

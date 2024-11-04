package com.sunnychung.application.multiplatform.hellohttp.util

class MutableObjectRef<T>(var value: T) {
    override fun equals(other: Any?): Boolean {
        if (other !is MutableObjectRef<*>) return false
        return value === other.value
    }

//    override fun hashCode(): Int {
//        return ref.hashCode()
//    }
}

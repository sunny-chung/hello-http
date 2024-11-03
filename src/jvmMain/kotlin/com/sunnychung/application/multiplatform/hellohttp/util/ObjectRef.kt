package com.sunnychung.application.multiplatform.hellohttp.util

class ObjectRef<T>(val ref: T) {
    override fun equals(other: Any?): Boolean {
        if (other !is ObjectRef<*>) return false
        return ref === other.ref
    }
}

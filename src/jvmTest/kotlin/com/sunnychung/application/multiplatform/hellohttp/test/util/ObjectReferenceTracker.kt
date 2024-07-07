package com.sunnychung.application.multiplatform.hellohttp.test.util

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class ObjectReferenceTracker<T : Any>(val subject: T) {
    private val subjectObjects = mutableListOf<Any>() // set is not used, because two different objects can be "equal"

    init {
        trackAllObjects(subject, subjectObjects)
        println("${subjectObjects.size} object references found")
    }

    fun trackAllObjects(parent: Any, tracker: MutableList<Any>) {
        if (tracker.any { it === parent }) return // prevent infinite loop due to cyclic dependencies
        parent::class.memberProperties.forEach {
            val clazz = it.javaField?.type
//            println(">> ${it.name} -- ${clazz?.simpleName} - ${clazz?.isPrimitive}, ${clazz?.isEnum}, ${clazz == String::class.java}")
            if (clazz?.isPrimitive == false && clazz?.isEnum == false && clazz != String::class.java) {
                val value = (it as KProperty1<Any, *>).get(parent)
                if (value != null) {
                    tracker += value
                    trackAllObjects(value, tracker)
                }
                if (value is Iterable<*>) {
                    value.forEach { it?.let { trackAllObjects(it, tracker) } }
                } else if (value is Array<*>) {
                    value.forEach { it?.let { trackAllObjects(it, tracker) } }
                }
            }
        }
    }

    fun assertNoObjectReferenceIsCopied(other: T, lazyErrorMessage: (Any) -> String) {
        val newObjectRefs = mutableListOf<Any>()
        trackAllObjects(other, newObjectRefs)
        newObjectRefs.forEach { newRef ->
            assert(subjectObjects.none { it === newRef }) { lazyErrorMessage(newRef) }
        }
    }
}

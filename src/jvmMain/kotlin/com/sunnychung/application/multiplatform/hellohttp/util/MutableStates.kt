package com.sunnychung.application.multiplatform.hellohttp.util

fun <T> MutableList<T>.replaceIf(replacement: T, condition: (T) -> Boolean) {
    synchronized(this) {
        val index = indexOfFirst(condition)
        if (index >= 0) {
            set(index, replacement)
        } else {
            throw IllegalStateException("Cannot replace state in iterables")
        }
    }
}

/**
 * This method is NOT thread-safe.
 */
fun <T> MutableList<T>.upsert(entity: T, condition: (T) -> Boolean, update: (T, T) -> T): T {
    synchronized(this) {
        val index = indexOfFirst(condition)
        if (index >= 0) {
            val update = update(get(index), entity)
            set(index, update)
            return update
        } else {
            add(entity)
            return entity
        }
    }
}

/**
 * This method is NOT thread-safe.
 */
fun <K, V> MutableMap<K, V>.getAndDoSomethingOrPut(key: K, doIfExists: (V) -> Unit, putIfNotExists: () -> V): V {
    return this[key]?.also(doIfExists)
        ?: run {
            val newValue = putIfNotExists()
            this[key] = newValue
            newValue
        }
}

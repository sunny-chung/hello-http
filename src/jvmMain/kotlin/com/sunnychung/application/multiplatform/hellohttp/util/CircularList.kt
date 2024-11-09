package com.sunnychung.application.multiplatform.hellohttp.util

/**
 * Not thread-safe.
 */
class CircularList<T>(val capacity: Int) {

    internal var head = -1
    internal var tail = -1

    val size: Int
        get() {
            if (head < 0 || tail < 0) return 0
            return ((head + capacity - tail + 1) % capacity).let {
                if (it == 0) capacity else it
            }
        }

    val isEmpty: Boolean
        get() = size <= 0

    val isNotEmpty: Boolean
        get() = size > 0

    private var store = ArrayList<T?>()

    fun push(item: T) {
        val oldSize = size
        head = (head + 1) % capacity
        if (store.size <= head) {
            store += item
        } else {
            if (oldSize >= capacity) {
                tail = (head + 1) % capacity
            }
            store[head] = item
        }
        if (tail < 0) {
            tail = head
        }
    }

    fun removeHead(): T? {
        if (size == 0) {
            return null
        }
        val item = store[head]
        store[head] = null
        if (head == tail) {
            head = -1
            tail = -1
        } else {
            head = (head - 1 + capacity) % capacity
        }
        return item
    }

    fun removeTail(): T? {
        if (size == 0) {
            return null
        }
        val item = store[tail]
        store[tail] = null
        if (head == tail) {
            head = -1
            tail = -1
        } else {
            tail = (tail + 1) % capacity
        }
        return item
    }

    fun clear() {
        head = -1
        tail = -1
        store = ArrayList()
    }
}

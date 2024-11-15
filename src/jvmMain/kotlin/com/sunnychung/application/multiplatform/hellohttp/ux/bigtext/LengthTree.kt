package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

open class LengthTree<V>(computations: RedBlackTreeComputations<V>) : RedBlackTree2<@UnsafeVariance V>(computations)
        where V : LengthNodeValue, V : Comparable<@UnsafeVariance V>, V : DebuggableNode<in @UnsafeVariance V> {

    fun findNodeByCharIndex(index: Int, isIncludeMarkerNodes: Boolean = true, isExact: Boolean = false): RedBlackTree<V>.Node? {
        var find = index
        var lastMatch: RedBlackTree<V>.Node? = null
        return findNode {
            when (find) {
                in Int.MIN_VALUE until it.value.leftStringLength -> -1
                it.value.leftStringLength, in it.value.leftStringLength until it.value.leftStringLength + it.value.bufferLength -> {
                    lastMatch = it
                    if (!isExact && isIncludeMarkerNodes && find == it.value.leftStringLength && it.left.isNotNil()) {
                        -1
                    } else if (!isExact && !isIncludeMarkerNodes && find == it.value.leftStringLength + it.value.bufferLength && it.right.isNotNil()) {
                        1
                    } else {
                        0
                    }
                }
                in it.value.leftStringLength + it.value.bufferLength until Int.MAX_VALUE -> (
                    if (it.right.isNotNil()) {
                        1
                    } else {
                        0
                    }
                )
                else -> throw IllegalStateException("what is find? $find")
            }.also { compareResult ->
                val isTurnRight = compareResult > 0
                if (isTurnRight) {
                    find -= it.value.leftStringLength + it.value.bufferLength
                }
            }
        }?.takeIf {
            val nodePosStart = findPositionStart(it)
            nodePosStart <= index && (
                index < nodePosStart + it.value.bufferLength
                    || it.value.bufferLength == 0
                    || (index == getRoot().length() && it === rightmost(getRoot()))
            )
        }
            ?: lastMatch
    }

    fun findNodeByRenderCharIndex(index: Int): RedBlackTree<V>.Node? {
        var find = index
        return findNode {
            when (find) {
                in Int.MIN_VALUE until it.value.leftRenderLength -> -1
                in it.value.leftRenderLength until it.value.leftRenderLength + it.value.currentRenderLength -> 0
                in it.value.leftRenderLength + it.value.currentRenderLength until Int.MAX_VALUE -> (
                    if (it.right.isNotNil()) {
                        1
                    } else {
                        0
                    }
                ).also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftRenderLength + it.value.currentRenderLength
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }
    }

    fun findPositionStart(node: RedBlackTree<V>.Node): Int {
        var start = node.value.leftStringLength
        var node = node
        while (node.parent.isNotNil()) {
            if (node === node.parent.right) {
                start += node.parent.value.leftStringLength + node.parent.value.bufferLength
            }
            node = node.parent
        }
        return start
    }
}

fun <V> RedBlackTree<V>.Node.length(): Int where V : LengthNodeValue, V : Comparable<V> =
    (getValue()?.leftStringLength ?: 0) +
            (getValue()?.bufferLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.length() ?: 0)

fun <V> RedBlackTree<V>.Node.renderLength(): Int where V : LengthNodeValue, V : Comparable<V> =
    (getValue()?.leftRenderLength ?: 0) +
            (getValue()?.currentRenderLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.renderLength() ?: 0)

fun <V> RedBlackTree<V>.Node.overallLength(): Int where V : LengthNodeValue, V : Comparable<V> =
    (getValue()?.leftOverallLength ?: 0) +
            (getValue()?.currentOverallLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.overallLength() ?: 0)

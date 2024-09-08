package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

open class LengthTree<out V>(computations: RedBlackTreeComputations<V>) : RedBlackTree2<@UnsafeVariance V>(computations)
        where V : LengthNodeValue, V : Comparable<@UnsafeVariance V>, V : DebuggableNode<in @UnsafeVariance V> {

    fun findNodeByCharIndex(index: Int): RedBlackTree<V>.Node? {
        var find = index
        return findNode {
            when (find) {
                in Int.MIN_VALUE until it.value.leftStringLength -> -1
                it.value.leftStringLength, in it.value.leftStringLength until it.value.leftStringLength + it.value.bufferLength -> {
                    if (it.left.isNotNil() && find == it.value.leftStringLength && it.left.value.bufferLength == 0) {
                        -1
                    } else {
                        0
                    }
                }
                in it.value.leftStringLength + it.value.bufferLength until Int.MAX_VALUE -> 1.also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftStringLength + it.value.bufferLength
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }
    }

    fun findNodeByRenderCharIndex(index: Int): RedBlackTree<V>.Node? {
        var find = index
        return findNode {
            when (find) {
                in Int.MIN_VALUE until it.value.leftRenderLength -> -1
                in it.value.leftRenderLength until it.value.leftRenderLength + it.value.currentRenderLength -> 0
                in it.value.leftRenderLength + it.value.currentRenderLength until Int.MAX_VALUE -> 1.also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftRenderLength + it.value.currentRenderLength
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }
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

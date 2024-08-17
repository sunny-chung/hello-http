package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.util.findAllIndicesOfChar
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree.Node
import kotlin.random.Random

class BigTextNodeValue : Comparable<BigTextNodeValue>, DebuggableNode<BigTextNodeValue> {
    var leftNumOfLineBreaks: Int = -1
    var leftNumOfRows: Int = -1
    var leftLastRowWidth: Int = -1
    var leftStringLength: Int = -1
//    var rowOffsetStarts: List<Int> = emptyList()

    var bufferIndex: Int = -1
    var bufferOffsetStart: Int = -1
    var bufferOffsetEndExclusive: Int = -1
    var bufferNumLineBreaksInRange: Int = -1

    val bufferLength: Int
        get() = bufferOffsetEndExclusive - bufferOffsetStart

    internal var node: RedBlackTree<BigTextNodeValue>.Node? = null

    private val key = Random.nextInt()

    override fun attach(node: RedBlackTree<BigTextNodeValue>.Node) {
        this.node = node
    }

    override fun detach() {
        node = null
    }

    override fun compareTo(other: BigTextNodeValue): Int {
        return compareValues(leftStringLength, other.leftStringLength)
    }

    override fun debugKey(): String = "$key"
    override fun debugLabel(node: RedBlackTree<BigTextNodeValue>.Node): String =
        "$leftStringLength [$bufferIndex: $bufferOffsetStart ..< $bufferOffsetEndExclusive] L ${node.length()}"
}

class TextBuffer(val size: Int) {
    private val buffer = StringBuilder(size)

    var lineOffsetStarts: List<Int> = emptyList()
//    var lineOffsetStarts: SortedSet<Int> = sortedSetOf()
//    var rowOffsetStarts: List<Int> = emptyList()

    val length: Int
        get() = buffer.length

    fun append(text: String): IntRange {
        val start = buffer.length
        buffer.append(text)
//        text.forEachIndexed { index, c ->
//            if (c == '\n') {
//                lineOffsetStarts += start + index
//            }
//        }
        text.findAllIndicesOfChar('\n').forEach {
            lineOffsetStarts += start + it
        }
        return start until start + text.length
    }

    override fun toString(): String {
        return buffer.toString()
    }

    fun subSequence(start: Int, endExclusive: Int): CharSequence {
        return buffer.subSequence(start, endExclusive)
    }
}
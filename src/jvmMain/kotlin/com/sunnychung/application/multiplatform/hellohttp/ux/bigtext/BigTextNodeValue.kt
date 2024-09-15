package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.util.findAllIndicesOfChar
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree
import java.util.SortedSet
import kotlin.random.Random

open class BigTextNodeValue : Comparable<BigTextNodeValue>, DebuggableNode<BigTextNodeValue>, LengthNodeValue {
    var leftNumOfLineBreaks: Int = -1
    var leftNumOfRowBreaks: Int = -1
    override var leftStringLength: Int = -1
//    var rowBreakOffsets: SortedSet<Int> = sortedSetOf()
    /**
     * Row break positions in the domain of character indices of the {bufferIndex}-th buffer.
     */
    var rowBreakOffsets: List<Int> = emptyList()
    var lastRowWidth: Float = 0f
    var isEndWithForceRowBreak: Boolean = false

    var bufferIndex: Int = -1
    var bufferOffsetStart: Int = -1
    var bufferOffsetEndExclusive: Int = -1
    var bufferNumLineBreaksInRange: Int = -1
    lateinit var buffer: TextBuffer
    var bufferOwnership: BufferOwnership = BufferOwnership.Owned

    override val bufferLength: Int
        get() = bufferOffsetEndExclusive - bufferOffsetStart

    override val leftOverallLength: Int
        get() = leftStringLength

    override val currentOverallLength: Int
        get() = bufferLength

    override val leftRenderLength: Int
        get() = leftStringLength

    override val currentRenderLength: Int
        get() = bufferLength

    open val renderBufferStart: Int
        get() = bufferOffsetStart

    open val renderBufferEndExclusive: Int
        get() = bufferOffsetEndExclusive

    internal var node: RedBlackTree<BigTextNodeValue>.Node? = null

    private val key = Random.nextInt()

    override fun attach(node: RedBlackTree<BigTextNodeValue>.Node) {
        this.node = node
    }

    override fun detach() {
        node = null
    }

    override fun compareTo(other: BigTextNodeValue): Int {
        return compareValues(leftOverallLength, other.leftOverallLength)
    }

    override fun debugKey(): String = "$key"
    override fun debugLabel(node: RedBlackTree<BigTextNodeValue>.Node): String =
        "$leftStringLength [$bufferIndex: $bufferOffsetStart ..< $bufferOffsetEndExclusive] L ${node.length()} r $leftNumOfRowBreaks/$rowBreakOffsets lw $lastRowWidth $isEndWithForceRowBreak '${buffer.subSequence(renderBufferStart, renderBufferEndExclusive).toString().replace("\n", "\\n")}'"
}

class TextBuffer(val size: Int) {
    private val buffer = StringBuilder(size)

    /**
     * Line break positions in the domain of character indices of this buffer.
     */
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
        if (start >= endExclusive) {
            return ""
        }
        return buffer.subSequence(start, endExclusive)
    }
}

enum class BufferOwnership {
    Owned, Delegated
}

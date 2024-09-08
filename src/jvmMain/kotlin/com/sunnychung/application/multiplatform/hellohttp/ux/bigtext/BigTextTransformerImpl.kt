package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

class BigTextTransformerImpl(private val delegate: BigTextImpl) : BigTextImpl(chunkSize = delegate.chunkSize) {

    override val tree: LengthTree<BigTextNodeValue> = LengthTree<BigTextTransformNodeValue>(
        object : RedBlackTreeComputations<BigTextTransformNodeValue> {
            override fun recomputeFromLeaf(it: RedBlackTree<BigTextTransformNodeValue>.Node) = recomputeAggregatedValues(it as RedBlackTree<BigTextNodeValue>.Node)
            override fun computeWhenLeftRotate(x: BigTextTransformNodeValue, y: BigTextTransformNodeValue) {}
            override fun computeWhenRightRotate(x: BigTextTransformNodeValue, y: BigTextTransformNodeValue) {}
        }
    )

    private fun BigTextNodeValue.toBigTextTransformNodeValue() : BigTextTransformNodeValue {
        return BigTextTransformNodeValue().also {
            it.leftNumOfLineBreaks = leftNumOfLineBreaks
            it.leftNumOfRowBreaks = leftNumOfRowBreaks
            it.leftStringLength = leftStringLength
            it.rowBreakOffsets = rowBreakOffsets.toList()
            it.lastRowWidth = lastRowWidth
            it.isEndWithForceRowBreak = isEndWithForceRowBreak
            it.bufferOffsetStart = bufferOffsetStart
            it.bufferOffsetEndExclusive = bufferOffsetEndExclusive
            it.bufferNumLineBreaksInRange = bufferNumLineBreaksInRange
            it.buffer = buffer // copy by ref
            it.bufferOwnership = BufferOwnership.Delegated
        }
    }

    fun RedBlackTree<BigTextNodeValue>.Node.toBigTextTransformNode(parentNode: RedBlackTree<BigTextTransformNodeValue>.Node) : RedBlackTree<BigTextTransformNodeValue>.Node {
        if (this === delegate.tree.NIL) {
            return (tree as LengthTree<BigTextTransformNodeValue>).NIL
        }

        return (tree as LengthTree<BigTextTransformNodeValue>).Node(
            value.toBigTextTransformNodeValue(),
            color,
            parentNode,
            tree.NIL,
            tree.NIL,
        ).also {
            it.left = left.toBigTextTransformNode(it)
            it.right = right.toBigTextTransformNode(it)
        }
    }

    init {
        (tree as LengthTree<BigTextTransformNodeValue>).setRoot(delegate.tree.getRoot().toBigTextTransformNode(tree.NIL))
        layouter = delegate.layouter
        contentWidth = delegate.contentWidth
    }

    override val length: Int
        get() = (tree as LengthTree<BigTextTransformNodeValue>).getRoot().renderLength()

    val originalLength: Int
        get() = tree.getRoot().length()

    override fun createNodeValue(): BigTextNodeValue {
        return BigTextTransformNodeValue()
    }

    fun insertOriginal(pos: Int, nodeValue: BigTextNodeValue) { // FIXME call me
        require(pos in 0 .. originalLength) { "Out of bound. pos = $pos, originalLength = $originalLength" }

        insertChunkAtPosition(
            position = pos,
            chunkedStringLength = nodeValue.bufferLength,
            ownership = BufferOwnership.Delegated,
            buffer = nodeValue.buffer,
            range = nodeValue.bufferOffsetStart until nodeValue.bufferOffsetEndExclusive
        ) {
            bufferIndex = -1
            bufferOffsetStart = nodeValue.bufferOffsetStart
            bufferOffsetEndExclusive = nodeValue.bufferOffsetEndExclusive
            this.buffer = nodeValue.buffer
            this.bufferOwnership = BufferOwnership.Delegated

            leftStringLength = 0
        }
    }

    private fun transformInsertChunkAtPosition(position: Int, chunkedString: String) {
        log.d { "transformInsertChunkAtPosition($position, $chunkedString)" }
        require(chunkedString.length <= chunkSize)
        var buffer = if (buffers.isNotEmpty()) {
            buffers.last().takeIf { it.length + chunkedString.length <= chunkSize }
        } else null
        if (buffer == null) {
            buffer = TextBuffer(chunkSize)
            buffers += buffer
        }
        require(buffer.length + chunkedString.length <= chunkSize)
        val range = buffer.append(chunkedString)
        insertChunkAtPosition(position, chunkedString.length, BufferOwnership.Owned, buffer, range) {
            this as BigTextTransformNodeValue
            bufferIndex = -1
            bufferOffsetStart = -1
            bufferOffsetEndExclusive = -1
            transformedBufferStart = range.start
            transformedBufferEndExclusive = range.endInclusive + 1
            this.buffer = buffer
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
    }

    fun transformInsert(pos: Int, text: String): Int {
        require(pos in 0 .. originalLength) { "Out of bound. pos = $pos, originalLength = $originalLength" }

        /**
         * As insert position is searched by leftmost of original string position,
         * the insert is done by inserting to the same point in reverse order,
         * which is different from BigTextImpl#insertAt.
         */

        var start = text.length
        var last = buffers.lastOrNull()?.length
        while (start > 0) {
            if (last == null || last >= chunkSize) {
//                buffers += TextBuffer()
                last = 0
            }
            val available = chunkSize - last
            val append = minOf(available, start)
            start -= append
            transformInsertChunkAtPosition(pos, text.substring(start until start + append))
            last = buffers.last().length
        }
        layout(maxOf(0, pos - 1), minOf(length, pos + text.length + 1))
        return text.length
    }

    fun deleteOriginal(originalRange: IntRange) { // FIXME call me
        require((originalRange.endInclusive + 1) in 0 .. originalLength) { "Out of bound. endExclusive = ${originalRange.endInclusive + 1}, originalLength = $originalLength" }
        super.delete(originalRange)
    }

    fun transformDelete(originalRange: IntRange): Int {
        require(originalRange.start <= originalRange.endInclusive + 1) { "start should be <= endExclusive" }
        require(0 <= originalRange.start) { "Invalid start" }
        require(originalRange.endInclusive + 1 <= originalLength) { "endExclusive is out of bound" }

        if (originalRange.start == originalRange.endInclusive + 1) {
            return 0
        }

        val startNode = tree.findNodeByCharIndex(originalRange.start)!!
        val buffer = startNode.value.buffer // the buffer is not used. just to prevent NPE
        super.delete(originalRange)
        insertChunkAtPosition(originalRange.start, originalRange.length, BufferOwnership.Owned, buffer, -2 .. -2) {
            this as BigTextTransformNodeValue
            bufferIndex = -1
            bufferOffsetStart = 0
            bufferOffsetEndExclusive = originalRange.length
            transformedBufferStart = -2
            transformedBufferEndExclusive = -2
            this.buffer = buffer
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
        return - originalRange.length
    }

    override fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue, left: RedBlackTree<BigTextNodeValue>.Node?) = with (nodeValue) {
        super.computeCurrentNodeProperties(nodeValue, left)

        this as BigTextTransformNodeValue
        left as RedBlackTree<BigTextTransformNodeValue>.Node?
        leftTransformedLength = left?.transformedOffset() ?: 0
        leftRenderLength = left?.renderLength() ?: 0
        leftOverallLength = left?.overallLength() ?: 0
    }
}

fun RedBlackTree<BigTextTransformNodeValue>.Node.transformedOffset(): Int =
    (getValue()?.leftTransformedLength ?: 0) +
            (getValue()?.currentTransformedLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.transformedOffset() ?: 0)

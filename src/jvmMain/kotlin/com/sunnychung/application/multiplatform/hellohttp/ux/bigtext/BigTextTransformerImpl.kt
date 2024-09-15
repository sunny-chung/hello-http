package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

val logT = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText.Transform")

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

            it.leftRenderLength = leftStringLength
            it.leftOverallLength = leftStringLength
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
            it.value.attach(it as RedBlackTree<BigTextNodeValue>.Node)
            val n = it as RedBlackTree<BigTextTransformNodeValue>.Node
            n.left = left.toBigTextTransformNode(n)
            n.right = right.toBigTextTransformNode(n)
        }
    }

    init {
        (tree as LengthTree<BigTextTransformNodeValue>).setRoot(delegate.tree.getRoot().toBigTextTransformNode(tree.NIL))
//        tree.visitInPostOrder {
//            recomputeAggregatedValues(it as RedBlackTree<BigTextNodeValue>.Node)
//        }
        delegate.layouter?.let { setLayouter(it) }
        delegate.contentWidth?.let { setContentWidth(it) }
        delegate.changeHook = object : BigTextChangeHook {
            override fun afterInsertChunk(modifiedText: BigText, position: Int, newValue: BigTextNodeValue) {
                insertOriginal(position, newValue)
            }
            override fun afterDelete(modifiedText: BigText, position: IntRange) {
                deleteOriginal(position)
            }
        }
    }

    override val length: Int
        get() = (tree as LengthTree<BigTextTransformNodeValue>).getRoot().renderLength()

    val originalLength: Int
        get() = tree.getRoot().length()

    override fun createNodeValue(): BigTextNodeValue {
        return BigTextTransformNodeValue()
    }

    fun insertOriginal(pos: Int, nodeValue: BigTextNodeValue) {
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
        logT.d { "transformInsertChunkAtPosition($position, $chunkedString)" }
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
        logT.d { "transformInsert($pos, \"$text\")" }
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
        val renderPositionStart = findRenderPositionStart(tree.findNodeByCharIndex(pos)!!)
        layout(maxOf(0, renderPositionStart - 1), minOf(length, renderPositionStart + text.length + 1))
//        layout()
        return text.length
    }

    fun transformInsertAtOriginalEnd(text: String): Int = transformInsert(originalLength, text)

    fun deleteOriginal(originalRange: IntRange) {
        require(0 <= originalRange.start) { "Invalid start" }
        require((originalRange.endInclusive + 1) in 0 .. originalLength) { "Out of bound. endExclusive = ${originalRange.endInclusive + 1}, originalLength = $originalLength" }
        super.deleteUnchecked(originalRange.start, originalRange.endInclusive + 1)
    }

    fun transformDelete(originalRange: IntRange): Int {
        logT.d { "transformDelete($originalRange)" }
        require(originalRange.start <= originalRange.endInclusive + 1) { "start should be <= endExclusive" }
        require(0 <= originalRange.start) { "Invalid start" }
        require(originalRange.endInclusive + 1 <= originalLength) { "endExclusive is out of bound" }

        if (originalRange.start == originalRange.endInclusive + 1) {
            return 0
        }

        val startNode = tree.findNodeByCharIndex(originalRange.start)!!
        val renderStartPos = findRenderPositionStart(startNode)
        val buffer = startNode.value.buffer // the buffer is not used. just to prevent NPE
        super.deleteUnchecked(originalRange.start, originalRange.endInclusive + 1)
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
        layout(maxOf(0, renderStartPos - 1), minOf(length, renderStartPos + 1))
//        tree.visitInPostOrder { recomputeAggregatedValues(it) } //
        logT.d { inspect("after transformDelete $originalRange") }
        return - originalRange.length
    }

    fun transformReplace(originalRange: IntRange, newText: String) {
        transformDelete(originalRange)
        transformInsert(originalRange.start, newText)
    }

    override fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue, left: RedBlackTree<BigTextNodeValue>.Node?) = with (nodeValue) {
        super.computeCurrentNodeProperties(nodeValue, left)

        this as BigTextTransformNodeValue
        left as RedBlackTree<BigTextTransformNodeValue>.Node?
        leftTransformedLength = left?.transformedOffset() ?: 0
        leftRenderLength = left?.renderLength() ?: 0
        leftOverallLength = left?.overallLength() ?: 0
    }

    override fun insertAt(pos: Int, text: String): Int = transformInsert(pos, text)

    override fun append(text: String): Int = transformInsertAtOriginalEnd(text)

    override fun delete(start: Int, endExclusive: Int): Int = transformDelete(start until endExclusive)

    override fun replace(range: IntRange, text: String) = transformReplace(range, text)
}

fun RedBlackTree<BigTextTransformNodeValue>.Node.transformedOffset(): Int =
    (getValue()?.leftTransformedLength ?: 0) +
            (getValue()?.currentTransformedLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.transformedOffset() ?: 0)

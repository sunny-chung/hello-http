package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.extension.intersect
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.extension.toNonEmptyRange
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

val logT = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText.Transform")

class BigTextTransformerImpl(internal val delegate: BigText) : BigTextImpl(
    chunkSize = delegate.chunkSize,
    textBufferFactory = delegate.textBufferFactory,
    charSequenceBuilderFactory = delegate.charSequenceBuilderFactory,
    charSequenceFactory = delegate.charSequenceFactory,
), BigTextTransformed {

    private var hasReachedExtensiveSearch: Boolean = false

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
            it.renderNumLineBreaksInRange = renderNumLineBreaksInRange
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

    override val numOfOriginalLines: Int
        get() = delegate.numOfOriginalLines

    // not thread-safe
    val charRangesToReapplyTransforms = mutableSetOf<IntRange>()

    override fun createNodeValue(): BigTextNodeValue {
        return BigTextTransformNodeValue()
    }

    fun insertOriginal(
        pos: Int,
        nodeValue: BigTextNodeValue,
        bufferOffsetStart: Int = nodeValue.bufferOffsetStart,
        bufferOffsetEndExclusive: Int = nodeValue.bufferOffsetEndExclusive,
    ) {
        require(pos in 0..originalLength) { "Out of bound. pos = $pos, originalLength = $originalLength" }

        val renderPos = findTransformedPositionByOriginalPosition(pos)

        insertChunkAtPosition(
            position = pos,
            chunkedStringLength = bufferOffsetEndExclusive - bufferOffsetStart,
            ownership = BufferOwnership.Delegated,
            buffer = nodeValue.buffer,
            range = bufferOffsetStart until bufferOffsetEndExclusive
        ) {
            bufferIndex = -1
            this.bufferOffsetStart = bufferOffsetStart
            this.bufferOffsetEndExclusive = bufferOffsetEndExclusive
            this.buffer = nodeValue.buffer
            this.bufferOwnership = BufferOwnership.Delegated

            leftStringLength = 0
        }

        val insertLength = bufferOffsetEndExclusive - bufferOffsetStart
        layout(maxOf(0, renderPos - 1), minOf(length, renderPos + insertLength + 1))
    }

    private fun transformInsertChunkAtPosition(position: Int, chunkedString: CharSequence, offsetMapping: BigTextTransformOffsetMapping, incrementalTransformOffsetMappingLength: Int, isReplaceOriginal: Boolean): Int {
        logT.d { "transformInsertChunkAtPosition($position, $chunkedString)" }
        require(chunkedString.length <= chunkSize)
        var buffer = if (buffers.isNotEmpty()) {
            buffers.last().takeIf { it.length + chunkedString.length <= chunkSize }
        } else null
        if (buffer == null) {
            buffer = textBufferFactory(chunkSize)
            buffers += buffer
        }
        require(buffer.length + chunkedString.length <= chunkSize)
        val range = buffer.append(chunkedString)
        return insertChunkAtPosition(position, chunkedString.length, BufferOwnership.Owned, buffer, range, true) {
            this as BigTextTransformNodeValue
            bufferIndex = -1
            if (isReplaceOriginal && incrementalTransformOffsetMappingLength > 0) {
                bufferOffsetStart = position
                bufferOffsetEndExclusive = position + incrementalTransformOffsetMappingLength
            } else {
                bufferOffsetStart = -1
                bufferOffsetEndExclusive = -1
            }
            transformedBufferStart = range.start
            transformedBufferEndExclusive = range.endInclusive + 1
            transformOffsetMapping = offsetMapping
            this.incrementalTransformOffsetMappingLength = incrementalTransformOffsetMappingLength
            this.buffer = buffer
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
    }

    fun transformInsert(pos: Int, text: CharSequence): Int {
        return transformInsert(pos, text, BigTextTransformOffsetMapping.WholeBlock, 0, false)
    }

    private fun transformInsert(pos: Int, text: CharSequence, offsetMapping: BigTextTransformOffsetMapping, incrementalTransformOffsetMappingLength: Int, isReplaceOriginal: Boolean): Int {
        logT.d { "transformInsert($pos, \"$text\")" }
        require(pos in 0 .. originalLength) { "Out of bound. pos = $pos, originalLength = $originalLength" }

        var renderPositionStart: Int? = null

        when (offsetMapping) {
            BigTextTransformOffsetMapping.WholeBlock -> {
//                /**
//                 * As insert position is searched by leftmost of original string position,
//                 * the insert is done by inserting to the same point in reverse order,
//                 * which is different from BigTextImpl#insertAt.
//                 */
//
//                var start = text.length
//                var last = buffers.lastOrNull()?.length
////                var isTheLastChunk = true
//                while (start > 0) {
//                    if (last == null || last >= chunkSize) {
////                buffers += TextBuffer()
//                        last = 0
//                    }
//                    val available = chunkSize - last
//                    val append = minOf(available, start)
//                    start -= append
//
//                    val incrementalOffsetLength = maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start))
//                    // the last chunk values at least 1-char length
////            val incrementalOffsetLength = if (isTheLastChunk && incrementalTransformOffsetMappingLength > 0) {
////                maxOf(1, minOf(append, incrementalTransformOffsetMappingLength - start))
////            } else {
////                maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start - 1))
////            }
//                    transformInsertChunkAtPosition(pos, text.subSequence(start until start + append), offsetMapping, incrementalOffsetLength, isReplaceOriginal)
//                    last = buffers.last().length
////                    isTheLastChunk = false
//                }

                var start = 0
                var last = buffers.lastOrNull()?.length
                while (start < text.length) {
                    if (last == null || last >= chunkSize) {
//                buffers += TextBuffer()
                        last = 0
                    }
                    val available = chunkSize - last
                    val append = minOf(available, text.length - start)

                    val incrementalOffsetLength = maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start))
                    // the last chunk values at least 1-char length
//            val incrementalOffsetLength = if (isTheLastChunk && incrementalTransformOffsetMappingLength > 0) {
//                maxOf(1, minOf(append, incrementalTransformOffsetMappingLength - start))
//            } else {
//                maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start - 1))
//            }
                    transformInsertChunkAtPosition(pos, text.subSequence(start until start + append), offsetMapping, incrementalOffsetLength, isReplaceOriginal).let {
                        if (renderPositionStart == null) {
                            renderPositionStart = it
                        }
                    }
                    start += append
                    last = buffers.last().length
                }
            }

            BigTextTransformOffsetMapping.Incremental -> {
                var start = 0
                var last = buffers.lastOrNull()?.length
                var insertOffset = 0
                while (start < text.length) {
                    if (last == null || last >= chunkSize) {
//                buffers += TextBuffer()
                        last = 0
                    }
                    val available = chunkSize - last
                    val append = minOf(available, text.length - start)

                    val incrementalOffsetLength = maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start))
                    // the last chunk values at least 1-char length
//            val incrementalOffsetLength = if (isTheLastChunk && incrementalTransformOffsetMappingLength > 0) {
//                maxOf(1, minOf(append, incrementalTransformOffsetMappingLength - start))
//            } else {
//                maxOf(0, minOf(append, incrementalTransformOffsetMappingLength - start - 1))
//            }
                    transformInsertChunkAtPosition(pos + insertOffset, text.subSequence(start until start + append), offsetMapping, incrementalOffsetLength, isReplaceOriginal).let {
                        if (renderPositionStart == null) {
                            renderPositionStart = it
                        }
                    }
                    insertOffset += incrementalOffsetLength
                    start += append
                    last = buffers.last().length
                }
            }
        }

        if (renderPositionStart == null) {
            renderPositionStart = 0
        }

//        val renderPositionStart = findRenderPositionStart(tree.findNodeByCharIndex(pos)!!)
        layout(maxOf(0, renderPositionStart!! - 1), minOf(length, renderPositionStart!! + text.length + 1))
//        layout()
        return text.length
    }

    fun transformInsertAtOriginalEnd(text: CharSequence): Int = transformInsert(originalLength, text)

    fun deleteOriginal(originalRange: IntRange, isReMapPositionNeeded: Boolean = true) {
        require(0 <= originalRange.start) { "Invalid start" }
        require((originalRange.endInclusive + 1) in 0 .. originalLength) { "Out of bound. endExclusive = ${originalRange.endInclusive + 1}, originalLength = $originalLength" }
        val renderPositionStart = findTransformedPositionByOriginalPosition(originalRange.start)
        if (isReMapPositionNeeded) {
            super.deleteUnchecked(
                start = findOriginalPositionByTransformedPosition(renderPositionStart),
                endExclusive = findOriginalPositionByTransformedPosition(
                    findTransformedPositionByOriginalPosition(
                        originalRange.endInclusive
                    )
                ) + 1,
                deleteMarker = null,
                isSkipLayout = true
            )
        } else {
            super.deleteUnchecked(
                start = originalRange.start,
                endExclusive = originalRange.endInclusive + 1,
                deleteMarker = null,
                isSkipLayout = true
            )
        }
        layout(maxOf(0, renderPositionStart - 1), minOf(length, renderPositionStart + 1))
    }

    fun transformDelete(originalRange: IntRange): Int {
        return transformDelete(originalRange = originalRange, isAddMarker = true, deleteMarkerRange = originalRange)
    }

    private fun transformDelete(originalRange: IntRange, isAddMarker: Boolean, deleteMarkerRange: IntRange): Int {
        logT.d { "transformDelete($originalRange)" }
        require(originalRange.start <= originalRange.endInclusive + 1) { "start should be <= endExclusive" }
        require(0 <= originalRange.start) { "Invalid start" }
        require(originalRange.endInclusive + 1 <= originalLength) { "endExclusive is out of bound" }

        if (originalRange.start == originalRange.endInclusive + 1) {
            return 0
        }

        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "transformDelete -- before findNodeByCharIndex" }
        val startNode = tree.findNodeByCharIndex(originalRange.start)!!
        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "transformDelete -- after findNodeByCharIndex" }
        val renderStartPos = findRenderPositionStart(startNode)
        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "transformDelete -- after findRenderPositionStart" }
        val buffer = startNode.value.buffer // the buffer is not used. just to prevent NPE
        super.deleteUnchecked(
            start = originalRange.start,
            endExclusive = originalRange.endInclusive + 1,
            deleteMarker = if (isAddMarker) createDeleteMarkerNodeValue(deleteMarkerRange) else null,
            isSkipLayout = true,
        )
        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "transformDelete -- after deleteUnchecked" }
        if (isAddMarker) {
//            insertDeleteMarker(originalRange)
        }
        layout(maxOf(0, renderStartPos - 1), minOf(length, renderStartPos + 1))
        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "transformDelete -- after layout" }
//        tree.visitInPostOrder { recomputeAggregatedValues(it) } //
        logT.d { inspect("after transformDelete $originalRange") }
        return - originalRange.length
    }

    private fun createDeleteMarkerNodeValue(originalRange: IntRange): BigTextNodeValue {
        val dummyBuffer = StringTextBuffer(1)
        return createNodeValue().apply {
            this as BigTextTransformNodeValue
            bufferIndex = -1
            bufferOffsetStart = 0
            bufferOffsetEndExclusive = originalRange.length
            transformedBufferStart = -2
            transformedBufferEndExclusive = -2
            this.buffer = dummyBuffer
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
    }

    private fun insertDeleteMarker(originalRange: IntRange) {
        val dummyBuffer = StringTextBuffer(1)
        insertChunkAtPosition(originalRange.start, originalRange.length, BufferOwnership.Owned, dummyBuffer, -2..-2, true) {
            this as BigTextTransformNodeValue
            bufferIndex = -1
            bufferOffsetStart = 0
            bufferOffsetEndExclusive = originalRange.length
            transformedBufferStart = -2
            transformedBufferEndExclusive = -2
            this.buffer = dummyBuffer
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
    }

    /**
     * Use cases:
     * 1. Delete all transform operations within a range
     * 2. Delete some transform operations that fulfills the filter within a range
     * 3. Replace all transformed inserts within a range with a new insert
     */
    fun deleteTransformIf(originalRange: IntRange, filter: (BigTextTransformNodeValue) -> Boolean = { it.currentTransformedLength > 0 }): Int {
        logT.d { "deleteTransformIf($originalRange)" }
        require(originalRange.start <= originalRange.endInclusive + 1) { "start should be <= endExclusive" }
        require(0 <= originalRange.start) { "Invalid start" }
        require(originalRange.endInclusive + 1 <= originalLength) { "endExclusive is out of bound" }

        if (originalRange.start == originalRange.endInclusive + 1) {
            return 0
        }

        val startNode = tree.findNodeByCharIndex(originalRange.start)!!
        val endNode = tree.findNodeByCharIndex(originalRange.endInclusive + 1)!!
        val renderStartPos = findRenderPositionStart(startNode)
//        val renderEndPos = findRenderPositionStart(endNode) + endNode.value.currentRenderLength
        val renderEndPos = findTransformedPositionByOriginalPosition(originalRange.endInclusive + 1)

        var node: RedBlackTree<BigTextNodeValue>.Node? = endNode
        var nodeRange = charIndexRangeOfNode(node!!)
        val newNodesInDescendingOrder = mutableListOf<BigTextNodeValue>()
        while (node?.isNotNil() == true && (originalRange.start <= nodeRange.endInclusive || originalRange.start <= nodeRange.start)) {
            val prev = tree.prevNode(node)
            logT.d { "DTI nodeRange=$nodeRange, o=${node!!.value.bufferOwnership.name.first()}, int=${!(originalRange.toNonEmptyRange() intersect nodeRange.toNonEmptyRange()).isEmpty()}, f=${filter(node!!.value as BigTextTransformNodeValue)}" }
            if (!(originalRange.toNonEmptyRange() intersect nodeRange.toNonEmptyRange()).isEmpty()
                && node.value.bufferOwnership == BufferOwnership.Owned
                && filter(node.value as BigTextTransformNodeValue)
            ) {
                if (originalRange.endInclusive in nodeRange.start..nodeRange.last - 1) {
                    // need to split
                    val splitAtIndex = originalRange.endInclusive + 1 - nodeRange.start
                    logT.d { "T Split E at $splitAtIndex" }
                    newNodesInDescendingOrder += createNodeValue().apply { // the second part of the existing string
                        bufferIndex = node!!.value.bufferIndex
                        bufferOffsetStart = node!!.value.bufferOffsetStart + splitAtIndex
                        bufferOffsetEndExclusive = node!!.value.bufferOffsetEndExclusive
                        buffer = node!!.value.buffer
                        bufferOwnership = node!!.value.bufferOwnership

                        leftStringLength = 0

                        this as BigTextTransformNodeValue
                        val nv = node!!.value as BigTextTransformNodeValue
                        transformedBufferStart = nv.transformedBufferStart
                        transformedBufferEndExclusive = nv.transformedBufferEndExclusive
                    }
                }
                if (originalRange.start in nodeRange.start + 1..nodeRange.last) {
                    // need to split
                    val splitAtIndex = originalRange.start - nodeRange.start
                    logT.d { "T Split S at $splitAtIndex" }
                    newNodesInDescendingOrder += createNodeValue().apply { // the first part of the existing string
                        bufferIndex = node!!.value.bufferIndex
                        bufferOffsetStart = node!!.value.bufferOffsetStart
                        bufferOffsetEndExclusive = node!!.value.bufferOffsetStart + splitAtIndex
                        buffer = node!!.value.buffer
                        bufferOwnership = node!!.value.bufferOwnership

                        leftStringLength = 0

                        this as BigTextTransformNodeValue
                        val nv = node!!.value as BigTextTransformNodeValue
                        transformedBufferStart = nv.transformedBufferStart
                        transformedBufferEndExclusive = nv.transformedBufferEndExclusive
                    }
                }
                logT.d { "T Delete node ${node!!.value.debugKey()} at ${nodeRange.start} .. ${nodeRange.last}" }
                if (nodeRange.start == 2083112) {
                    isD = true
                }
                tree.delete(node)
                logT.v { inspect("T After delete " + node?.value?.debugKey()) }
            }
            node = prev
//            nodeRange = nodeRange.start - chunkSize .. nodeRange.last - chunkSize
            if (node != null) {
                nodeRange = charIndexRangeOfNode(node) // TODO optimize by calculation instead of querying
                logT.d { "new range = $nodeRange" }
            }
        }

        newNodesInDescendingOrder.asReversed().forEach {
            if (node != null) {
                node = tree.insertRight(node!!, it)
            } else if (!tree.isEmpty) { // no previous node, so insert at leftmost of the tree
                val leftmost = tree.leftmost(tree.getRoot())
                node = tree.insertLeft(leftmost, it)
            } else {
                node = tree.insertValue(it)
            }
        }

        layout(maxOf(0, renderStartPos - 1), minOf(length, renderEndPos + 1))
//        tree.visitInPostOrder { recomputeAggregatedValues(it) } //
        logT.d { inspect("after deleteTransformIf $originalRange") }
        return - originalRange.length
    }

    fun transformReplace(originalRange: IntRange, newText: CharSequence, offsetMapping: BigTextTransformOffsetMapping = BigTextTransformOffsetMapping.Incremental) {
        logT.d { "transformReplace($originalRange, $newText, $offsetMapping)" }
//        deleteTransformIf(originalRange)
        val incrementalTransformOffsetMappingLength = if (offsetMapping == BigTextTransformOffsetMapping.Incremental) {
            minOf(originalRange.length, newText.length)
        } else {
            0
        }
        com.sunnychung.application.multiplatform.hellohttp.util.log.d { "transformReplace -- before transformDelete" }
//        transformDelete(originalRange = originalRange, isAddMarker = incrementalTransformOffsetMappingLength <= 0)
        transformDelete(originalRange = originalRange, isAddMarker = incrementalTransformOffsetMappingLength <= 0 || originalRange.length > incrementalTransformOffsetMappingLength, deleteMarkerRange = if (incrementalTransformOffsetMappingLength <= 0) {
            originalRange
        } else {
            originalRange.endInclusive + 1 - maxOf(0, originalRange.length - incrementalTransformOffsetMappingLength) .. originalRange.endInclusive
        })
        com.sunnychung.application.multiplatform.hellohttp.util.log.d { "transformReplace -- before transformInsert" }
        transformInsert(
            pos = originalRange.start,
            text = newText,
            offsetMapping = offsetMapping,
            incrementalTransformOffsetMappingLength = incrementalTransformOffsetMappingLength,
//            incrementalTransformOffsetMappingLength = incrementalTransformOffsetMappingLength - 1,
            isReplaceOriginal = incrementalTransformOffsetMappingLength > 0,
        )
        com.sunnychung.application.multiplatform.hellohttp.util.log.d { "transformReplace -- after transformInsert" }
//        if (incrementalTransformOffsetMappingLength > 0 && originalRange.length > incrementalTransformOffsetMappingLength) {
//            insertDeleteMarker(originalRange.endInclusive + 1 - maxOf(0, originalRange.length - incrementalTransformOffsetMappingLength) .. originalRange.endInclusive)
//        }
    }

    override fun updateRightValueDuringNodeSplit(
        rightNodeValue: BigTextNodeValue,
        oldNodeValue: BigTextNodeValue,
        splitAtIndex: Int
    ) {
        super.updateRightValueDuringNodeSplit(rightNodeValue, oldNodeValue, splitAtIndex)
        with (rightNodeValue as BigTextTransformNodeValue) {
            oldNodeValue as BigTextTransformNodeValue
            transformOffsetMapping = oldNodeValue.transformOffsetMapping
            incrementalTransformOffsetMappingLength = maxOf(0, oldNodeValue.incrementalTransformOffsetMappingLength - splitAtIndex)
            if (oldNodeValue.transformedBufferStart >= 0) {
                transformedBufferStart = oldNodeValue.transformedBufferStart + splitAtIndex
                transformedBufferEndExclusive = oldNodeValue.transformedBufferEndExclusive
            }
        }
    }

    override fun updateLeftValueDuringNodeSplit(
        leftNodeValue: BigTextNodeValue,
        oldNodeValue: BigTextNodeValue,
        splitAtIndex: Int
    ) {
        super.updateLeftValueDuringNodeSplit(leftNodeValue, oldNodeValue, splitAtIndex)
        with (leftNodeValue as BigTextTransformNodeValue) {
            oldNodeValue as BigTextTransformNodeValue
            transformOffsetMapping = oldNodeValue.transformOffsetMapping
            incrementalTransformOffsetMappingLength = minOf(splitAtIndex, oldNodeValue.incrementalTransformOffsetMappingLength)
            if (oldNodeValue.transformedBufferStart >= 0) {
                transformedBufferStart = oldNodeValue.transformedBufferStart
                transformedBufferEndExclusive = oldNodeValue.transformedBufferStart + splitAtIndex
            }
        }
    }

    override fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue, left: RedBlackTree<BigTextNodeValue>.Node?) = with (nodeValue) {
        super.computeCurrentNodeProperties(nodeValue, left)

        this as BigTextTransformNodeValue
        left as RedBlackTree<BigTextTransformNodeValue>.Node?
        leftTransformedLength = left?.transformedOffset() ?: 0
        leftRenderLength = left?.renderLength() ?: 0
        leftOverallLength = left?.overallLength() ?: 0
    }

    internal fun resetDebugFlags() {
        hasReachedExtensiveSearch = false
    }

    internal fun hasReachedExtensiveSearch() = hasReachedExtensiveSearch

    override fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int {
        // TODO this function can be further optimized
        if (originalPosition == originalLength) { // the retrieved 'node' is incorrect for the last position
            return length
        }
        val node = tree.findNodeByCharIndex(originalPosition, isIncludeMarkerNodes = false)
            ?: throw IndexOutOfBoundsException("Node at original position $originalPosition not found")
        val nodeStart = findPositionStart(node)
        val indexFromNodeStart = originalPosition - nodeStart
        val firstMarkerNode = tree.findNodeByCharIndex(nodeStart, isIncludeMarkerNodes = true)
            ?: throw IndexOutOfBoundsException("Node at original position $nodeStart not found")
        val transformedStart = findRenderPositionStart(node)
        if (firstMarkerNode === node) {
            return transformedStart + if (node.value.bufferOwnership == BufferOwnership.Delegated) {
                indexFromNodeStart
            } else if ((node.value as? BigTextTransformNodeValue)?.transformOffsetMapping == BigTextTransformOffsetMapping.Incremental) {
//                indexFromNodeStart - (node.value as BigTextTransformNodeValue).incrementalTransformOffsetMappingLength
                minOf((node.value as BigTextTransformNodeValue).incrementalTransformOffsetMappingLength, indexFromNodeStart)
            } else {
                node.value.currentRenderLength
            }
        }

//        val nodeStartBeforeMarkers = findPositionStart(node)
        hasReachedExtensiveSearch = true
        logT.d { "hasReachedExtensiveSearch" }

//        val transformedStartBeforeMarkers = findRenderPositionStart(firstMarkerNode)

//        var itOffset = 0
//        var compulsoryOffset = 0
//        var isNotYetFulfillIncrementalOffset = true
//        var n = firstMarkerNode as RedBlackTree<BigTextTransformNodeValue>.Node
//        while (true) {
//            if (/*isNotYetFulfillIncrementalOffset &&*/ n.value.currentTransformedLength > 0 && n !== node) {
//                when (n.value.transformOffsetMapping) {
//                    BigTextTransformOffsetMapping.WholeBlock -> {
//                        compulsoryOffset += n.value.currentTransformedLength
//                    }
//                    BigTextTransformOffsetMapping.Incremental -> {
////                        if (indexFromNodeStart in itOffset until itOffset + n.value.currentTransformedLength) {
////                            itOffset = indexFromNodeStart
////                            isNotYetFulfillIncrementalOffset = false
////                        }
////                        itOffset += n.value.currentTransformedLength
//                    }
//                }
//            } else if (n.value.bufferOwnership == BufferOwnership.Owned && n.value.currentTransformedLength < 0) {
//                compulsoryOffset += n.value.currentTransformedLength
//            }
//
//            if (n === node) {
//                break
//            }
//
//            n = tree.nextNode(n as RedBlackTree<BigTextNodeValue>.Node) as RedBlackTree<BigTextTransformNodeValue>.Node
//        }

//        return transformedStartBeforeMarkers + indexFromNodeStart + compulsoryOffset
//        return transformedStart + indexFromNodeStart + compulsoryOffset

        var incrementalTransformLength = 0
        var incrementalTransformLimit = 0
        var n = node as RedBlackTree<BigTextTransformNodeValue>.Node
        var isBreakIfReachAnyIncrementalChunk = false
        while (true) {
            if (n.value.transformOffsetMapping == BigTextTransformOffsetMapping.Incremental && n.value.transformedBufferEndExclusive - n.value.transformedBufferStart > 0) {
                if (isBreakIfReachAnyIncrementalChunk) { // this chunk does not own by this offset, but previous offset with buffer length at least 1
                    break
                }
                incrementalTransformLength += n.value.currentTransformedLength
                incrementalTransformLimit += n.value.incrementalTransformOffsetMappingLength
            } else if (n.value.transformOffsetMapping == BigTextTransformOffsetMapping.WholeBlock) {
                isBreakIfReachAnyIncrementalChunk = true
            }
            if (n === firstMarkerNode) {
                break
            }
            n = tree.prevNode(n as RedBlackTree<BigTextNodeValue>.Node) as RedBlackTree<BigTextTransformNodeValue>.Node
        }
        if (incrementalTransformLimit > 0) { // incremental replacement
//            return transformedStart - maxOf(0, incrementalTransformLength - minOf(incrementalTransformLimit, indexFromNodeStart))

//            val transformedStartBeforeMarkers = findRenderPositionStart(firstMarkerNode)
            return transformedStart + minOf(incrementalTransformLimit, indexFromNodeStart)
        }
//        return transformedStart + indexFromNodeStart
        return transformedStart + if (node.value.bufferOwnership == BufferOwnership.Delegated || incrementalTransformLength > 0) {
            indexFromNodeStart
        } else {
            0
//            node.value.currentRenderLength
        }
    }

    override fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int {
        // TODO this function can be further optimized
        if (transformedPosition == length) {
            return originalLength
        }
        val node = tree.findNodeByRenderCharIndex(transformedPosition)
            ?: throw IndexOutOfBoundsException("Node at transformed position $transformedPosition not found")
        val transformedStart = findRenderPositionStart(node)
        val indexFromNodeStart = transformedPosition - transformedStart
        val nodeStart = findPositionStart(node)
        val nv = node.value as BigTextTransformNodeValue

        val firstMarkerNode = tree.findNodeByCharIndex(nodeStart, isIncludeMarkerNodes = true)
            ?: throw IndexOutOfBoundsException("Node at original position $nodeStart not found")

        if (firstMarkerNode === node) {
            return nodeStart + if (nv.bufferOwnership == BufferOwnership.Delegated) {
                indexFromNodeStart
            } else if (nv.currentTransformedLength < 0) { // deletion
                -nv.currentTransformedLength
            } else if (nv.transformOffsetMapping == BigTextTransformOffsetMapping.Incremental) {
//                val incrementalLength = minOf(nv.currentTransformedLength, indexFromNodeStart)
                val incrementalLength = minOf(nv.incrementalTransformOffsetMappingLength, indexFromNodeStart)
                incrementalLength
            } else {
                0
            }
        }

        hasReachedExtensiveSearch = true
        logT.d { "hasReachedExtensiveSearch" }

//        val nodeStartBeforeMarkers = findRenderPositionStart(firstMarkerNode)
//
//        var itOffset = 0
//        var n = firstMarkerNode as RedBlackTree<BigTextTransformNodeValue>.Node
//        while (n !== node) {
//            if (n.value.currentTransformedLength > 0) {
//                when (n.value.transformOffsetMapping) {
//                    BigTextTransformOffsetMapping.WholeBlock -> {
//                    }
//                    BigTextTransformOffsetMapping.Incremental -> {
//                        if (indexFromNodeStart in itOffset until itOffset + n.value.currentTransformedLength) {
//                            itOffset = indexFromNodeStart
//                            break
//                        }
//                    }
//                }
//                itOffset += n.value.currentTransformedLength
//            }
//
//            n = tree.nextNode(n as RedBlackTree<BigTextNodeValue>.Node) as RedBlackTree<BigTextTransformNodeValue>.Node
//        }
//
//        return nodeStartBeforeMarkers + itOffset

        var n = firstMarkerNode as RedBlackTree<BigTextTransformNodeValue>.Node
        var incrementalTransformLength = 0
        var incrementalTransformLimit = 0
        var nonIncrementalTransformLength = 0
        while (true) {
            if (n.value.transformOffsetMapping == BigTextTransformOffsetMapping.Incremental && n.value.currentTransformedLength > 0) {
                if (n !== node) {
                    incrementalTransformLength += n.value.currentTransformedLength
                }
                incrementalTransformLimit += n.value.incrementalTransformOffsetMappingLength
            }
            if (n === node) {
                break
            }
            if (n.value.transformOffsetMapping != BigTextTransformOffsetMapping.Incremental && n.value.currentTransformedLength > 0) {
                nonIncrementalTransformLength += n.value.currentTransformedLength
            }
            n = tree.nextNode(n as RedBlackTree<BigTextNodeValue>.Node) as RedBlackTree<BigTextTransformNodeValue>.Node
        }
        if (incrementalTransformLimit > 0) { // incremental replacement
//            return nodeStart - maxOf(0, incrementalTransformLength - indexFromNodeStart)
//            return nodeStart - incrementalTransformLength + minOf(incrementalTransformLimit, indexFromNodeStart)

            val transformedStartBeforeMarkers = findRenderPositionStart(firstMarkerNode)
            val indexFromNodeStart2 = transformedPosition - transformedStartBeforeMarkers
            return nodeStart + minOf(incrementalTransformLimit, indexFromNodeStart2 - nonIncrementalTransformLength)
        }
        return nodeStart + minOf(node.value.bufferLength, indexFromNodeStart)
    }

    fun findFirstRowIndexByOriginalLineIndex(originalLineIndex: Int): Int {
        require(originalLineIndex in 0 .. delegate.numOfLines) { "Original line index $originalLineIndex is out of range." }
        if (delegate.tree.isEmpty && originalLineIndex == 0) {
            return 0
        }
        val (originalNode, originalNodeLineStart) = delegate.tree.findNodeByLineBreaksExact(originalLineIndex)
            ?: throw IllegalStateException("Node of line index $originalLineIndex is not found")
        val originalNodePositionStart = delegate.tree.findPositionStart(originalNode)
        val lineBreakIndex = originalLineIndex - originalNodeLineStart - 1
        val lineOriginalPositionStart = originalNodePositionStart + if (lineBreakIndex >= 0) {
            val lineOffsets = originalNode.value.buffer.lineOffsetStarts
            val lineOffsetStartIndex = lineOffsets.binarySearchForMinIndexOfValueAtLeast(originalNode.value.bufferOffsetStart)
            require(lineOffsetStartIndex >= 0)
            lineOffsets[lineOffsetStartIndex + lineBreakIndex] - originalNode.value.bufferOffsetStart + /* find the position just after the '\n' char */ 1
        } else {
            0
        }

        val transformedPosition = findTransformedPositionByOriginalPosition(lineOriginalPositionStart)
        return findRowIndexByPosition(transformedPosition)
    }

    fun findOriginalLineIndexByRowIndex(rowIndex: Int): Int {
        val rowPositionStart = findRowPositionStartIndexByRowIndex(rowIndex)
        val originalPosition = findOriginalPositionByTransformedPosition(rowPositionStart)
        val (originalLine, originalCol) = delegate.findLineAndColumnFromRenderPosition(originalPosition)
        return originalLine
    }

    override fun insertAt(pos: Int, text: CharSequence): Int = transformInsert(pos, text)

    override fun append(text: CharSequence): Int = transformInsertAtOriginalEnd(text)

    override fun delete(start: Int, endExclusive: Int): Int = transformDelete(start until endExclusive)

    override fun replace(range: IntRange, text: CharSequence) = transformReplace(range, text)

    override fun replace(range: IntRange, text: CharSequence, offsetMapping: BigTextTransformOffsetMapping) = transformReplace(range, text, offsetMapping)

    override fun restoreToOriginal(range: IntRange) {
        val renderPositionAtOriginalStart = findTransformedPositionByOriginalPosition(range.start)
        val renderPositionAtOriginalEnd = findTransformedPositionByOriginalPosition(range.endInclusive)

        deleteTransformIf(range)
        deleteOriginal(range, isReMapPositionNeeded = false)

        // insert the original text from `delegate`
        val originalNodeStart = delegate.tree.findNodeByCharIndex(range.start)
            ?: throw IndexOutOfBoundsException("Original node at position ${range.start} not found")
        var nodePositionStart = delegate.tree.findPositionStart(originalNodeStart)
        var insertPoint = range.start
        var node = originalNodeStart
        var insertOffsetStart = node.value.bufferOffsetStart + (range.start - nodePositionStart)
        do {
            val insertOffsetEndExclusive = if ((nodePositionStart + node.value.bufferLength) > (range.endInclusive + 1)) {
                node.value.bufferOffsetStart + (range.endInclusive + 1 - nodePositionStart)
            } else {
                node.value.bufferOffsetEndExclusive
            }
            insertOriginal(insertPoint, node.value, insertOffsetStart, insertOffsetEndExclusive)

            if (insertPoint + (insertOffsetEndExclusive - insertOffsetStart) > range.endInclusive) {
                break
            }

            node = delegate.tree.nextNode(node)!!
            nodePositionStart = delegate.tree.findPositionStart(node)
            insertPoint += insertOffsetEndExclusive - insertOffsetStart
            insertOffsetStart = node.value.bufferOffsetStart
        } while (nodePositionStart <= range.endInclusive)

        layout(maxOf(0, renderPositionAtOriginalStart - 1), minOf(length, renderPositionAtOriginalEnd + 1))
    }

//    override fun layoutTransaction(transaction: BigTextLayoutTransaction.() -> Unit) {
//        val transactionManager = BigTextLayoutTransaction(this)
//        with(transactionManager) {
//            start()
//            transaction()
//            close()
//        }
//    }

    override fun requestReapplyTransformation(originalRange: IntRange) {
        charRangesToReapplyTransforms += originalRange
    }

    override fun decorate(nodeValue: BigTextNodeValue, text: CharSequence, renderPositions: IntRange): CharSequence {
        val decorator = decorator ?: return text
        return if (nodeValue.bufferOwnership == BufferOwnership.Delegated) {
            val originalStart = findOriginalPositionByTransformedPosition(renderPositions.start)
            val originalEndInclusive = findOriginalPositionByTransformedPosition(renderPositions.endInclusive)
            decorator.onApplyDecorationOnOriginal(text, originalStart .. originalEndInclusive)
        } else {
            decorator.onApplyDecorationOnTransformation(text, renderPositions)
        }
    }
}

fun RedBlackTree<BigTextTransformNodeValue>.Node.transformedOffset(): Int =
    (getValue()?.leftTransformedLength ?: 0) +
            (getValue()?.currentTransformedLength ?: 0) +
            (getRight().takeIf { it.isNotNil() }?.transformedOffset() ?: 0)

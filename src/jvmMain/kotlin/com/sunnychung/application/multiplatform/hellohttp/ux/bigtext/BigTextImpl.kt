package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.addToThisAscendingListWithoutDuplicate
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

val log = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText")

val logQ = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText.Query")

val logL = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText.Layout")

val logV = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Debug
}, tag = "BigText.View")

internal var isD = false

private const val EPS = 1e-4f

class BigTextImpl : BigText {
    val tree = RedBlackTree2<BigTextNodeValue>(
        object : RedBlackTreeComputations<BigTextNodeValue> {
            override fun recomputeFromLeaf(it: RedBlackTree<BigTextNodeValue>.Node) = recomputeAggregatedValues(it)
            override fun computeWhenLeftRotate(x: BigTextNodeValue, y: BigTextNodeValue) = computeWhenLeftRotate0(x, y)
            override fun computeWhenRightRotate(x: BigTextNodeValue, y: BigTextNodeValue) = computeWhenRightRotate0(x, y)
//            override fun transferComputeResultTo(from: BigTextNodeValue, to: BigTextNodeValue) = transferComputeResultTo0(from, to)
        }
    )
    val buffers = mutableListOf<TextBuffer>()

    val chunkSize: Int // TODO change to a large number

    var layouter: TextLayouter? = null
        private set

    private var contentWidth: Float? = null

    var onLayoutCallback: (() -> Unit)? = null

    constructor() {
        chunkSize = 2 * 1024 * 1024 // 2 MB
    }

    constructor(chunkSize: Int) {
        require(chunkSize > 0) { "chunkSize must be positive" }
        this.chunkSize = chunkSize
    }

    fun RedBlackTree2<BigTextNodeValue>.findNodeByCharIndex(index: Int): RedBlackTree<BigTextNodeValue>.Node? {
        var find = index
        return findNode {
            when (find) {
                in Int.MIN_VALUE until it.value.leftStringLength -> -1
                in it.value.leftStringLength until it.value.leftStringLength + it.value.bufferLength -> 0
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

    fun RedBlackTree2<BigTextNodeValue>.findNodeByLineBreaks(index: Int): Pair<RedBlackTree<BigTextNodeValue>.Node, Int>? {
        var find = index
        var lineStart = 0
        return findNode {
            index
            when (find) {
                in Int.MIN_VALUE until it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
//                it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
                in it.value.leftNumOfLineBreaks until it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange -> 0
                in it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange  until Int.MAX_VALUE -> (if (it.right.isNotNil()) 1 else 0).also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange
                        lineStart += it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }?.let { it to lineStart + it.value.leftNumOfLineBreaks /*findLineStart(it)*/ }
    }

    fun RedBlackTree2<BigTextNodeValue>.findNodeByRowBreaks(index: Int): Pair<RedBlackTree<BigTextNodeValue>.Node, Int>? {
        var find = index
        var rowStart = 0
        return findNode {
            index
            when (find) {
                in Int.MIN_VALUE until it.value.leftNumOfRowBreaks -> if (it.left.isNotNil()) -1 else 0
                in it.value.leftNumOfRowBreaks until it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size -> 0
                in it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size  until Int.MAX_VALUE -> (if (it.right.isNotNil()) 1 else 0).also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size
                        rowStart += it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }?.let { it to rowStart + it.value.leftNumOfRowBreaks /*findLineStart(it)*/ }
    }

    fun findPositionByRowIndex(index: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        return tree.findNodeByRowBreaks(index - 1)!!.second
    }

    fun findRowIndexByPosition(position: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        this.length.let { length ->
            if (position == length) {
                return lastRowIndex
            } else if (position > length) {
                throw IndexOutOfBoundsException("position = $position but length = $length")
            }
        }
        val node = tree.findNodeByCharIndex(position)!!
        val startPos = findPositionStart(node)
        val nv = node.value
        val rowIndexInThisPartition = nv.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(position - startPos + nv.bufferOffsetStart) + 1
        val startRowIndex = findRowStart(node)
        return startRowIndex + rowIndexInThisPartition
    }

    fun findRowPositionStartIndexByRowIndex(index: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        require(index in (0 .. numOfRows)) { "Row index $index is out of bound. numOfRows = $numOfRows" }
        if (index == 0) {
            return 0;
        }

        val node = (tree.findNodeByRowBreaks(index - 1)
            ?: throw IllegalStateException("Cannot find the node right after ${index - 1} row breaks")
        ).first
        val rowStart = findRowStart(node)
        val startPos = findPositionStart(node)
        return startPos + if (index - 1 - rowStart == node.value.rowBreakOffsets.size && node.value.isEndWithForceRowBreak) {
            node.value.bufferLength
        } else if (index > 0) {
            node.value.rowBreakOffsets[index - 1 - rowStart] - node.value.bufferOffsetStart
        } else {
            0
        }
    }

    /**
     * @param rowIndex 0-based
     * @return 0-based
     */
    fun findLineIndexByRowIndex(rowIndex: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        require(rowIndex in (0 .. numOfRows)) { "Row index $rowIndex is out of bound. numOfRows = $numOfRows"}
        if (rowIndex == 0) {
            return 0
        }

        val (node, rowIndexStart) = tree.findNodeByRowBreaks(rowIndex - 1)!!
        val rowOffset = if (rowIndex - 1 - rowIndexStart == node.value.rowBreakOffsets.size && node.value.isEndWithForceRowBreak) {
            node.value.bufferOffsetEndExclusive
        } else if (rowIndex > 0) {
            node.value.rowBreakOffsets[rowIndex - 1 - rowIndexStart]
        } else {
            0
        }
        val positionStart = findPositionStart(node)
        val rowPositionStart = positionStart + rowOffset - node.value.bufferOffsetStart
        val lineBreakPosition = rowPositionStart - 1

        val lineBreakAtNode = tree.findNodeByCharIndex(lineBreakPosition)!!
        val lineStart = findLineStart(lineBreakAtNode)
        val positionStartOfLineBreakNode = findPositionStart(lineBreakAtNode)
        val lineBreakOffsetStarts = buffers[lineBreakAtNode.value.bufferIndex].lineOffsetStarts
        val lineBreakMinIndex = lineBreakOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineBreakAtNode.value.bufferOffsetStart)
        val lineBreakIndex = lineBreakOffsetStarts.binarySearchForMaxIndexOfValueAtMost(lineBreakPosition - positionStartOfLineBreakNode + lineBreakAtNode.value.bufferOffsetStart)
        return (lineStart + if (lineBreakIndex < lineBreakMinIndex) {
            0
        } else {
            /**
             * If lineBreakIndex >= lineBreakMinIndex, there are at least (lineBreakIndex - lineBreakMinIndex + 1) line breaks before this position.
             * If there is 1 line break before, then line index should be 1, etc..
             */
            lineBreakIndex - lineBreakMinIndex + 1
        }).also { log.d { "findLineIndexByRowIndex($rowIndex) = $it" } }
    }

    fun RedBlackTree<BigTextNodeValue>.Node.findRowBreakIndexOfLineOffset(lineOffset: Int): Int {
        return value.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(lineOffset + 1 /* rowBreak is 1 char after '\n' while lineBreak is right at '\n' */).let { // if lineOffset == 0, it should return -1
            if (it in value.rowBreakOffsets.indices) {
                return@let it
            }
            if (it == -1 && lineOffset == 0) {
                return@let it
            }
            if (lineOffset + 1 == value.bufferOffsetEndExclusive && value.isEndWithForceRowBreak) {
                return@let value.rowBreakOffsets.size
            }
            throw IndexOutOfBoundsException("Cannot find rowBreakOffset ${lineOffset + 1}")
        }
    }

    @Deprecated("not used. need to be fixed before use.")
    fun findFirstRowIndexOfLineOfRowIndex(rowIndex: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        val lineIndex = findLineIndexByRowIndex(rowIndex)
        val (lineStartNode, lineIndexStart) = tree.findNodeByLineBreaks(lineIndex - 1)!!
//        val positionOfLineStartNode = findPositionStart(lineStartNode)
        val lineOffsetStarts = buffers[lineStartNode.value.bufferIndex].lineOffsetStarts
        val inRangeLineStartIndex = lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineStartNode.value.bufferOffsetStart)
        val lineOffset = if (lineIndex - 1 >= 0) {
            lineOffsetStarts[inRangeLineStartIndex + lineIndex - 1 - lineIndexStart]
        } else {
            0
        }

        val rowBreakOffsetIndex = lineStartNode.findRowBreakIndexOfLineOffset(lineOffset)
        val rowBreaksStart = findRowStart(lineStartNode)
        return rowBreaksStart + rowBreakOffsetIndex + 1
    }

    /**
     * @param lineIndex 0-based line index
     * @return 0-based row index
     */
    fun findFirstRowIndexOfLine(lineIndex: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        require(lineIndex in (0 .. numOfLines)) { "Line index $lineIndex is out of bound. numOfLines = $numOfLines" }
        if (lineIndex == 0) {
            return 0;
        }

        val (lineStartNode, lineIndexStart) = tree.findNodeByLineBreaks(lineIndex - 1)
            ?: throw IllegalStateException("Cannot find the node right after ${lineIndex - 1} line breaks")
//        val positionOfLineStartNode = findPositionStart(lineStartNode)
        val lineOffsetStarts = buffers[lineStartNode.value.bufferIndex].lineOffsetStarts
        val inRangeLineStartIndex = lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineStartNode.value.bufferOffsetStart)
        val lineOffset = if (lineIndex - 1 >= 0) {
            lineOffsetStarts[inRangeLineStartIndex + lineIndex - 1 - lineIndexStart] - lineStartNode.value.bufferOffsetStart
        } else {
            0
        }
        val lineStartPos = findPositionStart(lineStartNode) + lineOffset

//        val rowBreakOffsetIndex = lineStartNode.findRowBreakIndexOfLineOffset(lineOffset)
//        val rowBreaksStart = findRowStart(lineStartNode)
//        return rowBreaksStart + rowBreakOffsetIndex + 1

        val rowStartPos = lineStartPos + 1 /* rowBreak is 1 char after '\n' while lineBreak is right at '\n' */
        val actualNode = tree.findNodeByCharIndex(rowStartPos) ?: run {
            if (rowStartPos == length && tree.rightmost(tree.getRoot()).value.isEndWithForceRowBreak) {
                return lastRowIndex
            }
            throw IndexOutOfBoundsException("pos $rowStartPos is out of bound. length = $length")
        }
        val actualNodeStartPos = findPositionStart(actualNode)
        val rowBreakOffsetIndex = actualNode.value.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(rowStartPos - actualNodeStartPos + actualNode.value.bufferOffsetStart)
        val rowBreaksStart = findRowStart(actualNode)
        return rowBreaksStart + rowBreakOffsetIndex + 1
    }

    protected fun findPositionStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
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

    protected fun findLineStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
        var start = node.value.leftNumOfLineBreaks
        var node = node
        while (node.parent.isNotNil()) {
            if (node === node.parent.right) {
                start += node.parent.value.leftNumOfLineBreaks + node.parent.value.bufferNumLineBreaksInRange
            }
            node = node.parent
        }
        return start
    }

    /**
     * Find the first 0-based row index of the node, in the global domain of this BigText.
     */
    protected fun findRowStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
        var start = node.value.leftNumOfRowBreaks
        var node = node
        while (node.parent.isNotNil()) {
            if (node === node.parent.right) {
                start += node.parent.value.leftNumOfRowBreaks + node.parent.value.rowBreakOffsets.size
            }
            node = node.parent
        }
        return start
    }

    private fun insertChunkAtPosition(position: Int, chunkedString: String) {
        log.d { "insertChunkAtPosition($position, $chunkedString)" }
        require(chunkedString.length <= chunkSize)
//        if (position == 64) {
//            log.d { inspect("$position") }
//        }
        var buffer = if (buffers.isNotEmpty()) {
            buffers.last().takeIf { it.length + chunkedString.length <= chunkSize }
        } else null
        if (buffer == null) {
            buffer = TextBuffer(chunkSize)
            buffers += buffer
        }
        require(buffer.length + chunkedString.length <= chunkSize)
        val range = buffer.append(chunkedString)
        var node = tree.findNodeByCharIndex(maxOf(0, position - 1)) // TODO optimize, don't do twice
        val nodeStart = node?.let { findPositionStart(it) } // TODO optimize, don't do twice
        if (node != null) {
            log.d { "> existing node (${node!!.value.debugKey()}) $nodeStart .. ${nodeStart!! + node!!.value.bufferLength - 1}" }
            require(maxOf(0, position - 1) in nodeStart!! .. nodeStart!! + node.value.bufferLength - 1) {
                printDebug()
                findPositionStart(node!!)
                "Found node ${node!!.value.debugKey()} but it is not in searching range"
            }
        }
        var insertDirection: InsertDirection = InsertDirection.Undefined
        val toBeRelayouted = mutableListOf<BigTextNodeValue>()
        val newNodeValues = if (node != null && position in nodeStart!! .. nodeStart!! + node.value.bufferLength - 1) {
            val splitAtIndex = position - nodeStart
            log.d { "> split at $splitAtIndex" }
            val oldEnd = node.value.bufferOffsetEndExclusive
            val secondPartNodeValue = BigTextNodeValue().apply { // the second part of the old string
                bufferIndex = node!!.value.bufferIndex
                bufferOffsetStart = node!!.value.bufferOffsetStart + splitAtIndex
                bufferOffsetEndExclusive = oldEnd

                leftStringLength = 0
            }
            if (splitAtIndex > 0) {
                node.value.bufferOffsetEndExclusive = node.value.bufferOffsetStart + splitAtIndex
            } else {
                tree.delete(node)
                node = tree.findNodeByCharIndex(maxOf(0, position - 1))
                insertDirection = InsertDirection.Left
            }
            require(splitAtIndex + chunkedString.length <= chunkSize)
            toBeRelayouted += secondPartNodeValue
            listOf(
                BigTextNodeValue().apply { // new string
                    bufferIndex = buffers.lastIndex
                    bufferOffsetStart = range.start
                    bufferOffsetEndExclusive = range.endInclusive + 1

                    leftStringLength = 0
                },
                secondPartNodeValue
            ).reversed() // IMPORTANT: the insertion order is reversed
        } else if (node == null || node.value.bufferIndex != buffers.lastIndex || node.value.bufferOffsetEndExclusive != range.start) {
            log.d { "> create new node" }
            listOf(BigTextNodeValue().apply {
                bufferIndex = buffers.lastIndex
                bufferOffsetStart = range.start
                bufferOffsetEndExclusive = range.endInclusive + 1

                leftStringLength = 0
            })
        } else {
            node.value.apply {
                log.d { "> update existing node end from $bufferOffsetEndExclusive to ${bufferOffsetEndExclusive + range.length}" }
                bufferOffsetEndExclusive += range.length
            }
            recomputeAggregatedValues(node)
            emptyList()
        }
        if (newNodeValues.isNotEmpty() && insertDirection == InsertDirection.Left) {
            node = if (node != null) {
                tree.insertLeft(node, newNodeValues.first())
            } else {
                tree.insertValue(newNodeValues.first())!!
            }
            (1 .. newNodeValues.lastIndex).forEach {
                node = tree.insertLeft(node!!, newNodeValues[it])
            }
        } else {
            newNodeValues.forEach {
                if (node?.value?.leftStringLength == position) {
                    tree.insertLeft(node!!, it) // insert before existing node
                } else if (node != null) {
                    tree.insertRight(node!!, it)
                } else {
                    tree.insertValue(it)
                }
            }
        }

        toBeRelayouted.forEach {
            val startPos = findPositionStart(it.node!!)
            val endPos = startPos + it.bufferLength
            layout(startPos, endPos)
        }

        log.v { inspect("Finish I " + node?.value?.debugKey()) }
    }

    fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue) = with (nodeValue) {
//        bufferNumLineBreaksInRange = buffers[bufferIndex].lineOffsetStarts.subSet(bufferOffsetStart, bufferOffsetEndExclusive).size
        bufferNumLineBreaksInRange = buffers[bufferIndex].lineOffsetStarts.run {
            binarySearchForMinIndexOfValueAtLeast(bufferOffsetEndExclusive) - maxOf(0, binarySearchForMinIndexOfValueAtLeast(bufferOffsetStart))
        }
        leftNumOfLineBreaks = node?.left?.numLineBreaks() ?: 0
        log.v { ">> leftNumOfLineBreaks ${node?.value?.debugKey()} -> $leftNumOfLineBreaks" }

        leftNumOfRowBreaks = node?.left?.numRowBreaks() ?: 0
    }

    fun recomputeAggregatedValues(node: RedBlackTree<BigTextNodeValue>.Node) {
        log.v { inspect("${node.value?.debugKey()} start") }

        var node = node
        while (node.isNotNil()) {
            val left = node.left.takeIf { it.isNotNil() }
            with (node.getValue()) {
                // recompute leftStringLength
                leftStringLength = left?.length() ?: 0
                log.v { ">> ${node.value.debugKey()} -> $leftStringLength (${left?.value?.debugKey()}/ ${left?.length()})" }

                // recompute leftNumOfLineBreaks
                computeCurrentNodeProperties(this)

                // TODO calc other metrics
            }
            log.v { ">> ${node.parent.value?.debugKey()} parent -> ${node.value?.debugKey()}" }
            node = node.parent
        }
        log.v { inspect("${node.value?.debugKey()} end") }
        log.v { "" }
    }

    fun computeWhenLeftRotate0(x: BigTextNodeValue, y: BigTextNodeValue) {
        y.leftStringLength += x.leftStringLength + x.bufferLength
        // TODO calc other metrics
    }

    fun computeWhenRightRotate0(x: BigTextNodeValue, y: BigTextNodeValue) {
        y.leftStringLength -= x.leftStringLength + x.bufferLength
        // TODO calc other metrics
    }

//    fun transferComputeResultTo0(from: BigTextNodeValue, to: BigTextNodeValue) {
//        to.leftStringLength = from.leftStringLength
//        // TODO calc other metrics
//    }

    override val length: Int
        get() = tree.getRoot().length()

    val lastIndex: Int
        get() = length - 1

    override fun buildString(): String {
        return tree.joinToString("") {
            buffers[it.bufferIndex].subSequence(it.bufferOffsetStart, it.bufferOffsetEndExclusive)
        }
    }

    override fun substring(start: Int, endExclusive: Int): String { // O(lg L + (e - s))
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive $endExclusive is out of bound. length = $length" }

        if (start == endExclusive) {
            return ""
        }

        val result = StringBuilder(endExclusive - start)
        var node = tree.findNodeByCharIndex(start) ?: throw IllegalStateException("Cannot find string node for position $start")
        var nodeStartPos = findPositionStart(node)
        var numRemainCharsToCopy = endExclusive - start
        var copyFromBufferIndex = start - nodeStartPos + node.value.bufferOffsetStart
        while (numRemainCharsToCopy > 0) {
            val numCharsToCopy = minOf(endExclusive, nodeStartPos + node.value.bufferLength) - maxOf(start, nodeStartPos)
            val copyUntilBufferIndex = copyFromBufferIndex + numCharsToCopy
            if (numCharsToCopy > 0) {
                val subsequence = buffers[node.value.bufferIndex].subSequence(copyFromBufferIndex, copyUntilBufferIndex)
                result.append(subsequence)
                numRemainCharsToCopy -= numCharsToCopy
            } else {
                break
            }
            if (numRemainCharsToCopy > 0) {
                nodeStartPos += node.value.bufferLength
                node = tree.nextNode(node) ?: throw IllegalStateException("Cannot find the next string node")
                copyFromBufferIndex = node.value.bufferOffsetStart
            }
        }

        return result.toString()
    }

    fun findLineString(lineIndex: Int): String {
        require(0 <= lineIndex) { "lineIndex $lineIndex must be non-negative." }
        require(lineIndex <= numOfLines) { "lineIndex $lineIndex out of bound, numOfLines = $numOfLines." }

        /**
         * @param lineOffset 0 = start of buffer; 1 = char index after the 1st '\n'; 2 = char index after the 2nd '\n'; ...
         */
        fun findCharPosOfLineOffset(node: RedBlackTree<BigTextNodeValue>.Node, lineOffset: Int): Int {
            val buffer = buffers[node.value!!.bufferIndex]
            val lineStartIndexInBuffer = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(node.value!!.bufferOffsetStart)
            val lineEndIndexInBuffer = buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(node.value!!.bufferOffsetEndExclusive - 1)
            val offsetedLineOffset = maxOf(0, lineStartIndexInBuffer) + (lineOffset) - 1
            val charOffsetInBuffer = if (offsetedLineOffset > lineEndIndexInBuffer) {
                node.value!!.bufferOffsetEndExclusive
            } else if (lineOffset - 1 >= 0) {
                buffer.lineOffsetStarts[offsetedLineOffset] + 1
            } else {
                node.value!!.bufferOffsetStart
            }
            return findPositionStart(node) + (charOffsetInBuffer - node.value!!.bufferOffsetStart)
        }

        val (startNode, startNodeLineStart) = tree.findNodeByLineBreaks(lineIndex - 1)!!
        val endNodeFindPair = tree.findNodeByLineBreaks(lineIndex)
        val endCharIndex = if (endNodeFindPair != null) { // includes the last '\n' char
            val (endNode, endNodeLineStart) = endNodeFindPair
            require(endNodeLineStart <= lineIndex) { "Node ${endNode.value.debugKey()} violates [endNodeLineStart <= lineIndex]" }
//            val lca = tree.lowestCommonAncestor(startNode, endNode)
            findCharPosOfLineOffset(endNode, lineIndex + 1 - endNodeLineStart)
        } else {
            length
        }
        val startCharIndex = findCharPosOfLineOffset(startNode, lineIndex - startNodeLineStart)
        logQ.d { "line #$lineIndex -> $startCharIndex ..< $endCharIndex" }
        return substring(startCharIndex, endCharIndex) // includes the last '\n' char
    }

    fun findRowString(rowIndex: Int): String {
        /**
         * @param rowOffset 0 = start of buffer; 1 = char index of the first row break
         */
        fun findCharPosOfRowOffset(node: RedBlackTree<BigTextNodeValue>.Node, rowOffset: Int): Int {
            val charOffsetInBuffer = if (rowOffset - 1 > node.value!!.rowBreakOffsets.size - 1) {
                node.value!!.bufferOffsetEndExclusive
            } else if (rowOffset - 1 >= 0) {
                val offsetedRowOffset = rowOffset - 1
                node.value!!.rowBreakOffsets[offsetedRowOffset]
            } else {
                node.value!!.bufferOffsetStart
            }
            return findPositionStart(node) + (charOffsetInBuffer - node.value!!.bufferOffsetStart)
        }

        val (startNode, startNodeRowStart) = tree.findNodeByRowBreaks(rowIndex - 1) ?:
            if (rowIndex <= numOfRows) {
                return ""
            } else {
                throw IndexOutOfBoundsException("numOfRows = $numOfRows; but given index = $rowIndex")
            }
        val endNodeFindPair = tree.findNodeByRowBreaks(rowIndex)
        val endCharIndex = if (endNodeFindPair != null) { // includes the last '\n' char
            val (endNode, endNodeRowStart) = endNodeFindPair
            require(endNodeRowStart <= rowIndex) { "Node ${endNode.value.debugKey()} violates [endNodeRowStart <= rowIndex]" }
//            val lca = tree.lowestCommonAncestor(startNode, endNode)
            findCharPosOfRowOffset(endNode, rowIndex + 1 - endNodeRowStart)
        } else {
            length
        }
        val startCharIndex = findCharPosOfRowOffset(startNode, rowIndex - startNodeRowStart)
        logQ.d { "row #$rowIndex -> $startCharIndex ..< $endCharIndex" }
        return substring(startCharIndex, endCharIndex) // includes the last '\n' char
    }

    override fun append(text: String): Int {
        return insertAt(length, text)
//        var start = 0
//        while (start < text.length) {
//            var last = buffers.lastOrNull()?.length
//            if (last == null || last >= chunkSize) {
//                buffers += TextBuffer()
//                last = 0
//            }
//            val available = chunkSize - last
//            val append = minOf(available, text.length - start)
//            appendChunk(text.substring(start until start + append))
//            start += append
//        }
    }

    override fun insertAt(pos: Int, text: String): Int {
        var start = 0
        val prevNode = tree.findNodeByCharIndex(maxOf(0, pos - 1))
        val nodeStart = prevNode?.let { findPositionStart(it) }?.also {
            require(pos in it .. it + prevNode.value.bufferLength)
        }
        var last = buffers.lastOrNull()?.length // prevNode?.let { buffers[it.value.bufferIndex].length }
        if (prevNode != null && pos in nodeStart!! .. nodeStart!! + prevNode.value.bufferLength - 1) {
            val splitAtIndex = pos - nodeStart
            last = maxOf((last ?: 0) % chunkSize, splitAtIndex)
        }
        while (start < text.length) {
            if (last == null || last >= chunkSize) {
//                buffers += TextBuffer()
                last = 0
            }
            val available = chunkSize - last
            val append = minOf(available, text.length - start)
            insertChunkAtPosition(pos + start, text.substring(start until start + append))
            start += append
            last = buffers.last().length
        }
        layout(maxOf(0, pos - 1), minOf(length, pos + text.length + 1))
        return text.length
    }

    override fun delete(start: Int, endExclusive: Int): Int {
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive is out of bound" }

        if (start == endExclusive) {
            return 0
        }

        log.d { "delete $start ..< $endExclusive" }

        var node: RedBlackTree<BigTextNodeValue>.Node? = tree.findNodeByCharIndex(endExclusive - 1)
        var nodeRange = charIndexRangeOfNode(node!!)
        val newNodesInDescendingOrder = mutableListOf<BigTextNodeValue>()
        while (node?.isNotNil() == true && start <= nodeRange.endInclusive) {
            if (isD && nodeRange.start == 0) {
                isD = true
            }
            if (endExclusive - 1 in nodeRange.start..nodeRange.last - 1) {
                // need to split
                val splitAtIndex = endExclusive - nodeRange.start
                log.d { "Split E at $splitAtIndex" }
                newNodesInDescendingOrder += BigTextNodeValue().apply { // the second part of the existing string
                    bufferIndex = node!!.value.bufferIndex
                    bufferOffsetStart = node!!.value.bufferOffsetStart + splitAtIndex
                    bufferOffsetEndExclusive = node!!.value.bufferOffsetEndExclusive

                    leftStringLength = 0
                }
            }
            if (start in nodeRange.start + 1 .. nodeRange.last) {
                // need to split
                val splitAtIndex = start - nodeRange.start
                log.d { "Split S at $splitAtIndex" }
                newNodesInDescendingOrder += BigTextNodeValue().apply { // the first part of the existing string
                    bufferIndex = node!!.value.bufferIndex
                    bufferOffsetStart = node!!.value.bufferOffsetStart
                    bufferOffsetEndExclusive = node!!.value.bufferOffsetStart + splitAtIndex

                    leftStringLength = 0
                }
            }
            val prev = tree.prevNode(node)
            log.d { "Delete node ${node!!.value.debugKey()} at ${nodeRange.start} .. ${nodeRange.last}" }
            if (isD && nodeRange.start == 384) {
                isD = true
            }
            tree.delete(node)
            log.d { inspect("After delete " + node?.value?.debugKey()) }
            node = prev
//            nodeRange = nodeRange.start - chunkSize .. nodeRange.last - chunkSize
            if (node != null) {
                nodeRange = charIndexRangeOfNode(node) // TODO optimize by calculation instead of querying
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

//        // FIXME remove
//        tree.visitInPostOrder {
//            computeCurrentNodeProperties(it.value)
//        }

        newNodesInDescendingOrder.forEach {
            val startPos = findPositionStart(it.node!!)
            val endPos = startPos + it.bufferLength
            layout(startPos, endPos)
        }

        layout(maxOf(0, start - 1), minOf(length, start + 1))

        log.d { inspect("Finish D " + node?.value?.debugKey()) }

        return -(endExclusive - start)
    }

    fun charIndexRangeOfNode(node: RedBlackTree<BigTextNodeValue>.Node): IntRange {
        val start = findPositionStart(node)
        return start until start + node.value.bufferLength
    }

    override fun hashCode(): Int {
//        TODO("Not yet implemented")
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
//        TODO("Not yet implemented")
        return super.equals(other)
    }

    fun inspect(label: String = "") = buildString {
        appendLine("[$label] Buffer:\n${buffers.mapIndexed { i, it -> "    $i:\t$it\n" }.joinToString("")}")
        appendLine("[$label] Buffer Line Breaks:\n${buffers.mapIndexed { i, it -> "    $i:\t${it.lineOffsetStarts}\n" }.joinToString("")}")
        appendLine("[$label] Tree:\nflowchart TD\n${tree.debugTree()}")
        appendLine("[$label] String:\n${fullString()}")
        if (layouter != null && contentWidth != null) {
            appendLine("[$label] Layouted String:\n${(0 until numOfRows).joinToString("") {
                try {
                    "{${findRowString(it)}}\n"
                } catch (e: Throwable) {
                    "[$e]!\n"
                }
            }}")
        }
    }

    fun printDebug(label: String = "") {
        println(inspect(label))
    }

    fun setLayouter(layouter: TextLayouter) {
        if (this.layouter == layouter) {
            return
        }

        tree.forEach {
            val buffer = buffers[it.bufferIndex]
            val chunkString = buffer.subSequence(it.bufferOffsetStart, it.bufferOffsetEndExclusive)
            layouter.indexCharWidth(chunkString.toString())
        }

        this.layouter = layouter

        layout()
    }

    fun setContentWidth(contentWidth: Float) {
        require(contentWidth > EPS) { "contentWidth must be positive" }

        if (this.contentWidth == contentWidth) {
            return
        }

        this.contentWidth = contentWidth

        layout()
    }

    fun layout() {
        val layouter = this.layouter ?: return
        val contentWidth = this.contentWidth ?: return

        return layout(0, length)
        // the code below doesn't pass insertTriggersRelayout3(16)

        var lastOccupiedWidth = 0f
        val treeLastIndex = tree.size() - 1
        tree.forEachIndexed { index, node ->
            val buffer = buffers[node.bufferIndex]
            val lineBreakIndexFrom = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(node.bufferOffsetStart)
            val lineBreakIndexTo = buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(node.bufferOffsetEndExclusive - 1)
            var charStartIndexInBuffer = node.bufferOffsetStart
//            node.rowBreakOffsets.clear()
            val rowBreakOffsets = mutableListOf<Int>()
            (lineBreakIndexFrom .. lineBreakIndexTo).forEach { lineBreakEntryIndex ->
                val lineBreakCharIndex = buffer.lineOffsetStarts[lineBreakEntryIndex]
                val subsequence = buffer.subSequence(charStartIndexInBuffer, lineBreakCharIndex)
                logL.d { "node ${node.debugKey()} line break #$lineBreakEntryIndex seq $charStartIndexInBuffer ..< $lineBreakCharIndex" }

                val (rowCharOffsets, _) = layouter.layoutOneLine(subsequence, contentWidth, lastOccupiedWidth, charStartIndexInBuffer)
//                node.rowBreakOffsets += rowCharOffsets
                logL.d { "row break add $rowCharOffsets" }
                rowBreakOffsets += rowCharOffsets
                logL.d { "row break add ${lineBreakCharIndex + 1}" }
                rowBreakOffsets += lineBreakCharIndex + 1

                charStartIndexInBuffer = lineBreakCharIndex + 1
                lastOccupiedWidth = 0f
            }
            if (charStartIndexInBuffer < node.bufferOffsetEndExclusive) {
                val subsequence = buffer.subSequence(charStartIndexInBuffer, node.bufferOffsetEndExclusive)
                logL.d { "node ${node.debugKey()} last row seq $charStartIndexInBuffer ..< ${node.bufferOffsetEndExclusive}" }

                val (rowCharOffsets, lastRowOccupiedWidth) = layouter.layoutOneLine(subsequence, contentWidth, lastOccupiedWidth, charStartIndexInBuffer)
//                node.rowBreakOffsets += rowCharOffsets
                logL.d { "row break add $rowCharOffsets" }
                rowBreakOffsets += rowCharOffsets
                lastOccupiedWidth = lastRowOccupiedWidth
            }
            node.rowBreakOffsets = rowBreakOffsets
            node.lastRowWidth = lastOccupiedWidth
        }

    }

    protected fun layout(startPos: Int, endPosExclusive: Int) {
        val layouter = this.layouter ?: return
        val contentWidth = this.contentWidth ?: return

        var lastOccupiedWidth = 0f
        var node: RedBlackTree<BigTextNodeValue>.Node? = tree.findNodeByCharIndex(startPos) ?: return
        logL.d { "layout($startPos, $endPosExclusive)" }
        logL.v { inspect("before layout($startPos, $endPosExclusive)") }
        var nodeStartPos = findPositionStart(node!!)
        val nodeValue = node.value
        val buffer = buffers[nodeValue.bufferIndex]
        var lineBreakIndexFrom = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(
            (startPos - nodeStartPos) + nodeValue.bufferOffsetStart
        )
        var charStartIndexInBuffer = nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost((startPos - nodeStartPos) + nodeValue.bufferOffsetStart).let {
            if (it >= 0) {
                nodeValue.rowBreakOffsets[it]
            } else {
                val prevNode = tree.prevNode(node!!)
                if (prevNode != null) {
                    lastOccupiedWidth = prevNode.value.lastRowWidth // carry over
                    logL.d { "carry over width $lastOccupiedWidth" }
                }

                nodeValue.bufferOffsetStart
            }
        }
        logL.d { "charStartIndexInBuffer = $charStartIndexInBuffer" }

        // we are starting at charStartIndexInBuffer without carrying over last width, so include the row break at charStartIndexInBuffer
        val restoreRowBreakOffsets = if (startPos > 0) {
            nodeValue.rowBreakOffsets.subList(0, maxOf(0, nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(charStartIndexInBuffer) + 1))
        } else {
            emptyList()
        }
        logL.d { "restore row breaks of starting node $restoreRowBreakOffsets" }
        var hasRestoredRowBreaks = false

        var isBreakOnEncounterLineBreak = false

        // TODO refactor
        while (node != null) {
            var isBreakAfterThisIteration = false
            val nodeValue = node.value
            val buffer = buffers[nodeValue.bufferIndex]
            val lineBreakIndexTo =
                buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(nodeValue.bufferOffsetEndExclusive - 1)
                    .let {
                        if (endPosExclusive in nodeStartPos..nodeStartPos + nodeValue.bufferLength) {
                            minOf(
                                it,
                                buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(endPosExclusive - nodeStartPos + nodeValue.bufferOffsetStart)
                            )
                        } else {
                            it
                        }
                    }
            logL.d { "node ${nodeValue.debugKey()} LB $lineBreakIndexFrom .. $lineBreakIndexTo P $nodeStartPos" }
            logL.d { "buffer ${nodeValue.bufferIndex} LB ${buffer.lineOffsetStarts}" }

//            if (lineBreakIndexFrom > lineBreakIndexTo) {
            if (nodeStartPos > endPosExclusive + 1) { // do 1 more char because last round may just fill up the row but a row break is not created
                logL.d { "set BreakOnEncounterLineBreak" }
                isBreakOnEncounterLineBreak = true
            }

//            nodeValue.rowBreakOffsets.clear()
            val rowBreakOffsets = mutableListOf<Int>()
            var isEndWithForceRowBreak = false
            logL.d { "orig row breaks ${nodeValue.rowBreakOffsets}" }
//            if (true || nodeStartPos == 0) {
//                // we are starting at charStartIndexInBuffer without carrying over last width, so include the row break at charStartIndexInBuffer
//                rowBreakOffsets += nodeValue.rowBreakOffsets.subList(0, maxOf(0, nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(charStartIndexInBuffer) + 1))
//                logL.d { "restore row breaks of leftmost node $rowBreakOffsets" }
//            } else {
//                rowBreakOffsets += nodeValue.rowBreakOffsets.subList(
//                    0,
//                    maxOf(0, nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(charStartIndexInBuffer - 1) + 1)
//                )
//                logL.d { "restore row breaks $rowBreakOffsets" }
//            }
            if (!hasRestoredRowBreaks) {
                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(restoreRowBreakOffsets)
                hasRestoredRowBreaks = true
            }
            (lineBreakIndexFrom..lineBreakIndexTo).forEach { lineBreakEntryIndex ->
                val lineBreakCharIndex = buffer.lineOffsetStarts[lineBreakEntryIndex]
                val subsequence = buffer.subSequence(charStartIndexInBuffer, lineBreakCharIndex)
                logL.d { "node ${nodeValue.debugKey()} buf #${nodeValue.bufferIndex} line break #$lineBreakEntryIndex seq $charStartIndexInBuffer ..< $lineBreakCharIndex" }

                val (rowCharOffsets, _) = layouter.layoutOneLine(
                    subsequence,
                    contentWidth,
                    lastOccupiedWidth,
                    charStartIndexInBuffer
                )
//                nodeValue.rowBreakOffsets += rowCharOffsets
                logL.d { "row break add $rowCharOffsets lw = 0" }
                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(rowCharOffsets)

                if (subsequence.isEmpty() && lastOccupiedWidth >= contentWidth - EPS) {
                    logL.d { "row break add carry-over force break ${lineBreakCharIndex}" }
                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(lineBreakCharIndex)
                }

                charStartIndexInBuffer = lineBreakCharIndex + 1
                if (lineBreakCharIndex + 1 < nodeValue.bufferOffsetEndExclusive) {
                    logL.d { "row break add ${lineBreakCharIndex + 1}" }
                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(lineBreakCharIndex + 1)
                    lastOccupiedWidth = 0f
                } else {
                    lastOccupiedWidth = contentWidth + 0.1f // force a row break at the start of next layout
                    isEndWithForceRowBreak = true
                }

                if (isBreakOnEncounterLineBreak) {
                    isBreakAfterThisIteration = true
                }
            }
            val nextBoundary = if (
                lineBreakIndexTo + 1 <= buffer.lineOffsetStarts.lastIndex
//                    && buffer.lineOffsetStarts[lineBreakIndexTo + 1] - node.value.bufferOffsetStart + nodeStartPos < endPosExclusive
                    && buffer.lineOffsetStarts[lineBreakIndexTo + 1] < nodeValue.bufferOffsetEndExclusive
            ) {
                buffer.lineOffsetStarts[lineBreakIndexTo + 1]
            } else {
                nodeValue.bufferOffsetEndExclusive
            }
//            if (charStartIndexInBuffer < nodeValue.bufferOffsetEndExclusive) {
//            if (charStartIndexInBuffer < nodeValue.bufferOffsetEndExclusive && nodeStartPos + charStartIndexInBuffer - nodeValue.bufferOffsetStart < endPosExclusive) {
            if (charStartIndexInBuffer < nextBoundary) {
//                val subsequence = buffer.subSequence(charStartIndexInBuffer, nodeValue.bufferOffsetEndExclusive)
                val readRowUntilPos = nextBoundary //nodeValue.bufferOffsetEndExclusive //minOf(nodeValue.bufferOffsetEndExclusive, endPosExclusive - nodeStartPos + nodeValue.bufferOffsetStart)
                logL.d { "node ${nodeValue.debugKey()} last row seq $charStartIndexInBuffer ..< ${readRowUntilPos}. start = $nodeStartPos" }
                val subsequence = buffer.subSequence(charStartIndexInBuffer, readRowUntilPos)

                val (rowCharOffsets, lastRowOccupiedWidth) = layouter.layoutOneLine(
                    subsequence,
                    contentWidth,
                    lastOccupiedWidth,
                    charStartIndexInBuffer
                )
//                nodeValue.rowBreakOffsets += rowCharOffsets
                logL.d { "row break add $rowCharOffsets lw = $lastRowOccupiedWidth" }
                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(rowCharOffsets)
                lastOccupiedWidth = lastRowOccupiedWidth
                charStartIndexInBuffer = readRowUntilPos
            }
            if (charStartIndexInBuffer < nodeValue.bufferOffsetEndExclusive) {
//                val preserveIndexFrom = nodeValue.rowBreakOffsets.binarySearchForMinIndexOfValueAtLeast(endPosExclusive)
                val searchForValue = minOf(nodeValue.bufferOffsetEndExclusive, maxOf((rowBreakOffsets.lastOrNull() ?: -1) + 1, charStartIndexInBuffer))
                val preserveIndexFrom = nodeValue.rowBreakOffsets.binarySearchForMinIndexOfValueAtLeast(searchForValue)
                val preserveIndexTo = if (nodeStartPos + nodeValue.bufferLength >= length) {
                    nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(nodeValue.bufferOffsetEndExclusive) // keep the row after the last '\n'
                } else {
                    nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(nodeValue.bufferOffsetEndExclusive - 1)
                }
                logL.d { "reach the end, preserve RB from $preserveIndexFrom (at least $searchForValue) ~ $preserveIndexTo (${nodeValue.bufferOffsetEndExclusive}). RB = ${nodeValue.rowBreakOffsets}." }
                val restoreRowBreaks = nodeValue.rowBreakOffsets.subList(preserveIndexFrom, minOf(nodeValue.rowBreakOffsets.size, preserveIndexTo + 1))
                if (restoreRowBreaks.isNotEmpty() || nodeValue.isEndWithForceRowBreak) {
                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(restoreRowBreaks)
                    logL.d { "Restore lw ${nodeValue.lastRowWidth}." }
                    lastOccupiedWidth = nodeValue.lastRowWidth
                    isEndWithForceRowBreak = isEndWithForceRowBreak || nodeValue.isEndWithForceRowBreak
                }
            }
            nodeValue.rowBreakOffsets = rowBreakOffsets
            nodeValue.lastRowWidth = lastOccupiedWidth
            nodeValue.isEndWithForceRowBreak = isEndWithForceRowBreak
            recomputeAggregatedValues(node) // TODO optimize

            if (isBreakOnEncounterLineBreak && isBreakAfterThisIteration) { // TODO it can be further optimized to break immediately on line break
                logL.d { "break" }
                break
            }

            node = tree.nextNode(node)?.also {
                logL.d { "node ${node!!.value.debugKey()} b#${node!!.value.bufferIndex} next ${it.value.debugKey()} b#${it!!.value.bufferIndex}" }
            }
            if (node != null) {
                nodeStartPos += nodeValue.bufferLength
                val nodeValue = node.value
                val buffer = buffers[nodeValue.bufferIndex]
                lineBreakIndexFrom = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(nodeValue.bufferOffsetStart)
                charStartIndexInBuffer = nodeValue.bufferOffsetStart
            }
        }

//        tree.visitInPostOrder {
//            recomputeAggregatedValues(it)
//        }

        onLayoutCallback?.invoke()
    }

    val hasLayouted: Boolean
        get() = layouter != null && contentWidth != null

    val numOfRows: Int
        get() = tree.getRoot().numRowBreaks() + 1 + // TODO cache the result
            run {
                val lastNode = tree.rightmost(tree.getRoot()).takeIf { it.isNotNil() }
                val lastValue = lastNode?.value ?: return@run 0
                val lastLineOffset = buffers[lastValue.bufferIndex].lineOffsetStarts.let {
                    val lastIndex = it.binarySearchForMaxIndexOfValueAtMost(lastValue.bufferOffsetEndExclusive - 1)
                    if (lastIndex in 0 .. it.lastIndex) {
                        it[lastIndex]
                    } else {
                        null
                    }
                } ?: return@run 0
                val lastNodePos = findPositionStart(lastNode)
                if (lastNodePos + (lastLineOffset - lastValue.bufferOffsetStart) == lastIndex) {
                    1 // one extra row if the string ends with '\n'
                } else {
                    0
                }
            }

    val numOfLines: Int
        get() = tree.getRoot().numLineBreaks() + 1

    val lastRowIndex: Int
        get() = numOfRows - 1

    /**
     * This is an expensive operation that defeats the purpose of BigText.
     * TODO: take out all the usage of this function
     */
//    fun produceLayoutResult(): BigTextLayoutResult {
//        val lineFirstRowIndices = ArrayList<Int>(numOfLines)
//        val rowStartCharIndices = ArrayList<Int>(numOfRows)
//
//        tree.forEach { nv ->
//            val pos = findPositionStart(nv.node!!)
//            rowStartCharIndices += nv.rowBreakOffsets.map { it - nv.bufferOffsetStart + pos }
//        }
//
//        tree.forEach { nv ->
//            if (nv.bufferNumLineBreaksInRange < 1) {
//                return@forEach
//            }
//            val pos = findPositionStart(nv.node!!)
//            val buffer = buffers[nv.bufferIndex]
//            val lb = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(nv.bufferOffsetStart)
//            val ub = buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(nv.bufferOffsetEndExclusive - 1)
//
//            rows += nv.rowBreakOffsets.map { it - nv.bufferOffsetStart + pos }
//        }
//
//        return BigTextLayoutResult(
//            lineRowSpans = emptyList(), // not used
//            lineFirstRowIndices =
//        )
//    }

}

fun RedBlackTree<BigTextNodeValue>.Node.length(): Int =
    (getValue()?.leftStringLength ?: 0) +
        (getValue()?.bufferLength ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.length() ?: 0)

fun RedBlackTree<BigTextNodeValue>.Node.numLineBreaks(): Int {
    val value = getValue()
    return (value?.leftNumOfLineBreaks ?: 0) +
        (value?.bufferNumLineBreaksInRange ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.numLineBreaks() ?: 0)
}

fun RedBlackTree<BigTextNodeValue>.Node.numRowBreaks(): Int {
    val value = getValue()
    return (value?.leftNumOfRowBreaks ?: 0) +
        (value?.rowBreakOffsets?.size ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.numRowBreaks() ?: 0)
}

private enum class InsertDirection {
    Left, Right, Undefined
}

fun BigText.Companion.createFromLargeString(initialContent: String) = BigTextImpl().apply {
    append(initialContent)
}

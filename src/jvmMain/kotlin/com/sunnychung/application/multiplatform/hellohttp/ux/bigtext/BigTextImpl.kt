package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.addToThisAscendingListWithoutDuplicate
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMaxIndexOfValueAtMost
import com.sunnychung.application.multiplatform.hellohttp.extension.binarySearchForMinIndexOfValueAtLeast
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.CircularList
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.sunnychung.application.multiplatform.hellohttp.util.let
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

val log = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "BigText")

val logQ = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "BigText.Query")

val logL = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "BigText.Layout")

val logV = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Warn
}, tag = "BigText.View")

internal var isD = false

private const val EPS = 1e-4f

open class BigTextImpl(
    override val chunkSize: Int = 2 * 1024 * 1024, // 2 MB
    override val undoHistoryCapacity: Int = 1000,
    override val textBufferFactory: ((capacity: Int) -> TextBuffer) = { StringTextBuffer(it) },
    override val charSequenceBuilderFactory: ((capacity: Int) -> Appendable) = { StringBuilder(it) },
    override val charSequenceFactory: ((Appendable) -> CharSequence) = { it: Appendable -> it.toString() },
) : BigText, BigTextLayoutable {
    override val tree: LengthTree<BigTextNodeValue> = LengthTree<BigTextNodeValue>(
        object : RedBlackTreeComputations<BigTextNodeValue> {
            override fun recomputeFromLeaf(it: RedBlackTree<BigTextNodeValue>.Node) = recomputeAggregatedValues(it)
            override fun computeWhenLeftRotate(x: BigTextNodeValue, y: BigTextNodeValue) = computeWhenLeftRotate0(x, y)
            override fun computeWhenRightRotate(x: BigTextNodeValue, y: BigTextNodeValue) = computeWhenRightRotate0(x, y)
//            override fun transferComputeResultTo(from: BigTextNodeValue, to: BigTextNodeValue) = transferComputeResultTo0(from, to)
        }
    )
    val buffers = mutableListOf<TextBuffer>()

    final override var layouter: TextLayouter? = null
        @JvmName("_setLayouter")
        protected set

    internal var isLayoutEnabled: Boolean = true

    override var contentWidth: Float? = null

    override var onLayoutCallback: (() -> Unit)? = null

    /**
     * Note: It is required to call {@link recordCurrentChangeSequenceIntoUndoHistory()} manually to make undo works!
     */
    var isUndoEnabled: Boolean = false

    override var decorator: BigTextDecorator? = null

    override var undoMetadataSupplier: (() -> Any?)? = null

    private var currentUndoMetadata: Any? = null
    private var currentRedoMetadata: Any? = null
    var currentChanges: MutableList<BigTextInputChange> = mutableListOf()
        private set
    val undoHistory = CircularList<BigTextInputOperation>(undoHistoryCapacity)
    val redoHistory = CircularList<BigTextInputOperation>(undoHistoryCapacity)

    override var changeHook: BigTextChangeHook? = null

    init {
        require(chunkSize > 0) { "chunkSize must be positive" }
    }

    fun RedBlackTree2<BigTextNodeValue>.findNodeByLineBreaks(index: Int): Pair<RedBlackTree<BigTextNodeValue>.Node, Int>? {
        var find = index
        var lineStart = 0
        return findNode {
            index
            when (find) {
                in Int.MIN_VALUE until it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
//                it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
                in it.value.leftNumOfLineBreaks until it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange -> 0
                in it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange  until Int.MAX_VALUE -> (if (it.right.isNotNil()) 1 else 0).also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange
                        lineStart += it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }?.let { it to lineStart + it.value.leftNumOfLineBreaks /*findLineStart(it)*/ }
    }

    fun RedBlackTree2<BigTextNodeValue>.findNodeByLineBreaksExact(index: Int): Pair<RedBlackTree<BigTextNodeValue>.Node, Int>? {
        var find = index
        var lineStart = 0
        return findNode {
            index
            when (find) {
                in Int.MIN_VALUE .. it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
//                it.value.leftNumOfLineBreaks -> if (it.left.isNotNil()) -1 else 0
                in it.value.leftNumOfLineBreaks + 1 .. it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange -> 0
                in it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange + 1  until Int.MAX_VALUE -> (if (it.right.isNotNil()) 1 else 0).also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange
                        lineStart += it.value.leftNumOfLineBreaks + it.value.renderNumLineBreaksInRange
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
                in Int.MIN_VALUE .. it.value.leftNumOfRowBreaks -> if (it.left.isNotNil()) -1 else 0
                in it.value.leftNumOfRowBreaks + 1  .. it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size -> 0
                in it.value.leftNumOfRowBreaks + it.value.rowBreakOffsets.size + 1  until Int.MAX_VALUE -> (if (it.right.isNotNil()) 1 else 0).also { compareResult ->
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

    override fun findPositionByRowIndex(index: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        return tree.findNodeByRowBreaks(index - 1)!!.second
    }

    override fun findRowIndexByPosition(position: Int): Int {
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
        val node = tree.findNodeByRenderCharIndex(position)!!
        val startPos = findRenderPositionStart(node)
        val nv = node.value
        val rowIndexInThisPartition = nv.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(position - startPos + nv.bufferOffsetStart) + 1
        val startRowIndex = findRowStart(node)
        return startRowIndex + rowIndexInThisPartition
    }

    override fun findRowPositionStartIndexByRowIndex(index: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        require(index in (0 .. numOfRows)) { "Row index $index is out of bound. numOfRows = $numOfRows" }
        if (index == 0) {
            return 0;
        }

        val node = (tree.findNodeByRowBreaks(index)
            ?: throw IllegalStateException("Cannot find the node right after ${index - 1} row breaks")
        ).first
        val rowStart = findRowStart(node)
        val startPos = findRenderPositionStart(node)
        return startPos + if (index - 1 - rowStart == node.value.rowBreakOffsets.size && node.value.isEndWithForceRowBreak) {
            node.value.bufferLength
        } else if (index - 1 - rowStart >= 0) { /* TODO: In asynchronous layout, IndexOutOfBoundsException can be thrown when `rowBreakOffsets` is empty or has less elements than expected */
            node.value.rowBreakOffsets[index - 1 - rowStart] - node.value.renderBufferStart
        } else {
            0
        }
    }

    override fun findPositionStartOfLine(lineIndex: Int): Int {
        val (node, lineIndexStart) = tree.findNodeByLineBreaksExact(lineIndex)
            ?: throw IndexOutOfBoundsException("Cannot find node for line $lineIndex")
        val positionStart = findPositionStart(node)
        val lineBreakIndex = lineIndex - lineIndexStart - 1

        val positionStartOffsetOfLine = if (lineBreakIndex >= 0) {
            val lineOffsets = node.value.buffer.lineOffsetStarts
            val lineOffsetStartIndex = lineOffsets.binarySearchForMinIndexOfValueAtLeast(node.value.renderBufferStart)
            require(lineOffsetStartIndex >= 0)
            lineOffsets[lineOffsetStartIndex + lineBreakIndex] -
                    node.value.renderBufferStart +
                    /* find the position just after the '\n' char */ 1
        } else {
            0
        }

        return positionStart + positionStartOffsetOfLine
    }

    /**
     * @param rowIndex 0-based
     * @return 0-based
     */
    override fun findLineIndexByRowIndex(rowIndex: Int): Int {
        if (!hasLayouted) {
            return 0
        }

        require(rowIndex in (0 .. numOfRows)) { "Row index $rowIndex is out of bound. numOfRows = $numOfRows"}
        if (rowIndex == 0) {
            return 0
        }

        val (node, rowIndexStart) = tree.findNodeByRowBreaks(rowIndex)!!
        val rowOffset = if (rowIndex - rowIndexStart - 1 == node.value.rowBreakOffsets.size && node.value.isEndWithForceRowBreak) {
            node.value.renderBufferEndExclusive
        } else if (rowIndex - rowIndexStart - 1 >= 0) { // FIXME > or >=?
            val i = rowIndex - rowIndexStart - 1 // 0-th row break is the 1st row break. Usually rowBreakOffsets[0] > 0.
            if (i > node.value.rowBreakOffsets.lastIndex) {
                printDebug("IndexOutOfBoundsException")
                throw IndexOutOfBoundsException("findLineIndexByRowIndex($rowIndex) rowBreakOffsets[$i] length ${node.value.rowBreakOffsets.size}")
            }
            node.value.rowBreakOffsets[i]
        } else {
            0
        }
        val positionStart = findRenderPositionStart(node)
        val rowPositionStart = positionStart + rowOffset - node.value.renderBufferStart
        val lineBreakPosition = maxOf(0, rowPositionStart - 1)

        val lineBreakAtNode = tree.findNodeByRenderCharIndex(lineBreakPosition)!!
        val lineStart = findLineStart(lineBreakAtNode)
        val positionStartOfLineBreakNode = findRenderPositionStart(lineBreakAtNode)
        val lineBreakOffsetStarts = lineBreakAtNode.value.buffer.lineOffsetStarts
        // FIXME render pos domain should be converted to buffer pos domain before searching
        val lineBreakMinIndex = lineBreakOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineBreakAtNode.value.renderBufferStart)
        val lineBreakIndex = lineBreakOffsetStarts.binarySearchForMaxIndexOfValueAtMost(lineBreakPosition - positionStartOfLineBreakNode + lineBreakAtNode.value.renderBufferStart)
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
        val lineOffsetStarts = lineStartNode.value.buffer.lineOffsetStarts
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
    override fun findFirstRowIndexOfLine(lineIndex: Int): Int {
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
        val lineOffsetStarts = lineStartNode.value.buffer.lineOffsetStarts
        val inRangeLineStartIndex = lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineStartNode.value.renderBufferStart)
        val lineOffset = if (lineIndex - 1 >= 0) {
            lineOffsetStarts[inRangeLineStartIndex + lineIndex - 1 - lineIndexStart] - lineStartNode.value.renderBufferStart
        } else {
            0
        }
        val lineStartPos = findRenderPositionStart(lineStartNode) + lineOffset

//        val rowBreakOffsetIndex = lineStartNode.findRowBreakIndexOfLineOffset(lineOffset)
//        val rowBreaksStart = findRowStart(lineStartNode)
//        return rowBreaksStart + rowBreakOffsetIndex + 1

        val rowStartPos = lineStartPos + 1 /* rowBreak is 1 char after '\n' while lineBreak is right at '\n' */
        val actualNode = tree.findNodeByRenderCharIndex(rowStartPos) ?: run {
            if (rowStartPos == length && tree.rightmost(tree.getRoot()).value.isEndWithForceRowBreak) {
                return lastRowIndex
            }
            throw IndexOutOfBoundsException("pos $rowStartPos is out of bound. length = $length")
        }
        val actualNodeStartPos = findRenderPositionStart(actualNode)
        val rowBreaksStart = findRowStart(actualNode)
        if (actualNode.value.isEndWithForceRowBreak && rowStartPos - actualNodeStartPos + actualNode.value.renderBufferStart >= actualNode.value.renderBufferEndExclusive) {
            return rowBreaksStart + actualNode.value.rowBreakOffsets.size + 1
        }
        val rowBreakOffsetIndex = actualNode.value.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(rowStartPos - actualNodeStartPos + actualNode.value.renderBufferStart)
        return rowBreaksStart + rowBreakOffsetIndex + 1
    }

    protected fun findPositionStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
        return tree.findPositionStart(node)
    }

    protected fun findRenderPositionStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
        var start = node.value.leftRenderLength
        var node = node
        while (node.parent.isNotNil()) {
            if (node === node.parent.right) {
                start += node.parent.value.leftRenderLength + node.parent.value.currentRenderLength
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
                start += node.parent.value.leftNumOfLineBreaks + node.parent.value.renderNumLineBreaksInRange
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

    protected open fun createNodeValue(): BigTextNodeValue {
        return BigTextNodeValue()
    }

    private fun insertChunkAtPosition(position: Int, chunkedString: CharSequence) {
        log.d { "$this insertChunkAtPosition($position, ${chunkedString.length})" }
        log.v { "$this insertChunkAtPosition($position, $chunkedString)" }
        require(chunkedString.length <= chunkSize)
//        if (position == 64) {
//            log.d { inspect("$position") }
//        }
        var buffer = if (buffers.isNotEmpty()) {
            buffers.last().takeIf { it.length + chunkedString.length <= chunkSize }
        } else null
        if (buffer == null) {
            buffer = textBufferFactory(chunkSize)
            buffers += buffer
        }
        require(buffer.length + chunkedString.length <= chunkSize)
        val range = buffer.append(chunkedString)
        insertChunkAtPosition(position, chunkedString.length, BufferOwnership.Owned, buffer, range) {
            bufferIndex = buffers.lastIndex
            bufferOffsetStart = range.start
            bufferOffsetEndExclusive = range.endInclusive + 1
            this.buffer = buffers[bufferIndex]
            this.bufferOwnership = BufferOwnership.Owned

            leftStringLength = 0
        }
        if (isUndoEnabled) {
            recordCurrentUndoMetadata()
            currentChanges += BigTextInputChange(
                type = BigTextChangeEventType.Insert,
                buffer = buffer,
                bufferCharIndexes = range.start .. range.endInclusive,
                positions = position until position + chunkedString.length
            ).also {
                log.v { "Record change for undo: $it" }
            }
            clearRedoHistory()
        }
    }

    protected fun insertChunkAtPosition(position: Int, chunkedStringLength: Int, ownership: BufferOwnership, buffer: TextBuffer, range: IntRange, isInsertAtRightmost: Boolean = false, newNodeConfigurer: BigTextNodeValue.() -> Unit): Int {
//        var node = tree.findNodeByCharIndex(position, !isInsertAtRightmost) // TODO optimize, don't do twice
        fun findNodeJustBeforePosition(position: Int, isThrowErrorIfMissing: Boolean): Pair<RedBlackTree<BigTextNodeValue>.Node?, Int?> {
            // kotlin compiler bug: `node` and `nodeStart` are wrongly interpreted as the outer one, thus smart cast is impossible
            // make the variable names different to workaround
            var node_ = tree.findNodeByCharIndex(position, true) // TODO optimize, don't do twice
            var nodeStart_ = node_?.let { findPositionStart(it) } // TODO optimize, don't do twice
            val findPosition = maxOf(0, position - 1)
            if (node_ != null && findPosition < nodeStart_!!) {
                node_ = tree.prevNode(node_!!)
                nodeStart_ = node_?.let { findPositionStart(it) }
            }
            if (node_ != null) {
                log.d { "> existing node (${node_!!.value.debugKey()}) ${node_!!.value.bufferOwnership.name.first()} $nodeStart_ .. ${nodeStart_!! + node_!!.value.bufferLength - 1}" }
                require(
                    findPosition in nodeStart_!!..nodeStart_!! + node_!!.value.bufferLength - 1 || node_!!.value.bufferLength == 0
                ) {
                    printDebug()
                    findPositionStart(node_!!)
                    "Found node ${node_!!.value.debugKey()} but it is not in searching range"
                }
            } else if (isThrowErrorIfMissing && !tree.isEmpty) {
                throw IllegalStateException("Node not found for position ${maxOf(0, position - 1)}")
            }
            return node_ to nodeStart_
        }
        var (node, nodeStart) = findNodeJustBeforePosition(
            position = if (isInsertAtRightmost) position + 1 else position,
            isThrowErrorIfMissing = !isInsertAtRightmost
        )
        if (node == null && isInsertAtRightmost && isNotEmpty) {
            log.w { "Node $position not found. Find ${position - 1} instead" }
            val r = findNodeJustBeforePosition(position = position, isThrowErrorIfMissing = true)
            node = r.first
            nodeStart = r.second
        }
        var insertDirection: InsertDirection = InsertDirection.Undefined
        val toBeRelayouted = mutableListOf<BigTextNodeValue>()
        var newContentNode: BigTextNodeValue? = null
        val newNodeValues = /*if (isInsertAtRightmost && node != null && position == nodeStart!! + node.value.bufferLength - 1) {
            log.d { "> create new node right" }
            insertDirection = InsertDirection.Right
            listOf(createNodeValue().apply {
                this.newNodeConfigurer()
                newContentNode = this
            })
        } else*/ if (node != null && position > 0 && position in nodeStart!! .. nodeStart!! + node.value.bufferLength - 1) {
            val splitAtIndex = position - nodeStart
            log.d { "> split at $splitAtIndex" }
            val oldEnd = node.value.bufferOffsetEndExclusive
            val secondPartNodeValue = createNodeValue().apply { // the second part of the old string
                bufferIndex = node!!.value.bufferIndex
                updateRightValueDuringNodeSplit(
                    rightNodeValue = this,
                    oldNodeValue = node!!.value,
                    splitAtIndex = splitAtIndex
                )
                this.buffer = node!!.value.buffer
                this.bufferOwnership = node!!.value.bufferOwnership

                leftStringLength = 0
            }
            /**
             * Existing node char index range is (A ..< B), where A <= position < B, position is the insert position.
             * Modify nodes so that existing node is (A ..< position),
             * new node 1 (position ..< position + length),
             * new node 2 (position + length ..< B).
             * If A == position, then existing node is empty and thus can be deleted.
             */
            if (splitAtIndex > 0) {
                updateLeftValueDuringNodeSplit(
                    leftNodeValue = node!!.value,
                    oldNodeValue = node!!.value,
                    splitAtIndex = splitAtIndex
                )
            } else {
                val prevNode = tree.prevNode(node)
                tree.delete(node)
                node = prevNode
                if (node != null && isInsertAtRightmost) {
                    val nodeStart = findPositionStart(node)
                    if (nodeStart + node.value.bufferLength <= position) {
                        insertDirection = InsertDirection.Right
                    } else {
                        insertDirection = InsertDirection.Left
                    }
                } else {
                    insertDirection = InsertDirection.Left
                }
            }
            // require(splitAtIndex + chunkedStringLength <= chunkSize) // this check appears to be not guarding anything
            toBeRelayouted += secondPartNodeValue
            listOf(
                createNodeValue().apply { // new string
                    this.newNodeConfigurer()
                    newContentNode = this
                },
                secondPartNodeValue
            ).reversed() // IMPORTANT: the insertion order is reversed
        } else if (node == null || node.value.bufferOwnership != ownership || node.value.buffer !== buffer || (node.value.bufferOwnership == BufferOwnership.Owned && node.value.bufferIndex != buffers.lastIndex) || node.value.bufferOffsetEndExclusive != range.start || position == 0) {
            log.d { "> create new node" }
            listOf(createNodeValue().apply {
                this.newNodeConfigurer()
                newContentNode = this
            })
        } else {
            node.value.apply {
                log.d { "> update existing node end from $bufferOffsetEndExclusive to ${bufferOffsetEndExclusive + range.length}" }
                bufferOffsetEndExclusive += chunkedStringLength
                newContentNode = createNodeValue().apply {
                    this.newNodeConfigurer()
                    newContentNode = this
                }
            }
            recomputeAggregatedValues(node)
            emptyList()
        }
        var leftmostNewNodeValue: RedBlackTree<BigTextNodeValue>.Node? = null
        if (newNodeValues.isNotEmpty() && insertDirection == InsertDirection.Left) {
            node = if (node != null) {
                tree.insertLeft(node, newNodeValues.first())
            } else {
                tree.insertValue(newNodeValues.first())!!
            }
            (1 .. newNodeValues.lastIndex).forEach {
                node = tree.insertLeft(node!!, newNodeValues[it])
            }
            leftmostNewNodeValue = node
        } else {
            val existingNodePositionStart = node?.value?.leftStringLength
            newNodeValues.reversed().forEach {
                node = if (existingNodePositionStart == position && insertDirection != InsertDirection.Right) {
                    tree.insertLeft(node!!, it) // insert before existing node
                } else if (node != null) {
                    tree.insertRight(node!!, it)
                } else {
                    tree.insertValue(it)
                }
                if (leftmostNewNodeValue == null) {
                    leftmostNewNodeValue = node
                }
            }
        }

        toBeRelayouted.forEach {
            val startPos = findRenderPositionStart(it.node!!)
            val endPos = startPos + it.currentRenderLength
            layout(startPos, endPos)
        }

        let(changeHook, newContentNode) { hook, nodeValue ->
            hook.afterInsertChunk(this, position, nodeValue)
        }

        log.v { inspect("Finish I " + node?.value?.debugKey()) }

        return leftmostNewNodeValue?.let { findRenderPositionStart(it) } ?: 0
    }

    protected open fun updateRightValueDuringNodeSplit(rightNodeValue: BigTextNodeValue, oldNodeValue: BigTextNodeValue, splitAtIndex: Int) {
        with (rightNodeValue) {
            bufferOffsetStart = oldNodeValue.bufferOffsetStart + splitAtIndex
            bufferOffsetEndExclusive = oldNodeValue.bufferOffsetEndExclusive
        }
    }

    protected open fun updateLeftValueDuringNodeSplit(leftNodeValue: BigTextNodeValue, oldNodeValue: BigTextNodeValue, splitAtIndex: Int) {
        with (leftNodeValue) {
            bufferOffsetStart = oldNodeValue.bufferOffsetStart
            bufferOffsetEndExclusive = oldNodeValue.bufferOffsetStart + splitAtIndex
        }
    }

    open fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue, left: RedBlackTree<BigTextNodeValue>.Node?) = with (nodeValue) {
        // recompute leftStringLength
        leftStringLength = left?.length() ?: 0
        log.v { ">> ${node?.value?.debugKey()} -> $leftStringLength (${left?.value?.debugKey()}/ ${left?.length()})" }

        // recompute leftNumOfLineBreaks
//        bufferNumLineBreaksInRange = buffers[bufferIndex].lineOffsetStarts.subSet(bufferOffsetStart, bufferOffsetEndExclusive).size
        bufferNumLineBreaksInRange = buffer.lineOffsetStarts.run {
            binarySearchForMinIndexOfValueAtLeast(bufferOffsetEndExclusive) - maxOf(0, binarySearchForMinIndexOfValueAtLeast(bufferOffsetStart))
        }
        renderNumLineBreaksInRange = if (currentRenderLength > 0) {
            buffer.lineOffsetStarts.run {
                binarySearchForMinIndexOfValueAtLeast(renderBufferEndExclusive) - maxOf(0, binarySearchForMinIndexOfValueAtLeast(renderBufferStart))
            }
        } else {
            0
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
//            assert(node === node.getValue().node)
            with (node.getValue()) {
                computeCurrentNodeProperties(this, left)
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

    override val lastIndex: Int
        get() = length - 1

    override val isEmpty: Boolean
        get() = length <= 0

    override val isNotEmpty: Boolean
        get() = length > 0

    override fun buildString(): String {
        return tree.joinToString("") {
            it.buffer.subSequence(it.renderBufferStart, it.renderBufferEndExclusive)
        }
    }

    override fun buildCharSequence(): CharSequence {
        val builder = charSequenceBuilderFactory(length)
        tree.forEach {
            builder.append(it.buffer.subSequence(it.renderBufferStart, it.renderBufferEndExclusive))
        }
        return charSequenceFactory(builder)
    }

    override fun substring(start: Int, endExclusive: Int): CharSequence { // O(lg L + (e - s))
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive $endExclusive is out of bound. length = $length" }

        if (start == endExclusive) {
            return ""
        }

        val result = charSequenceBuilderFactory(endExclusive - start)
        var node = tree.findNodeByRenderCharIndex(start) ?: throw IllegalStateException("Cannot find string node for position $start")
        var nodeStartPos = findRenderPositionStart(node)
        var numRemainCharsToCopy = endExclusive - start
        var copyFromBufferIndex = start - nodeStartPos + node.value.renderBufferStart
        while (numRemainCharsToCopy > 0) {
            val numCharsToCopy = minOf(endExclusive, nodeStartPos + node.value.currentRenderLength) - maxOf(start, nodeStartPos)
            val copyUntilBufferIndex = copyFromBufferIndex + numCharsToCopy
            if (numCharsToCopy > 0) {
                val subsequence = node.value.buffer.subSequence(copyFromBufferIndex, copyUntilBufferIndex)
                result.append(subsequence)
                numRemainCharsToCopy -= numCharsToCopy
            } /*else {
                break
            }*/
            if (numRemainCharsToCopy > 0) {
                nodeStartPos += node.value.currentRenderLength
                node = tree.nextNode(node) ?: throw IllegalStateException("Cannot find the next string node. Requested = $start ..< $endExclusive. Remain = $numRemainCharsToCopy")
                copyFromBufferIndex = node.value.renderBufferStart
            }
        }

        return charSequenceFactory(result)
    }

    // TODO: refactor not to duplicate implementation of substring
    override fun subSequence(start: Int, endExclusive: Int): CharSequence {
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive $endExclusive is out of bound. length = $length" }

        if (start == endExclusive) {
            return charSequenceFactory(charSequenceBuilderFactory(0))
        }

        log.v { "subSequence start" }

        val result = charSequenceBuilderFactory(endExclusive - start)
        var node = tree.findNodeByRenderCharIndex(start) ?: throw IllegalStateException("Cannot find string node for position $start")
        var nodeStartPos = findRenderPositionStart(node)
        var numRemainCharsToCopy = endExclusive - start
        var copyFromBufferIndex = start - nodeStartPos + node.value.renderBufferStart
        log.v { "subSequence before loop ($numRemainCharsToCopy)" }
        while (numRemainCharsToCopy > 0) {
            val copyEndExclusive = minOf(endExclusive, nodeStartPos + node.value.currentRenderLength)
            val copyStart = maxOf(start, nodeStartPos)
            val numCharsToCopy = copyEndExclusive - copyStart
            val copyUntilBufferIndex = copyFromBufferIndex + numCharsToCopy
            if (numCharsToCopy > 0) {
                val bufferSubsequence = node.value.buffer.subSequence(copyFromBufferIndex, copyUntilBufferIndex)
                log.v { "subSequence loop ($numRemainCharsToCopy) -- obtained sq" }
                val subsequence = if (decorator != null) {
                    decorate(node.value, bufferSubsequence, copyStart until copyEndExclusive).also {
                        if (it.length != numCharsToCopy) {
                            throw IllegalStateException("Returned CharSequence from decorator has length of ${it.length}. Expected length: $numCharsToCopy")
                        }
                    }
                } else {
                    bufferSubsequence
                }
                log.v { "subSequence loop ($numRemainCharsToCopy) -- obtained sq dec" }
                result.append(subsequence)
                log.v { "subSequence loop ($numRemainCharsToCopy) -- appended" }
                numRemainCharsToCopy -= numCharsToCopy
            } /*else {
                break
            }*/
            if (numRemainCharsToCopy > 0) {
                nodeStartPos += node.value.currentRenderLength
                node = tree.nextNode(node) ?: throw IllegalStateException("Cannot find the next string node. Requested = $start ..< $endExclusive. Remain = $numRemainCharsToCopy")
                copyFromBufferIndex = node.value.renderBufferStart
                log.v { "subSequence loop ($numRemainCharsToCopy) -- next" }
            }
        }

        return charSequenceFactory(result).also {
            log.v { "subSequence built" }
        }
    }

    protected open fun decorate(nodeValue: BigTextNodeValue, text: CharSequence, renderPositions: IntRange) =
        decorator!!.onApplyDecorationOnOriginal(text, renderPositions)

    /**
     * @param lineOffset 0 = start of buffer; 1 = char index after the 1st '\n'; 2 = char index after the 2nd '\n'; ...
     */
    protected fun findCharPosOfLineOffset(node: RedBlackTree<BigTextNodeValue>.Node, lineOffset: Int): Int {
        val buffer = node.value!!.buffer
        val lineStartIndexInBuffer = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(node.value!!.bufferOffsetStart)
        val lineEndIndexInBuffer = buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(node.value!!.bufferOffsetEndExclusive - 1)
        val offsetedLineOffset = maxOf(0, lineStartIndexInBuffer) + (lineOffset) - 1
        val charOffsetInBuffer = if (offsetedLineOffset > lineEndIndexInBuffer) {
            node.value!!.renderBufferEndExclusive
        } else if (lineOffset - 1 >= 0) {
            buffer.lineOffsetStarts[offsetedLineOffset] + 1
        } else {
            node.value!!.renderBufferStart
        }
        return findPositionStart(node) + (charOffsetInBuffer - node.value!!.renderBufferStart)
    }

    override fun findLineString(lineIndex: Int): CharSequence {
        require(0 <= lineIndex) { "lineIndex $lineIndex must be non-negative." }
        require(lineIndex <= numOfLines) { "lineIndex $lineIndex out of bound, numOfLines = $numOfLines." }

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

    override fun findRowString(rowIndex: Int): CharSequence {
        /**
         * @param rowOffset 0 = start of buffer; 1 = char index of the first row break
         */
        fun findCharPosOfRowOffset(node: RedBlackTree<BigTextNodeValue>.Node, rowOffset: Int): Int {
            val charOffsetInBuffer = if (rowOffset - 1 > node.value!!.rowBreakOffsets.size - 1) {
                node.value!!.renderBufferEndExclusive
            } else if (rowOffset - 1 >= 0) {
                val offsetedRowOffset = rowOffset - 1
                node.value!!.rowBreakOffsets[offsetedRowOffset]
            } else { // rowOffset == 0
                node.value!!.renderBufferStart
            }
            return findRenderPositionStart(node) + (charOffsetInBuffer - node.value!!.renderBufferStart)
        }

        val (startNode, startNodeRowStart) = tree.findNodeByRowBreaks(rowIndex) ?:
            if (rowIndex <= numOfRows) {
                return ""
            } else {
                throw IndexOutOfBoundsException("numOfRows = $numOfRows; but given index = $rowIndex")
            }
        val endNodeFindPair = tree.findNodeByRowBreaks(rowIndex + 1)
        val endCharIndex = if (endNodeFindPair != null) { // includes the last '\n' char
            val (endNode, endNodeRowStart) = endNodeFindPair
            require(endNodeRowStart <= rowIndex + 1) { "Node ${endNode.value.debugKey()} violates [endNodeRowStart <= rowIndex] ($endNodeRowStart)" }
//            val lca = tree.lowestCommonAncestor(startNode, endNode)
            findCharPosOfRowOffset(endNode, rowIndex + 1 - endNodeRowStart)
        } else {
            length
        }
        val startCharIndex = findCharPosOfRowOffset(startNode, rowIndex - startNodeRowStart)
        logQ.d { "row #$rowIndex -> $startCharIndex ..< $endCharIndex" }
        return substring(startCharIndex, endCharIndex) // includes the last '\n' char
    }

    override fun append(text: CharSequence): Int {
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

    override fun insertAt(pos: Int, text: CharSequence): Int {
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
        require(0 <= start) { "Invalid start ($start)" }
        require(endExclusive <= length) { "endExclusive is out of bound. Given endExclusive = $endExclusive). Length = $length" }

        return deleteUnchecked(start, endExclusive).also {
            changeHook?.afterDelete(this, start until endExclusive)
        }
    }

    protected fun deleteUnchecked(start: Int, endExclusive: Int, deleteMarker: BigTextNodeValue? = null, isSkipLayout: Boolean = false): Int {
        if (start == endExclusive) {
            return 0
        }

        log.d { "$this delete $start ..< $endExclusive" }

        var node: RedBlackTree<BigTextNodeValue>.Node? = tree.findNodeByCharIndex(endExclusive - 1, isIncludeMarkerNodes = false)
        var nodeRange = charIndexRangeOfNode(node!!)
        val newNodesInDescendingOrder = mutableListOf<BigTextNodeValue>()
        var hasAddedDeleteMarker = false
        while (node?.isNotNil() == true && start <= nodeRange.endInclusive) {
            if (isD && nodeRange.start == 0) {
                isD = true
            }
            var splitStartAt = 0
            var splitEndAt = node.value.bufferLength
            if (endExclusive - 1 in nodeRange.start..nodeRange.last - 1) {
                // need to split
                val splitAtIndex = endExclusive - nodeRange.start
                splitEndAt = splitAtIndex
                log.d { "Split E at $splitAtIndex" }
                newNodesInDescendingOrder += createNodeValue().apply { // the second part of the existing string
                    bufferIndex = node!!.value.bufferIndex // FIXME transform
                    updateRightValueDuringNodeSplit(
                        rightNodeValue = this,
                        oldNodeValue = node!!.value,
                        splitAtIndex = splitAtIndex
                    )
                    buffer = node!!.value.buffer
                    bufferOwnership = node!!.value.bufferOwnership

                    leftStringLength = 0
                }
            }
            if (start in nodeRange.start + 1 .. nodeRange.last) {
                if (!hasAddedDeleteMarker && deleteMarker != null) {
                    newNodesInDescendingOrder += deleteMarker
                    hasAddedDeleteMarker = true
                }

                // need to split
                val splitAtIndex = start - nodeRange.start
                splitStartAt = splitAtIndex
                log.d { "Split S at $splitAtIndex" }
                newNodesInDescendingOrder += createNodeValue().apply { // the first part of the existing string
                    bufferIndex = node!!.value.bufferIndex
                    updateLeftValueDuringNodeSplit(
                        leftNodeValue = this,
                        oldNodeValue = node!!.value,
                        splitAtIndex = splitAtIndex
                    )
                    buffer = node!!.value.buffer
                    bufferOwnership = node!!.value.bufferOwnership

                    leftStringLength = 0
                }
            }
            val prev = tree.prevNode(node)
            log.d { "Delete node ${node!!.value.debugKey()} at ${nodeRange.start} .. ${nodeRange.last}" }
            if (nodeRange.start == 2083112) {
                isD = true
            }
            if (isUndoEnabled) {
                recordCurrentUndoMetadata()
                currentChanges += BigTextInputChange(
                    type = BigTextChangeEventType.Delete,
                    buffer = node.value.buffer,
                    bufferCharIndexes = node.value.bufferOffsetStart + splitStartAt
                            until
                            node.value.bufferOffsetStart + splitEndAt,
                    positions = nodeRange.start + splitStartAt
                            until
                            nodeRange.start + splitEndAt,
                ).also {
                    log.v { "Record change for undo: $it" }
                }
                clearRedoHistory()
            }
            tree.delete(node)
            log.v { inspect("After delete " + node?.value?.debugKey()) }
            node = prev
//            nodeRange = nodeRange.start - chunkSize .. nodeRange.last - chunkSize
            if (node != null) {
                nodeRange = charIndexRangeOfNode(node) // TODO optimize by calculation instead of querying
            }
        }

        if (!hasAddedDeleteMarker && deleteMarker != null) {
            newNodesInDescendingOrder += deleteMarker
            hasAddedDeleteMarker = true
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
////            computeCurrentNodeProperties(it.value)
//            recomputeAggregatedValues(it)
//        }

        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "deleteUnchecked -- before layout" }

        // layout the new nodes explicitly, as
        // the layout outside the loop may not be able to touch the new nodes
        newNodesInDescendingOrder.forEach {
            val startPos = findRenderPositionStart(it.node!!)
            val endPos = startPos + it.currentRenderLength
            layout(startPos, endPos)
        }

        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "deleteUnchecked -- after inner layout 1" }

        if (!isSkipLayout) {
            layout(maxOf(0, start - 1), minOf(length, start + 1))
        }

        com.sunnychung.application.multiplatform.hellohttp.util.log.v { "deleteUnchecked -- after inner layout 2" }

        log.v { inspect("Finish D " + node?.value?.debugKey()) }

        return -(endExclusive - start)
    }

    override fun recordCurrentChangeSequenceIntoUndoHistory() {
        if (!isUndoEnabled) {
            return
        }

        if (currentChanges.isNotEmpty()) {
            undoHistory.push(BigTextInputOperation(currentChanges.toList(), currentUndoMetadata, undoMetadataSupplier?.invoke()))
            currentChanges = mutableListOf()
            recordCurrentUndoMetadata()
        }
    }

    protected fun recordCurrentUndoMetadata() {
        if (currentChanges.isEmpty()) {
            currentUndoMetadata = undoMetadataSupplier?.invoke()
        }
        currentRedoMetadata = undoMetadataSupplier?.invoke()
        log.d { "reset um = $currentUndoMetadata, rm = $currentRedoMetadata" }
    }

    protected fun clearRedoHistory() {
        redoHistory.clear()
    }

    protected fun applyReverseChangeSequence(changes: List<BigTextInputChange>, callback: BigTextChangeCallback?) {
        if (!isUndoEnabled) {
            return
        }
        try {
            isUndoEnabled = false // don't record the following changes into the undo history

            changes.asReversed().forEach {
                when (it.type) {
                    BigTextChangeEventType.Delete -> {
                        callback?.onValuePreChange(BigTextChangeEventType.Insert, it.positions.start, it.positions.endInclusive + 1)
                        insertChunkAtPosition(it.positions.start, it.bufferCharIndexes.length, BufferOwnership.Owned, it.buffer, it.bufferCharIndexes)  {
                            bufferIndex = -3
                            bufferOffsetStart = it.bufferCharIndexes.start
                            bufferOffsetEndExclusive = it.bufferCharIndexes.endInclusive + 1
                            this.buffer = it.buffer
                            this.bufferOwnership = BufferOwnership.Owned

                            leftStringLength = 0
                        }
                        callback?.onValuePostChange(BigTextChangeEventType.Insert, it.positions.start, it.positions.endInclusive + 1)
                    }

                    BigTextChangeEventType.Insert -> {
                        callback?.onValuePreChange(BigTextChangeEventType.Delete, it.positions.start, it.positions.endInclusive + 1)
                        delete(it.positions)
                        callback?.onValuePostChange(BigTextChangeEventType.Delete, it.positions.start, it.positions.endInclusive + 1)
                    }
                }
            }
        } finally {
            isUndoEnabled = true
        }
    }

    protected fun applyChangeSequence(changes: List<BigTextInputChange>, callback: BigTextChangeCallback?) {
        if (!isUndoEnabled) {
            return
        }
        try {
            isUndoEnabled = false // don't record the following changes into the undo history

            changes.forEach {
                when (it.type) {
                    BigTextChangeEventType.Insert -> {
                        callback?.onValuePreChange(BigTextChangeEventType.Insert, it.positions.start, it.positions.endInclusive + 1)
                        insertChunkAtPosition(it.positions.start, it.bufferCharIndexes.length, BufferOwnership.Owned, it.buffer, it.bufferCharIndexes)  {
                            bufferIndex = -3
                            bufferOffsetStart = it.bufferCharIndexes.start
                            bufferOffsetEndExclusive = it.bufferCharIndexes.endInclusive + 1
                            this.buffer = it.buffer
                            this.bufferOwnership = BufferOwnership.Owned

                            leftStringLength = 0
                        }
                        callback?.onValuePostChange(BigTextChangeEventType.Insert, it.positions.start, it.positions.endInclusive + 1)
                    }

                    BigTextChangeEventType.Delete -> {
                        callback?.onValuePreChange(BigTextChangeEventType.Delete, it.positions.start, it.positions.endInclusive + 1)
                        delete(it.positions)
                        callback?.onValuePostChange(BigTextChangeEventType.Delete, it.positions.start, it.positions.endInclusive + 1)
                    }
                }
            }
        } finally {
            isUndoEnabled = true
        }
    }

    override fun undo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> {
        if (!isUndoEnabled) {
            return false to null
        }
        if (currentChanges.isNotEmpty()) {
            val undoMetadata = currentUndoMetadata
            val redoMetadata = currentRedoMetadata
            applyReverseChangeSequence(currentChanges, callback)
            redoHistory.push(BigTextInputOperation(currentChanges.toList(), undoMetadata, redoMetadata))
            currentChanges = mutableListOf()
            recordCurrentUndoMetadata()
            return true to undoMetadata
        }
        val lastOperation = undoHistory.removeHead() ?: return false to null
        applyReverseChangeSequence(lastOperation.changes, callback)
        currentUndoMetadata = lastOperation.undoMetadata
        currentRedoMetadata = lastOperation.redoMetadata
        log.d { "undo set um = $currentUndoMetadata, rm = $currentRedoMetadata" }
        redoHistory.push(lastOperation)
        return true to lastOperation.undoMetadata
    }

    override fun redo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> {
        if (!isUndoEnabled) {
            return false to null
        }
        if (currentChanges.isNotEmpty()) { // should not happen
            return false to null
        }
        val lastOperation = redoHistory.removeHead() ?: return false to null
        applyChangeSequence(lastOperation.changes, callback)
        currentUndoMetadata = lastOperation.undoMetadata
        currentRedoMetadata = lastOperation.redoMetadata
        log.d { "undo set um = $currentUndoMetadata, rm = $currentRedoMetadata" }
        undoHistory.push(lastOperation)
        return true to lastOperation.redoMetadata
    }

    override fun isUndoable(): Boolean = isUndoEnabled && (currentChanges.isNotEmpty() || undoHistory.isNotEmpty)

    override fun isRedoable(): Boolean = isUndoEnabled && currentChanges.isEmpty() && redoHistory.isNotEmpty

    fun charIndexRangeOfNode(node: RedBlackTree<BigTextNodeValue>.Node): IntRange {
        val start = findPositionStart(node)
        return start until start + node.value.bufferLength
    }

    override fun findLineAndColumnFromRenderPosition(renderPosition: Int): Pair<Int, Int> {
        if (tree.isEmpty && renderPosition == 0) {
            return 0 to 0
        }
        val node = tree.findNodeByRenderCharIndex(renderPosition)
            ?: throw IndexOutOfBoundsException("Node for position $renderPosition not found")
        val nodeStart = findRenderPositionStart(node)
        val lineStart = findLineStart(node)

        if (node.renderLength() <= 0) {
            throw IllegalStateException("Node render length is not positive")
        }

        val buffer = node.value.buffer
        val lineBreakStartIndex = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(node.value.renderBufferStart)
        val lineBreakEndIndexInclusive = buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(node.value.renderBufferEndExclusive)
        val lineBreakOffset = minOf(
            lineBreakEndIndexInclusive + 1,
            buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(renderPosition - nodeStart + node.value.renderBufferStart)
        ).let {
            if (it >= 0 && it >= lineBreakStartIndex) {
                it - maxOf(0, lineBreakStartIndex)
            } else {
                0
            }
        }

        val lineIndex = lineStart + lineBreakOffset

        val (lineStartNode, lineStartNodeLineStart) = tree.findNodeByLineBreaks(lineIndex - 1)!!
        val lineOffsetStarts = lineStartNode.value.buffer.lineOffsetStarts
        val inRangeLineStartIndex = lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(lineStartNode.value.renderBufferStart)
        val lineOffset = if (lineIndex - 1 >= 0) {
            lineOffsetStarts[inRangeLineStartIndex + lineIndex - 1 - lineStartNodeLineStart] - lineStartNode.value.renderBufferStart + 1
        } else {
            0
        }
        val lineStartPos = findRenderPositionStart(lineStartNode) + lineOffset

        val columnIndex = renderPosition - lineStartPos

        return lineIndex to columnIndex
    }

    override fun findRenderCharIndexByLineAndColumn(lineIndex: Int, columnIndex: Int): Int {
        require(0 <= lineIndex) { "lineIndex $lineIndex must be non-negative." }
        require(lineIndex <= numOfLines) { "lineIndex $lineIndex out of bound, numOfLines = $numOfLines." }
        require(0 <= columnIndex) { "columnIndex $lineIndex must be non-negative." }

        val (startNode, startNodeLineStart) = tree.findNodeByLineBreaks(lineIndex - 1)!!
        val startCharIndex = findCharPosOfLineOffset(startNode, lineIndex - startNodeLineStart)

        return startCharIndex + columnIndex
    }

    override fun hashCode(): Int {
//        TODO("Not yet implemented")
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
//        TODO("Not yet implemented")
        return super.equals(other)
    }

    override fun inspect(label: String) = buildString {
        appendLine("[$label] Buffer:\n${buffers.mapIndexed { i, it -> "    $i:\t$it\n" }.joinToString("")}")
        appendLine("[$label] Buffer Line Breaks:\n${buffers.mapIndexed { i, it -> "    $i:\t${it.lineOffsetStarts}\n" }.joinToString("")}")
        appendLine("[$label] Tree:\nflowchart TD\n${tree.debugTree()}")
        appendLine("[$label] String:\n${buildString()}")
        if (layouter != null && contentWidth != null) {
            appendLine("[$label] Layouted String ($numOfRows):\n${(0 until numOfRows).joinToString("") {
                try {
                    "{${findRowString(it)}}\n"
                } catch (e: Throwable) {
                    "[$e]!\n"
                }
            }}")
        }
    }

    override fun printDebug(label: String) {
        println(inspect(label))
    }

    override fun setLayouter(layouter: TextLayouter) {
        if (this.layouter == layouter) {
            return
        }

        tree.forEach {
            val buffer = it.buffer
            val chunkString = buffer.subSequence(it.renderBufferStart, it.renderBufferEndExclusive)
            layouter.indexCharWidth(chunkString.toString())
        }

        this.layouter = layouter

        layout()
    }

    override fun setContentWidth(contentWidth: Float) {
        require(contentWidth > EPS) { "contentWidth must be positive" }

        if (this.contentWidth == contentWidth) {
            return
        }

        this.contentWidth = contentWidth

        layout()
    }

    override fun layout() {
        val layouter = this.layouter ?: return
        val contentWidth = this.contentWidth ?: return

        return layout(0, length)
        // the code below doesn't pass insertTriggersRelayout3(16)

        var lastOccupiedWidth = 0f
        val treeLastIndex = tree.size() - 1
        tree.forEachIndexed { index, node ->
            val buffer = node.buffer
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

    /**
     * @param startPos Begin index of render positions.
     * @param endPosExclusive End index (exclusive) of render positions.
     */
    fun layout(startPos: Int, endPosExclusive: Int) {
        if (!isLayoutEnabled) return
        _layout(startPos = startPos, endPosExclusive = endPosExclusive)
    }

    fun forceLayout(startPos: Int, endPosExclusive: Int) {
        _layout(startPos = startPos, endPosExclusive = endPosExclusive)
    }

    private fun _layout(startPos: Int, endPosExclusive: Int) {
        val layouter = this.layouter ?: return
        val contentWidth = this.contentWidth ?: return

        if (startPos >= length) return
        if (startPos >= endPosExclusive) return

        var lastOccupiedWidth = 0f
        var isLastEndWithForceRowBreak = false
        var node: RedBlackTree<BigTextNodeValue>.Node? = tree.findNodeByRenderCharIndex(startPos) ?: return
        logL.i { "layout($startPos, $endPosExclusive)" }
        logL.v { inspect("before layout($startPos, $endPosExclusive)") }
        var nodeStartPos = findRenderPositionStart(node!!)
        val nodeValue = node.value
        val buffer = nodeValue.buffer
        var lineBreakIndexFrom = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(
            (startPos - nodeStartPos) + nodeValue.renderBufferStart
        )
        var charStartIndexInBuffer = nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost((startPos - nodeStartPos) + nodeValue.renderBufferStart).let {
            if (it >= 0) {
                nodeValue.rowBreakOffsets[it]
            } else {
                val prevNode = tree.prevNode(node!!)
                if (prevNode != null) {
                    // carry over
                    lastOccupiedWidth = prevNode.value.lastRowWidth
                    isLastEndWithForceRowBreak = prevNode.value.isEndWithForceRowBreak
                    logL.d { "carry over width $lastOccupiedWidth $isLastEndWithForceRowBreak" }
                }

                nodeValue.renderBufferStart
            }
        }
        logL.d { "charStartIndexInBuffer = $charStartIndexInBuffer" }

        // we are starting at charStartIndexInBuffer without carrying over last width, so include the row break at charStartIndexInBuffer
        val restoreRowBreakOffsets = if (startPos > 0) {
            nodeValue.rowBreakOffsets.subList(0, maxOf(0, nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(charStartIndexInBuffer) + 1))
        } else {
            emptyList()
        }
        logL.v { "restore row breaks of starting node $restoreRowBreakOffsets" }
        var hasRestoredRowBreaks = false

        var isBreakOnEncounterLineBreak = false

        // TODO refactor
        while (node != null) {
            var isBreakAfterThisIteration = false
            val nodeValue = node.value
            val buffer = nodeValue.buffer
            val lineBreakIndexTo =
                buffer.lineOffsetStarts.binarySearchForMaxIndexOfValueAtMost(nodeValue.renderBufferEndExclusive - 1)
                    .let {
                        if (endPosExclusive in nodeStartPos..nodeStartPos + nodeValue.currentRenderLength) {
                            minOf(
                                it,
                                buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(endPosExclusive - nodeStartPos + nodeValue.renderBufferStart)
                            )
                        } else {
                            it
                        }
                    }
            logL.d { "node ${nodeValue.debugKey()} LB $lineBreakIndexFrom .. $lineBreakIndexTo P $nodeStartPos" }
            logL.v { "buffer ${nodeValue.bufferIndex} LB ${buffer.lineOffsetStarts}" }

//            if (lineBreakIndexFrom > lineBreakIndexTo) {
            if (nodeStartPos > endPosExclusive + 1) { // do 1 more char because last round may just fill up the row but a row break is not created
                logL.d { "set BreakOnEncounterLineBreak" }
                isBreakOnEncounterLineBreak = true
            }

//            nodeValue.rowBreakOffsets.clear()
            val rowBreakOffsets = mutableListOf<Int>()
            var isEndWithForceRowBreak = false
            logL.v { "orig row breaks ${nodeValue.rowBreakOffsets} lrw=${nodeValue.lastRowWidth} for ref only" }
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

            if (isLastEndWithForceRowBreak) {
                logL.d { "row break add carry-over force break ${charStartIndexInBuffer}" }
                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(charStartIndexInBuffer)
                lastOccupiedWidth = 0f
                isLastEndWithForceRowBreak = false
            }

            for (lineBreakEntryIndex in lineBreakIndexFrom..lineBreakIndexTo) {
                val lineBreakCharIndex = buffer.lineOffsetStarts[lineBreakEntryIndex]
                val subsequence = buffer.substring(charStartIndexInBuffer, lineBreakCharIndex)
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
                logL.d { "row break added ${rowBreakOffsets.size}" }

//                if (subsequence.isEmpty() && lastOccupiedWidth >= contentWidth - EPS) {
//                    logL.d { "row break add carry-over force break ${lineBreakCharIndex}" }
//                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(lineBreakCharIndex)
//                }

                charStartIndexInBuffer = lineBreakCharIndex + 1
                // place a row break right after the '\n' char
                if (lineBreakCharIndex + 1 < nodeValue.renderBufferEndExclusive) {
                    logL.d { "row break add ${lineBreakCharIndex + 1}" }
                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(lineBreakCharIndex + 1)
                    logL.d { "row break added ${rowBreakOffsets.size}" }
                    lastOccupiedWidth = 0f
                } else {
                    // the char after the '\n' char is not in this node
                    // mark a carry-over row break
                    lastOccupiedWidth = contentWidth + 0.1f // force a row break at the start of next layout
                    isEndWithForceRowBreak = true
                }

                if (isBreakOnEncounterLineBreak) {
                    isBreakAfterThisIteration = true

                    if (lineBreakEntryIndex + 1 <= lineBreakIndexTo && buffer.lineOffsetStarts[lineBreakEntryIndex + 1] < nodeValue.renderBufferEndExclusive) { // still remain some rows to process
                        val rowBreakOffsetDiff = lineBreakCharIndex - buffer.lineOffsetStarts[lineBreakEntryIndex]
                        val restoreRowOffsetFromIndex = nodeValue.rowBreakOffsets.binarySearchForMinIndexOfValueAtLeast(buffer.lineOffsetStarts[lineBreakEntryIndex] + 1)
                        nodeValue.rowBreakOffsets.subList(restoreRowOffsetFromIndex, nodeValue.rowBreakOffsets.size)
                            .map { it + rowBreakOffsetDiff }
                            .filter {
                                // should not happen
                                if (it >= nodeValue.renderBufferEndExclusive) throw RuntimeException("exceeds. $it")
                                it < nodeValue.renderBufferEndExclusive
                            }
                            .let { restoreRowOffsets ->
                                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(restoreRowOffsets)
                            }
                        charStartIndexInBuffer = (rowBreakOffsets.lastOrNull() ?: -1) + 1
                        lastOccupiedWidth = nodeValue.lastRowWidth
                        isEndWithForceRowBreak = nodeValue.isEndWithForceRowBreak
                    }
                    break
                }
            }
            val nextBoundary = if (
                lineBreakIndexTo + 1 <= buffer.lineOffsetStarts.lastIndex
//                    && buffer.lineOffsetStarts[lineBreakIndexTo + 1] - node.value.bufferOffsetStart + nodeStartPos < endPosExclusive
                    && buffer.lineOffsetStarts[lineBreakIndexTo + 1] < nodeValue.renderBufferEndExclusive
            ) {
                buffer.lineOffsetStarts[lineBreakIndexTo + 1]
            } else {
                nodeValue.renderBufferEndExclusive
            }
//            if (charStartIndexInBuffer < nodeValue.bufferOffsetEndExclusive) {
//            if (charStartIndexInBuffer < nodeValue.bufferOffsetEndExclusive && nodeStartPos + charStartIndexInBuffer - nodeValue.bufferOffsetStart < endPosExclusive) {
            if (!isBreakAfterThisIteration && charStartIndexInBuffer < nextBoundary) {
//                val subsequence = buffer.subSequence(charStartIndexInBuffer, nodeValue.bufferOffsetEndExclusive)
                val readRowUntilPos = nextBoundary //nodeValue.bufferOffsetEndExclusive //minOf(nodeValue.bufferOffsetEndExclusive, endPosExclusive - nodeStartPos + nodeValue.bufferOffsetStart)
                logL.d { "node ${nodeValue.debugKey()} last row seq $charStartIndexInBuffer ..< ${readRowUntilPos}. start = $nodeStartPos" }
                val subsequence = buffer.substring(charStartIndexInBuffer, readRowUntilPos)
                logL.d { "after substring" }

                val (rowCharOffsets, lastRowOccupiedWidth) = layouter.layoutOneLine(
                    subsequence,
                    contentWidth,
                    lastOccupiedWidth,
                    charStartIndexInBuffer
                )
//                nodeValue.rowBreakOffsets += rowCharOffsets
                logL.d { "row break add $rowCharOffsets lrw = $lastRowOccupiedWidth" }
                rowBreakOffsets.addToThisAscendingListWithoutDuplicate(rowCharOffsets)
                logL.d { "row break added ${rowBreakOffsets.size}" }
                lastOccupiedWidth = lastRowOccupiedWidth
                charStartIndexInBuffer = readRowUntilPos
            }
            if (charStartIndexInBuffer < nodeValue.renderBufferEndExclusive) {
//                val preserveIndexFrom = nodeValue.rowBreakOffsets.binarySearchForMinIndexOfValueAtLeast(endPosExclusive)
                val searchForValue = minOf(nodeValue.renderBufferEndExclusive, maxOf((rowBreakOffsets.lastOrNull() ?: -1) + 1, charStartIndexInBuffer))
                val preserveIndexFrom = nodeValue.rowBreakOffsets.binarySearchForMinIndexOfValueAtLeast(searchForValue)
                val preserveIndexTo = if (nodeStartPos + nodeValue.bufferLength >= length) {
                    nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(nodeValue.renderBufferEndExclusive) // keep the row after the last '\n'
                } else {
                    nodeValue.rowBreakOffsets.binarySearchForMaxIndexOfValueAtMost(nodeValue.renderBufferEndExclusive - 1)
                }
                logL.v { "reach the end, preserve RB from $preserveIndexFrom (at least $searchForValue) ~ $preserveIndexTo (${nodeValue.renderBufferEndExclusive}). RB = ${nodeValue.rowBreakOffsets}." }
                val restoreRowBreaks = nodeValue.rowBreakOffsets.subList(preserveIndexFrom, minOf(nodeValue.rowBreakOffsets.size, preserveIndexTo + 1))
                if (restoreRowBreaks.isNotEmpty() || nodeValue.isEndWithForceRowBreak || isBreakAfterThisIteration) {
                    rowBreakOffsets.addToThisAscendingListWithoutDuplicate(restoreRowBreaks)
                    logL.d { "row break restore end added ${rowBreakOffsets.size}" }
                    logL.d { "Restore lw ${nodeValue.lastRowWidth}." }
                    lastOccupiedWidth = nodeValue.lastRowWidth
                    isEndWithForceRowBreak = isEndWithForceRowBreak || nodeValue.isEndWithForceRowBreak
                }
            }
            logL.v { "node ${nodeValue.debugKey()} (${nodeStartPos} ..< ${nodeStartPos + nodeValue.renderBufferEndExclusive - nodeValue.renderBufferStart}) update lrw=$lastOccupiedWidth frb=$isEndWithForceRowBreak rb=$rowBreakOffsets" }
            nodeValue.rowBreakOffsets = rowBreakOffsets
            nodeValue.lastRowWidth = lastOccupiedWidth
            nodeValue.isEndWithForceRowBreak = isEndWithForceRowBreak
            isLastEndWithForceRowBreak = isEndWithForceRowBreak
            recomputeAggregatedValues(node) // TODO optimize
            logL.d { "after recomputeAggregatedValues" }

            if (isBreakOnEncounterLineBreak && isBreakAfterThisIteration) { // TODO it can be further optimized to break immediately on line break
                logL.d { "break" }
                break
            }

            node = tree.nextNode(node)?.also {
                logL.d { "node ${node!!.value.debugKey()} b#${node!!.value.bufferIndex} next ${it.value.debugKey()} b#${it!!.value.bufferIndex}" }
            }
            if (node != null) {
                nodeStartPos += nodeValue.currentRenderLength
                val nodeValue = node.value
                val buffer = nodeValue.buffer
                lineBreakIndexFrom = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(nodeValue.renderBufferStart)
                charStartIndexInBuffer = nodeValue.renderBufferStart
            }
        }

//        tree.visitInPostOrder {
//            recomputeAggregatedValues(it)
//        }

        logL.d { "before onLayoutCallback" }
        onLayoutCallback?.invoke()
        logL.d { "after onLayoutCallback" }
    }

    override val hasLayouted: Boolean
        get() = layouter != null && contentWidth != null

    override val numOfRows: Int
        get() = tree.getRoot().numRowBreaks() + 1 + // TODO cache the result
            run {
                val lastNode = tree.rightmost(tree.getRoot()).takeIf { it.isNotNil() }
                val lastValue = lastNode?.value ?: return@run 0
                val lastLineOffset = lastValue.buffer.lineOffsetStarts.let {
                    val lastIndex = it.binarySearchForMaxIndexOfValueAtMost(lastValue.renderBufferEndExclusive - 1)
                    if (lastIndex in 0 .. it.lastIndex) {
                        it[lastIndex]
                    } else {
                        null
                    }
                } ?: return@run 0
                val lastNodePos = findRenderPositionStart(lastNode)
                if (lastNodePos + (lastLineOffset - lastValue.renderBufferStart) == lastIndex) {
                    1 // one extra row if the string ends with '\n'
                } else {
                    0
                }
            }

    override val numOfLines: Int
        get() = tree.getRoot().numLineBreaks() + 1

    override val lastRowIndex: Int
        get() = numOfRows - 1

    override val numOfOriginalLines: Int
        get() = numOfLines

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

fun RedBlackTree<BigTextNodeValue>.Node.numLineBreaks(): Int {
    val value = getValue()
    return (value?.leftNumOfLineBreaks ?: 0) +
        (value?.renderNumLineBreaksInRange ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.numLineBreaks() ?: 0)
}

fun RedBlackTree<BigTextNodeValue>.Node.numRowBreaks(): Int {
    val value = getValue()
    return (value?.leftNumOfRowBreaks ?: 0) +
        (value?.rowBreakOffsets?.size ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.numRowBreaks() ?: 0)
}

fun RedBlackTree<BigTextNodeValue>.Node.computeLength(): Int {
    val value = getValue()
    return (value?.bufferLength ?: 0) +
        (getLeft().takeIf { it.isNotNil() }?.computeLength() ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.computeLength() ?: 0)
}

fun RedBlackTree<BigTextNodeValue>.Node.computeRenderLength(): Int {
    val value = getValue()
    return (value?.currentRenderLength ?: 0) +
        (getLeft().takeIf { it.isNotNil() }?.computeRenderLength() ?: 0) +
        (getRight().takeIf { it.isNotNil() }?.computeRenderLength() ?: 0)
}

private enum class InsertDirection {
    Left, Right, Undefined
}

fun BigText.Companion.createFromLargeString(initialContent: String) = BigTextImpl().apply {
    log.d { "createFromLargeString ${initialContent.length}" }
    append(initialContent)
}

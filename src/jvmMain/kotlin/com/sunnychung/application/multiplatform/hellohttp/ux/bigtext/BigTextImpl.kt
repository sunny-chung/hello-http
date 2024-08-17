package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
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
}, tag = "BigTextQuery")

internal var isD = false

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

    constructor() {
        chunkSize = 64
    }

    constructor(chunkSize: Int) {
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
                in it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange  until Int.MAX_VALUE -> 1.also { compareResult ->
                    val isTurnRight = compareResult > 0
                    if (isTurnRight) {
                        find -= it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange
                        lineStart += it.value.leftNumOfLineBreaks + it.value.bufferNumLineBreaksInRange
                    }
                }
                else -> throw IllegalStateException("what is find? $find")
            }
        }?.let { it to /*lineStart *//*+ it.value.leftNumOfLineBreaks*/ findLineStart(it) }
    }

    fun findPositionStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
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

    fun findLineStart(node: RedBlackTree<BigTextNodeValue>.Node): Int {
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

        log.v { inspect("Finish I " + node?.value?.debugKey()) }
    }

    fun computeCurrentNodeProperties(nodeValue: BigTextNodeValue) = with (nodeValue) {
//        bufferNumLineBreaksInRange = buffers[bufferIndex].lineOffsetStarts.subSet(bufferOffsetStart, bufferOffsetEndExclusive).size
        bufferNumLineBreaksInRange = buffers[bufferIndex].lineOffsetStarts.run {
            binarySearchForMinIndexOfValueAtLeast(bufferOffsetEndExclusive - 1) + 1 - maxOf(0, binarySearchForMinIndexOfValueAtLeast(bufferOffsetStart))
        }
        leftNumOfLineBreaks = node?.left?.numLineBreaks() ?: 0
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

    override fun fullString(): String {
        return tree.joinToString("") {
            buffers[it.bufferIndex].subSequence(it.bufferOffsetStart, it.bufferOffsetEndExclusive)
        }
    }

    override fun substring(start: Int, endExclusive: Int): String { // O(lg L + (e - s))
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive is out of bound" }

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
        fun findCharPosOfLineOffset(node: RedBlackTree<BigTextNodeValue>.Node, lineOffset: Int): Int {
            val buffer = buffers[node.value!!.bufferIndex]
            val lineStartIndexInBuffer = buffer.lineOffsetStarts.binarySearchForMinIndexOfValueAtLeast(node.value!!.bufferOffsetStart)
            val offsetedLineOffset = maxOf(0, lineStartIndexInBuffer) + (lineOffset)
            val charOffsetInBuffer = if (offsetedLineOffset >= 0) {
                buffer.lineOffsetStarts[offsetedLineOffset] + 1
            } else {
                0
            }
            return findPositionStart(node) + (charOffsetInBuffer - node.value!!.bufferOffsetStart)
        }

        val (startNode, startNodeLineStart) = tree.findNodeByLineBreaks(lineIndex - 1)!!
        val endNodeFindPair = tree.findNodeByLineBreaks(lineIndex)
        val endCharIndex = if (endNodeFindPair != null) { // includes the last '\n' char
            val (endNode, endNodeLineStart) = endNodeFindPair
            require(endNodeLineStart <= lineIndex)
//            val lca = tree.lowestCommonAncestor(startNode, endNode)
            findCharPosOfLineOffset(endNode, lineIndex - endNodeLineStart)
        } else {
            length
        }
        val startCharIndex = findCharPosOfLineOffset(startNode, lineIndex - 1 - startNodeLineStart)
        logQ.d { "line #$lineIndex -> $startCharIndex ..< $endCharIndex" }
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

        log.d { inspect("Finish D " + node?.value?.debugKey()) }

        return -(endExclusive - start)
    }

    fun charIndexRangeOfNode(node: RedBlackTree<BigTextNodeValue>.Node): IntRange {
        val start = findPositionStart(node)
        return start until start + node.value.bufferLength
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    fun inspect(label: String = "") = buildString {
        appendLine("[$label] Buffer:\n${buffers.mapIndexed { i, it -> "    $i:\t$it\n" }.joinToString("")}")
        appendLine("[$label] Tree:\nflowchart TD\n${tree.debugTree()}")
        appendLine("[$label] String:\n${fullString()}")
    }

    fun printDebug(label: String = "") {
        println(inspect(label))
    }


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

private enum class InsertDirection {
    Left, Right, Undefined
}

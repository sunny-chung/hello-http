package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.williamfiset.algorithms.datastructures.balancedtree.RedBlackTree

val log = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigText")

var isD = false

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

    internal constructor(chunkSize: Int) {
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

    private fun appendChunk(chunkedString: String) {
        insertChunkAtPosition(tree.root.length(), chunkedString)
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
            buffer = TextBuffer()
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

        log.d { inspect("Finish I " + node?.value?.debugKey()) }
    }

    fun recomputeAggregatedValues(node: RedBlackTree<BigTextNodeValue>.Node) {
        log.d { inspect("${node.value?.debugKey()} start") }

        var node = node
        while (node.isNotNil()) {
            val left = node.left.takeIf { it.isNotNil() }
            with (node.getValue()) {
                leftStringLength = left?.length() ?: 0
                log.d { ">> ${node.value.debugKey()} -> $leftStringLength (${left?.value?.debugKey()}/ ${left?.length()})" }
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
        get() = tree.root.length()

    override fun fullString(): String {
        return tree.joinToString("") {
            buffers[it.bufferIndex].toString().substring(it.bufferOffsetStart, it.bufferOffsetEndExclusive)
        }
    }

    override fun substring(start: Int, endExclusive: Int): String {
        TODO("Not yet implemented")
    }

    override fun append(text: String) {
        insertAt(length, text)
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

    override fun insertAt(pos: Int, text: String) {
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
    }

    override fun delete(start: Int, endExclusive: Int) {
        require(start <= endExclusive) { "start should be <= endExclusive" }
        require(0 <= start) { "Invalid start" }
        require(endExclusive <= length) { "endExclusive is out of bound" }

        if (start == endExclusive) {
            return
        }

        log.d { "delete $start ..< $endExclusive" }

        var node: RedBlackTree<BigTextNodeValue>.Node? = tree.findNodeByCharIndex(endExclusive - 1)
        var nodeRange = charIndexRangeOfNode(node!!)
        val newNodesInDescendingOrder = mutableListOf<BigTextNodeValue>()
        while (node?.isNotNil() == true && start <= nodeRange.endInclusive) {
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
//            if (!tree.delete(node.value)) {
//                throw IllegalStateException("Cannot delete node ${node.value.debugKey()} at ${nodeRange.start} .. ${nodeRange.last}")
//            }
            node = prev
//            nodeRange = nodeRange.start - chunkSize .. nodeRange.last - chunkSize
            if (node != null) {
                nodeRange = charIndexRangeOfNode(node) // TODO optimize by calculation instead of querying
            }
        }

        newNodesInDescendingOrder.asReversed().forEach {
            if (node != null) {
                node = tree.insertRight(node!!, it)
            } else {
                node = tree.insertValue(it)
            }
        }

        log.v { inspect("Finish D " + node?.value?.debugKey()) }
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

private enum class InsertDirection {
    Left, Right, Undefined
}

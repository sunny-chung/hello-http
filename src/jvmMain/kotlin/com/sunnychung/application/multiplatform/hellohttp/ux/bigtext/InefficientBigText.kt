package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.insert
import com.sunnychung.application.multiplatform.hellohttp.util.string

class InefficientBigText(text: String) : BigText {
    private var string: String = text

    override val length: Int
        get() = string.length

    override val lastIndex: Int
        get() = TODO("Not yet implemented")
    override val isEmpty: Boolean
        get() = TODO("Not yet implemented")
    override val isNotEmpty: Boolean
        get() = TODO("Not yet implemented")
    override val hasLayouted: Boolean
        get() = TODO("Not yet implemented")
    override val layouter: TextLayouter?
        get() = TODO("Not yet implemented")
    override val numOfLines: Int
        get() = TODO("Not yet implemented")
    override val numOfRows: Int
        get() = TODO("Not yet implemented")
    override val lastRowIndex: Int
        get() = TODO("Not yet implemented")
    override val numOfOriginalLines: Int
        get() = TODO("Not yet implemented")
    override val chunkSize: Int
        get() = TODO("Not yet implemented")
    override val undoHistoryCapacity: Int
        get() = TODO("Not yet implemented")
    override val textBufferFactory: (capacity: Int) -> TextBuffer
        get() = TODO("Not yet implemented")
    override val charSequenceBuilderFactory: (capacity: Int) -> Appendable
        get() = TODO("Not yet implemented")
    override val charSequenceFactory: (Appendable) -> CharSequence
        get() = TODO("Not yet implemented")
    override val tree: LengthTree<BigTextNodeValue>
        get() = TODO("Not yet implemented")
    override val contentWidth: Float?
        get() = TODO("Not yet implemented")
    override var undoMetadataSupplier: (() -> Any?)?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var changeHook: BigTextChangeHook?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun buildString(): String = string

    override fun buildCharSequence(): CharSequence = string

    override fun substring(start: Int, endExclusive: Int): CharSequence =
        string.substring(start, endExclusive)

    override fun substring(range: IntRange): CharSequence =
        substring(range.first, range.last)

    override fun findLineString(lineIndex: Int): CharSequence {
        TODO("Not yet implemented")
    }

    override fun findRowString(rowIndex: Int): CharSequence {
        TODO("Not yet implemented")
    }

    override fun append(text: CharSequence): Int {
        string += text
        return text.length
    }

    override fun insertAt(pos: Int, text: CharSequence): Int {
        string = string.insert(pos, text.string())
        return text.length
    }

    override fun delete(start: Int, endExclusive: Int): Int {
        string = string.removeRange(start, endExclusive)
        return -(endExclusive - start)
    }

    override fun recordCurrentChangeSequenceIntoUndoHistory() {
        TODO("Not yet implemented")
    }

    override fun undo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> {
        TODO("Not yet implemented")
    }

    override fun redo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> {
        TODO("Not yet implemented")
    }

    override fun isUndoable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isRedoable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun findLineAndColumnFromRenderPosition(renderPosition: Int): Pair<Int, Int> {
        TODO("Not yet implemented")
    }

    override fun findRenderCharIndexByLineAndColumn(lineIndex: Int, columnIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun findPositionStartOfLine(lineIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun findLineIndexByRowIndex(rowIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun findFirstRowIndexOfLine(lineIndex: Int): Int {
        TODO("Not yet implemented")
    }

    override fun setLayouter(layouter: TextLayouter) {
        TODO("Not yet implemented")
    }

    override fun setContentWidth(contentWidth: Float) {
        TODO("Not yet implemented")
    }

    override fun layout() {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int =
        string.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is BigText) {
            return false
        }
        return when(other) {
            is InefficientBigText -> string == other.buildString()
            else -> TODO()
        }
    }

    override fun inspect(label: String): String {
        TODO("Not yet implemented")
    }

    override fun printDebug(label: String) {
        TODO("Not yet implemented")
    }
}

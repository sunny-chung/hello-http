package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

open class ConcurrentBigText(open val delegate: BigText) : BigText {

    val lock = ReentrantReadWriteLock()

    override val length: Int
        get() = lock.read { delegate.length }
    override val lastIndex: Int
        get() = lock.read { delegate.lastIndex }
    override val isEmpty: Boolean
        get() = lock.read { delegate.isEmpty }
    override val isNotEmpty: Boolean
        get() = lock.read { delegate.isNotEmpty }
    override val hasLayouted: Boolean
        get() = lock.read { delegate.hasLayouted }
    override val layouter: TextLayouter?
        get() = lock.read { delegate.layouter }
    override val numOfLines: Int
        get() = lock.read { delegate.numOfLines }
    override val numOfRows: Int
        get() = lock.read { delegate.numOfRows }
    override val lastRowIndex: Int
        get() = lock.read { delegate.lastRowIndex }
    override val numOfOriginalLines: Int
        get() = lock.read { delegate.numOfOriginalLines }
    override val chunkSize: Int
        get() = lock.read { delegate.chunkSize }
    override val undoHistoryCapacity: Int
        get() = lock.read { delegate.undoHistoryCapacity }
    override val textBufferFactory: (capacity: Int) -> TextBuffer
        get() = lock.read { delegate.textBufferFactory }
    override val charSequenceBuilderFactory: (capacity: Int) -> Appendable
        get() = lock.read { delegate.charSequenceBuilderFactory }
    override val charSequenceFactory: (Appendable) -> CharSequence
        get() = lock.read { delegate.charSequenceFactory }
    override val tree: LengthTree<BigTextNodeValue>
        get() = lock.read { delegate.tree }
    override val contentWidth: Float?
        get() = lock.read { delegate.contentWidth }
    override var decorator: BigTextDecorator?
        get() = lock.read { delegate.decorator }
        set(value) { lock.write { delegate.decorator = value } }
    override var undoMetadataSupplier: (() -> Any?)?
        get() = lock.read { delegate.undoMetadataSupplier }
        set(value) { lock.write { delegate.undoMetadataSupplier = value } }
    override var changeHook: BigTextChangeHook?
        get() = lock.read { delegate.changeHook }
        set(value) { lock.write { delegate.changeHook = value } }

    override val isThreadSafe: Boolean
        get() = true

    override fun buildString(): String = lock.read { delegate.buildString() }

    override fun buildCharSequence(): CharSequence = lock.read { delegate.buildCharSequence() }

    override fun substring(start: Int, endExclusive: Int): CharSequence = lock.read { delegate.substring(start, endExclusive) }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = lock.read { delegate.subSequence(startIndex, endIndex) }
    override fun chunkAt(start: Int): String = lock.read { delegate.chunkAt(start) }

    override fun findLineString(lineIndex: Int): CharSequence = lock.read { delegate.findLineString(lineIndex) }

    override fun findRowString(rowIndex: Int): CharSequence = lock.read { delegate.findRowString(rowIndex) }

    override fun append(text: CharSequence): Int = lock.write { delegate.append(text) }

    override fun insertAt(pos: Int, text: CharSequence): Int = lock.write { delegate.insertAt(pos, text) }

    override fun delete(start: Int, endExclusive: Int): Int = lock.write { delegate.delete(start, endExclusive) }

    override fun replace(start: Int, endExclusive: Int, text: CharSequence) = lock.write {
        delegate.replace(start, endExclusive, text)
    }

    override fun replace(range: IntRange, text: CharSequence) = lock.write {
        delegate.replace(range, text)
    }

    override fun recordCurrentChangeSequenceIntoUndoHistory() = lock.write { delegate.recordCurrentChangeSequenceIntoUndoHistory() }

    override fun undo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> = lock.write { delegate.undo(callback) }

    override fun redo(callback: BigTextChangeCallback?): Pair<Boolean, Any?> = lock.write { delegate.redo(callback) }

    override fun isUndoable(): Boolean = lock.read { delegate.isUndoable() }

    override fun isRedoable(): Boolean = lock.read { delegate.isRedoable() }

    override fun findLineAndColumnFromRenderPosition(renderPosition: Int): Pair<Int, Int> = lock.read { delegate.findLineAndColumnFromRenderPosition(renderPosition) }

    override fun findRenderCharIndexByLineAndColumn(lineIndex: Int, columnIndex: Int): Int = lock.read { delegate.findRenderCharIndexByLineAndColumn(lineIndex, columnIndex) }

    override fun findPositionStartOfLine(lineIndex: Int): Int = lock.read { delegate.findPositionStartOfLine(lineIndex) }

    override fun findLineIndexByRowIndex(rowIndex: Int): Int = lock.read { delegate.findLineIndexByRowIndex(rowIndex) }

    override fun findFirstRowIndexOfLine(lineIndex: Int): Int = lock.read { delegate.findFirstRowIndexOfLine(lineIndex) }

    override fun setLayouter(layouter: TextLayouter) = lock.write { delegate.setLayouter(layouter) }

    override fun setContentWidth(contentWidth: Float) = lock.write { delegate.setContentWidth(contentWidth) }

    override fun layout() = lock.write { delegate.layout() }

    // the first call to `hashCode()` would write to cache
//    override fun hashCode(): Int = lock.write { delegate.hashCode() }
    // currently, BigTextImpl has no custom implementation over built-in's one, so no lock is needed.
    override fun hashCode(): Int = delegate.hashCode()

//    override fun equals(other: Any?): Boolean = lock.read { delegate.equals(other) }
    // currently, BigTextImpl has no custom implementation over built-in's one, so no lock is needed.
    override fun equals(other: Any?): Boolean {
        if (other !is ConcurrentBigText) return delegate.equals(other)
        return delegate.equals(other.delegate)
    }

    override fun inspect(label: String): String = lock.read { delegate.inspect(label) }

    override fun printDebug(label: String) = lock.read { delegate.printDebug(label) }

    fun withWriteLock(operation: (BigText) -> Unit) = lock.write { operation(delegate) }

    fun withReadLock(operation: (BigText) -> Unit) = lock.read { operation(delegate) }

    inline fun tryReadLock(operation: (BigText) -> Unit) {
        val isLocked = lock.readLock().tryLock()
        if (isLocked) {
            try {
                operation(delegate)
            } finally {
                lock.readLock().unlock()
            }
        }
    }

}

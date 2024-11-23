package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

import kotlin.concurrent.read
import kotlin.concurrent.write

class ConcurrentBigTextTransformed(override val delegate: BigTextTransformed) : BigTextTransformed, ConcurrentBigText(delegate) {

    init {
        delegate.originalText.layouter?.let { setLayouter(it) }
        delegate.originalText.contentWidth?.let { setContentWidth(it) }
        delegate.originalText.changeHook = object : BigTextChangeHook {
            override fun afterInsertChunk(modifiedText: BigText, position: Int, newValue: BigTextNodeValue) {
                insertOriginal(position, newValue)
            }
            override fun afterDelete(modifiedText: BigText, position: IntRange) {
                deleteOriginal(position)
            }
        }
    }

    override val originalText: BigText
        get() = lock.read { delegate.originalText }

    override val originalLength: Int
        get() = lock.read { delegate.originalLength }

    override fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int
        = lock.read { delegate.findTransformedPositionByOriginalPosition(originalPosition) }

    override fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int
        = lock.read { delegate.findOriginalPositionByTransformedPosition(transformedPosition) }

    override fun findFirstRowIndexByOriginalLineIndex(originalLineIndex: Int): Int
        = lock.read { delegate.findFirstRowIndexByOriginalLineIndex(originalLineIndex) }

    override fun findOriginalLineIndexByRowIndex(rowIndex: Int): Int
        = lock.read { delegate.findOriginalLineIndexByRowIndex(rowIndex) }

    override fun requestReapplyTransformation(originalRange: IntRange)
        = lock.write { delegate.requestReapplyTransformation(originalRange) }

    override fun insertOriginal(
        pos: Int,
        nodeValue: BigTextNodeValue,
        bufferOffsetStart: Int,
        bufferOffsetEndExclusive: Int
    ) = lock.write { delegate.insertOriginal(pos, nodeValue, bufferOffsetStart, bufferOffsetEndExclusive) }

    override fun deleteOriginal(originalRange: IntRange, isReMapPositionNeeded: Boolean)
        = lock.write { delegate.deleteOriginal(originalRange, isReMapPositionNeeded) }

    override fun replace(range: IntRange, text: CharSequence, offsetMapping: BigTextTransformOffsetMapping)
        = lock.write { delegate.replace(range, text, offsetMapping) }

    override fun restoreToOriginal(range: IntRange)
        = lock.write { delegate.restoreToOriginal(range) }

    override var onLayoutCallback: (() -> Unit)?
        get() = lock.read { delegate.onLayoutCallback }
        set(value) { lock.write { delegate.onLayoutCallback = value } }

    override fun findRowPositionStartIndexByRowIndex(index: Int): Int
        = lock.read { delegate.findRowPositionStartIndexByRowIndex(index) }

    override fun findRowIndexByPosition(position: Int): Int
        = lock.read { delegate.findRowIndexByPosition(position) }

    override fun findPositionByRowIndex(index: Int): Int
        = lock.read { delegate.findPositionByRowIndex(index) }
}

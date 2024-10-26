package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

interface BigTextLayoutable {

    val hasLayouted: Boolean

    val length: Int

    val numOfLines: Int

    val numOfOriginalLines: Int

    val numOfRows: Int

    val lastRowIndex: Int

    var onLayoutCallback: (() -> Unit)?

    fun setLayouter(layouter: TextLayouter)

    fun setContentWidth(contentWidth: Float)

    fun findRowPositionStartIndexByRowIndex(index: Int): Int

    fun findLineIndexByRowIndex(rowIndex: Int): Int

    fun findRowString(rowIndex: Int): CharSequence

    fun findRowIndexByPosition(position: Int): Int

    fun findPositionByRowIndex(index: Int): Int
}

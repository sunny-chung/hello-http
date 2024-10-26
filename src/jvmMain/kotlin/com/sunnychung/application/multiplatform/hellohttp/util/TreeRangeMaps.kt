package com.sunnychung.application.multiplatform.hellohttp.util

import com.google.common.collect.Range
import com.google.common.collect.TreeRangeMap

object TreeRangeMaps {

    fun from(ranges: Iterable<IntRange>): TreeRangeMap<Int, Int> {
        val tree = TreeRangeMap.create<Int, Int>()
        ranges.forEachIndexed { i, it ->
            tree.put(Range.closed(it.start, it.endInclusive), i)
        }
        return tree
    }
}
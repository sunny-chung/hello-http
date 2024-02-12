package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.CollapseTransformationOffsetMapping
import kotlin.test.Test
import kotlin.test.assertEquals

class CollapseTransformationOffsetMappingTest {
    /**
     *                          0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
     *                          a b c { d e f g h i j  }  k  l  {  m  n  }  o  p
     * collapsedCharRanges            [3, 11]                  [14, 17]
     * transformedText          a b c {   . . .   } k  l  {     .  .  .     }  o  p
     * originalToTransformed    0 1 2 3 9 9 9 9 9 9 9  9  10 11 12 18 18 18 19 20
     * transformedToOriginal    0 1 2 3 11       11 12 13 14 17             17 18 19
     */
    @Test
    fun test() {
        val mapping = CollapseTransformationOffsetMapping(
            listOf(3..11, 14..17)
        )
        assertEquals(
            listOf(0, 1, 2, 3, 9, 9, 9, 9, 9, 9, 9, 9, 10, 11, 12, 18, 18, 18, 19, 20),
            (0..19).map { mapping.originalToTransformed(it) }
        )
        assertEquals(
            listOf(0, 1, 2, 3, 11, 11, 11, 11, 11, 11, 12, 13, 14, 17, 17, 17, 17, 17, 17, 18, 19),
            (0..20).map { mapping.transformedToOriginal(it) }
        )
    }
}

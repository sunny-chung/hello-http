package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformationOffsetMapping
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentVariableTransformationOffsetMappingTest {
    /**
     *                        0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
     *                        a b c $ { { v a r } }  d  e  $  {  {  v  }  }  x  y
     * variableRanges               [3, 10        ]        [13, 18        ]
     * originalToTransformed  0 1 2 3 3 3 3 4 5 5 5  6  7  8  8  8  8  8  8  9  10
     * transformedToOriginal  0 1 2 6 6 6 6 7 8 8 8  11 12 16 16 16 16 16 16 19 20
     */
    @Test
    fun test() {
        val mapping = EnvironmentVariableTransformationOffsetMapping(TreeMap(mapOf(
            3 to 3..10,
            13 to 13..18
        )))
        assertEquals(
            listOf(0, 1, 2, 3, 3, 3, 3, 4, 5, 5, 5, 6, 7, 8, 8, 8, 8, 8, 8, 9, 10),
            (0..20).map { mapping.originalToTransformed(it) }
        )
        assertEquals(
            listOf(0, 1, 2, 6, 7, 8, 11, 12, 16, 19, 20),
            (0..10).map { mapping.transformedToOriginal(it) }
        )
    }
}
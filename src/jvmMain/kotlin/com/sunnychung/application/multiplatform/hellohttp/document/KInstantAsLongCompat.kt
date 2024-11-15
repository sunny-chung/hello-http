package com.sunnychung.application.multiplatform.hellohttp.document

import com.sunnychung.lib.multiplatform.kdatetime.KInstant

/**
 * This type exists because there is a bug in Kotlin 1.8.0 that
 * KInstantAsLong was serialized as KInstant.
 *
 * The data was widely serialized before it was fixed in later Kotlin versions.
 * This class intends to "keep" the bug effective, so that old data can still be decoded correctly.
 */
typealias KInstantAsLongCompat = KInstant

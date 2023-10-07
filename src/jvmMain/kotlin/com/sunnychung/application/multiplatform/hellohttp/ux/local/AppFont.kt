package com.sunnychung.application.multiplatform.hellohttp.ux.local

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class AppFont(
    val bodyFontSize: TextUnit,
    val buttonFontSize: TextUnit,

    val createLabelSize: TextUnit,
    val largeInfoSize: TextUnit,
)

val LocalFont = compositionLocalOf { regularFont() }

internal fun regularFont() = AppFont(
    bodyFontSize = 14.sp,
    buttonFontSize = 16.sp,

    createLabelSize = 20.sp,
    largeInfoSize = 29.sp,
)

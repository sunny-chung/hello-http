package com.sunnychung.application.multiplatform.hellohttp.ux.local

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class AppFont(
    val bodyFontSize: TextUnit,
    val buttonFontSize: TextUnit,
    val searchInputSize: TextUnit,
    val supplementSize: TextUnit,
    val codeEditorBodyFontSize: TextUnit,
    val codeEditorLineNumberFontSize: TextUnit,
    val transportTimelineBodyFontSize: TextUnit = codeEditorBodyFontSize,

    val createLabelSize: TextUnit,
    val largeInfoSize: TextUnit,
)

val LocalFont = compositionLocalOf { regularFont() }

internal fun regularFont() = AppFont(
    bodyFontSize = 14.sp,
    buttonFontSize = 16.sp,
    searchInputSize = 12.sp,
    supplementSize = 11.sp,
    codeEditorBodyFontSize = 13.sp,
    codeEditorLineNumberFontSize = 12.sp,

    createLabelSize = 20.sp,
    largeInfoSize = 29.sp,
)

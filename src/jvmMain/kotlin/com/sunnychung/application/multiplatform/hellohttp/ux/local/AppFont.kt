package com.sunnychung.application.multiplatform.hellohttp.ux.local

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.manager.AppRes

data class AppFont(
    val bodyFontSize: TextUnit,
    val buttonFontSize: TextUnit,
    val searchInputSize: TextUnit,
    val supplementSize: TextUnit,
    val badgeFontSize: TextUnit,
    val codeEditorBodyFontSize: TextUnit,
    val codeEditorLineNumberFontSize: TextUnit,
    val streamFontSize: TextUnit = codeEditorBodyFontSize,
    val transportTimelineBodyFontSize: TextUnit = codeEditorBodyFontSize,

    val createLabelSize: TextUnit,
    val largeInfoSize: TextUnit,

    val contextMenuFontSize: TextUnit,

    val normalFontFamily: FontFamily,
    val monospaceFontFamily: FontFamily,
)

val LocalFont = compositionLocalOf { regularFont() }

internal fun regularFont() = AppFont(
    bodyFontSize = 14.sp,
    buttonFontSize = 16.sp,
    searchInputSize = 12.sp,
    supplementSize = 11.sp,
    badgeFontSize = 9.sp,
    codeEditorBodyFontSize = 13.sp,
    codeEditorLineNumberFontSize = 11.sp,

    createLabelSize = 20.sp,
    largeInfoSize = 29.sp,

    contextMenuFontSize = 13.sp,

    normalFontFamily = FontFamily(
        Font(
            identity = "Comme-Light",
            getData = { AppContext.ResourceManager.getResource(AppRes.Font.CommeLight) },
            weight = FontWeight.Normal,
        ),
        Font(
            identity = "Comme-CommeRegular",
            getData = { AppContext.ResourceManager.getResource(AppRes.Font.CommeRegular) },
            weight = FontWeight.SemiBold,
        ),
        Font(
            identity = "Comme-SemiBold",
            getData = { AppContext.ResourceManager.getResource(AppRes.Font.CommeSemiBold) },
            weight = FontWeight.Bold,
        ),
    ),
    monospaceFontFamily = FontFamily(
        Font(
            identity = "PitagonSansMono-Regular",
            getData = { AppContext.ResourceManager.getResource(AppRes.Font.PitagonSansMonoRegular) },
            weight = FontWeight.Normal,
        ),
        Font(
            identity = "PitagonSansMono-Bold",
            getData = { AppContext.ResourceManager.getResource(AppRes.Font.PitagonSansMonoBold) },
            weight = FontWeight.Bold,
        )
    ),
)

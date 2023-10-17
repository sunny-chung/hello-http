package com.sunnychung.application.multiplatform.hellohttp.ux.local

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColor(
    val background: Color,
    val backgroundLight: Color,
    val backgroundSemiLight: Color,
    val backgroundContextMenu: Color,
    val backgroundInputField: Color,
    val backgroundButton: Color,
    val backgroundDialogOverlay: Color,
    val backgroundTooltip: Color,
    val backgroundHoverDroppable: Color,

    val primary: Color,
    val bright: Color,
    val text: Color = primary,
    val image: Color = primary,
    val line: Color,

    val placeholder: Color,
    val disabled: Color = placeholder,
    val highlight: Color,
    val cursor: Color,
    val warning: Color,

    val httpRequestGet: Color,
    val httpRequestPost: Color,
    val httpRequestPut: Color,
    val httpRequestDelete: Color,
    val httpRequestOthers: Color,

    val grpcRequest: Color,
    val graphqlRequest: Color,

    val pendingResponseBackground: Color,
    val successfulResponseBackground: Color,
    val errorResponseBackground: Color,

    val scrollBarUnhover: Color,
    val scrollBarHover: Color,
)

val LocalColor = compositionLocalOf { darkColorScheme() }

fun darkColorScheme(): AppColor = AppColor(
    background = Color(red = 0.1f, green = 0.1f, blue = 0.1f),
    backgroundLight = Color(red = 0.2f, green = 0.2f, blue = 0.2f),
    backgroundSemiLight = Color(red = 0.15f, green = 0.15f, blue = 0.15f),
    backgroundContextMenu = Color(red = 0.3f, green = 0.3f, blue = 0.3f),
    backgroundInputField = Color.Black,
    backgroundButton = Color(red = 0f, green = 0f, blue = 0.6f),
    backgroundDialogOverlay = Color.Black.copy(alpha = 0.6f),
    backgroundTooltip = Color(red = 0f, green = 0f, blue = 0.45f),
    backgroundHoverDroppable = Color(0f, 0f, 0.6f),

    primary = Color(red = 0.8f, green = 0.8f, blue = 1.0f),
    bright = Color.White,
    line = Color(red = 0.6f, green = 0.6f, blue = 0.6f),

    placeholder = Color(red = 0.5f, green = 0.5f, blue = 0.5f),
    highlight = Color(red = 0.9f, green = 0.9f, blue = 0.5f),
    cursor = Color(red = 1f, green = 1f, blue = 1f),
    warning = Color(red = 1f, green = 0.5f, blue = 0f),

    httpRequestGet = Color(red = 0f, green = 0.8f, blue = 0f),
    httpRequestPost = Color(red = 0.3f, green = 0.3f, blue = 0.8f),
    httpRequestPut = Color(red = 1f, green = 0.5f, blue = 0f),
    httpRequestDelete = Color(red = 0.9f, green = 0.3f, blue = 0.3f),
    httpRequestOthers = Color(red = 0.5f, green = 0.5f, blue = 0.5f),

    grpcRequest = Color(red = 1f, green = 1f, blue = 1f),
    graphqlRequest = Color(red = 204, green = 67, blue = 162),

    pendingResponseBackground = Color(red = 0.4f, green = 0.4f, blue = 0.4f),
    successfulResponseBackground = Color(red = 0.1f, green = 0.6f, blue = 0.1f),
    errorResponseBackground = Color(red = 1f, green = 0.2f, blue = 0.2f),

    scrollBarUnhover = Color(red = 0.7f, green = 0.7f, blue = 0.7f).copy(alpha = 0.25f),
    scrollBarHover = Color(red = 0.7f, green = 0.7f, blue = 0.7f).copy(alpha = 0.50f),
)

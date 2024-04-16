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
    val backgroundStopButton: Color,
    val backgroundDialogOverlay: Color,
    val backgroundTooltip: Color,
    val backgroundHoverDroppable: Color,
    val backgroundInputFieldHighlight: Color,
    val backgroundInputFieldHighlightEmphasize: Color,
    val backgroundCollapsed: Color,
    val backgroundFloatingButton: Color = background.copy(alpha = 0.7f),

    val primary: Color,
    val bright: Color,
    val successful: Color,
    val text: Color = primary,
    val unimportant: Color,
    val image: Color = primary,
    val line: Color,

    val placeholder: Color,
    val disabled: Color,
    val highlight: Color,
    val cursor: Color,
    val warning: Color,

    val httpRequestGet: Color,
    val httpRequestPost: Color,
    val httpRequestPut: Color,
    val httpRequestPatch: Color,
    val httpRequestDelete: Color,
    val httpRequestOthers: Color,

    val websocketRequest: Color,

    val grpcRequest: Color,
    val graphqlRequest: Color,

    val variableTextColor: Color,
    val variableBackgroundColor: Color,
    val variableErrorBackgroundColor: Color,

    val functionTextColor: Color,
    val functionBackgroundColor: Color,

    val pendingResponseBackground: Color,
    val successfulResponseBackground: Color,
    val errorResponseBackground: Color,

    val scrollBarUnhover: Color,
    val scrollBarHover: Color,

    val copyButton: Color = image.copy(alpha = 0.8f),

    val syntaxColor: SyntaxColor,
)

data class SyntaxColor(
    // common of json + graphql query
    val stringLiteral: Color,
    val numberLiteral: Color,
    val booleanTrueLiteral: Color,
    val booleanFalseLiteral: Color,
    val nothingLiteral: Color,
    val objectKey: Color,

    val comment: Color,
    val keyword: Color,
    val type: Color,
    val variable: Color,
    val field: Color,
    val directive: Color,
    val otherName: Color, // graphql operation name or fragment name
)

val LocalColor = compositionLocalOf { darkColorScheme() }

fun darkColorScheme(): AppColor = AppColor(
    background = Color(red = 0.1f, green = 0.1f, blue = 0.1f),
    backgroundLight = Color(red = 0.2f, green = 0.2f, blue = 0.2f),
    backgroundSemiLight = Color(red = 0.15f, green = 0.15f, blue = 0.15f),
    backgroundContextMenu = Color(red = 0.3f, green = 0.3f, blue = 0.3f),
    backgroundInputField = Color.Black,
    backgroundButton = Color(red = 0f, green = 0f, blue = 0.6f),
    backgroundStopButton = Color(red = 0.6f, green = 0f, blue = 0f),
    backgroundDialogOverlay = Color.Black.copy(alpha = 0.6f),
    backgroundTooltip = Color(red = 0f, green = 0f, blue = 0.45f),
    backgroundHoverDroppable = Color(0f, 0f, 0.6f),
    backgroundInputFieldHighlight = Color(red = 0.3f, green = 0.3f, blue = 0.78f),
    backgroundInputFieldHighlightEmphasize = Color(red = 0.6f, green = 0.38f, blue = 0f),
    backgroundCollapsed = Color(red = 0.4f, green = 0.4f, blue = 0.4f),

    primary = Color(red = 0.8f, green = 0.8f, blue = 1.0f),
    unimportant = Color(red = 0.45f, green = 0.45f, blue = 0.65f),
    bright = Color.White,
    successful = Color(red = 0.1f, green = 0.8f, blue = 0.1f),
    line = Color(red = 0.6f, green = 0.6f, blue = 0.6f),

    placeholder = Color(red = 0.5f, green = 0.5f, blue = 0.5f),
    disabled = Color(red = 0.3f, green = 0.3f, blue = 0.6f),
    highlight = Color(red = 0.9f, green = 0.9f, blue = 0.5f),
    cursor = Color(red = 1f, green = 1f, blue = 1f),
    warning = Color(red = 1f, green = 0.5f, blue = 0f),

    httpRequestGet = Color(red = 0f, green = 0.8f, blue = 0f),
    httpRequestPost = Color(red = 0.3f, green = 0.3f, blue = 0.8f),
    httpRequestPut = Color(red = 1f, green = 0.6f, blue = 0f),
    httpRequestPatch = Color(red = 0.93f, green = 0.45f, blue = 0.2f),
    httpRequestDelete = Color(red = 0.9f, green = 0.3f, blue = 0.3f),
    httpRequestOthers = Color(red = 0.5f, green = 0.5f, blue = 0.5f),

    websocketRequest = Color(red = 0.6f, green = 0.6f, blue = 0.9f),

    grpcRequest = Color(red = 0.9f, green = 0.9f, blue = 0.9f),
    graphqlRequest = Color(red = 204, green = 67, blue = 162),

    variableTextColor = Color.Yellow,
    variableBackgroundColor = Color(red = 0.3f, green = 0.3f, blue = 0.8f),
    variableErrorBackgroundColor = Color(red = 0.6f, green = 0.2f, blue = 0.3f),

    functionTextColor = Color.White,
    functionBackgroundColor = Color(red = 0.1f, green = 0.4f, blue = 0.1f),

    pendingResponseBackground = Color(red = 0.4f, green = 0.4f, blue = 0.4f),
    successfulResponseBackground = Color(red = 0.1f, green = 0.6f, blue = 0.1f),
    errorResponseBackground = Color(red = 1f, green = 0.2f, blue = 0.2f),

    scrollBarUnhover = Color(red = 0.7f, green = 0.7f, blue = 0.7f).copy(alpha = 0.25f),
    scrollBarHover = Color(red = 0.7f, green = 0.7f, blue = 0.7f).copy(alpha = 0.50f),

    syntaxColor = SyntaxColor(
        stringLiteral = Color(red = 0.8f, green = 0.8f, blue = 0.2f),
        numberLiteral = Color(red = 0.4f, green = 0.4f, blue = 0.9f),
        booleanTrueLiteral = Color(red = 0.2f, green = 0.8f, blue = 0.2f),
        booleanFalseLiteral = Color(red = 0.9f, green = 0.4f, blue = 0.4f),
        nothingLiteral = Color(red = 0.6f, green = 0.6f, blue = 0.6f),
        objectKey = Color(red = 0.5f, green = 0.7f, blue = 1f),

        comment = Color(red = 0.35f, green = 0.35f, blue = 0.35f),
        keyword = Color(red = 0.88f, green = 0.34f, blue = 0.12f),
        type = Color(red = 0.49f, green = 0.6f, blue = 0.88f),
        variable = Color(red = 0.88f, green = 0.6f, blue = 0.11f),
        field = Color(red = 0.88f, green = 0.86f, blue = 0.42f),
        otherName = Color(red = 0.88f, green = 0.78f, blue = 0.49f),
        directive = Color(red = 0.88f, green = 0.16f, blue = 0.5f),
    ),
)

fun lightColorScheme(): AppColor = AppColor(
    background = Color(red = 0.9f, green = 0.9f, blue = 0.9f),
    backgroundLight = Color(red = 0.8f, green = 0.8f, blue = 0.8f),
    backgroundSemiLight = Color(red = 0.85f, green = 0.85f, blue = 0.85f),
    backgroundContextMenu = Color(red = 0.88f, green = 0.88f, blue = 0.88f),
    backgroundInputField = Color(red = 0.92f, green = 0.92f, blue = 0.92f),
    backgroundButton = Color(red = 0.6f, green = 0.6f, blue = 1f),
    backgroundStopButton = Color(red = 1f, green = 0.43f, blue = 0.43f),
    backgroundDialogOverlay = Color.Black.copy(alpha = 0.4f),
    backgroundTooltip = Color(red = 0.7f, green = 0.7f, blue = 0.9f),
    backgroundHoverDroppable = Color(0f, 0f, 0.4f),
    backgroundInputFieldHighlight = Color(red = 0.6f, green = 0.6f, blue = 0.8f),
    backgroundInputFieldHighlightEmphasize = Color(red = 0.8f, green = 0.8f, blue = 0.3f),
    backgroundCollapsed = Color(red = 0.6f, green = 0.6f, blue = 0.6f),

    primary = Color(red = 0.2f, green = 0.2f, blue = 0.3f),
    unimportant = Color(red = 0.4f, green = 0.4f, blue = 0.5f),
    bright = Color.Black,
    successful = Color(red = 0.1f, green = 0.6f, blue = 0.1f),
    line = Color(red = 0.4f, green = 0.4f, blue = 0.4f),

    placeholder = Color(red = 0.5f, green = 0.5f, blue = 0.5f),
    disabled = Color(red = 0.7f, green = 0.7f, blue = 0.8f),
    highlight = Color(red = 0.4f, green = 0.4f, blue = 0.9f),
    cursor = Color(red = 0.3f, green = 0.3f, blue = 0.3f),
    warning = Color(red = 0.8f, green = 0.3f, blue = 0f),

    httpRequestGet = Color(red = 0f, green = 0.6f, blue = 0f),
    httpRequestPost = Color(red = 0.3f, green = 0.3f, blue = 0.8f),
    httpRequestPut = Color(red = 1f, green = 0.5f, blue = 0f),
    httpRequestPatch = Color(red = 0.25f, green = 0.5f, blue = 0.22f),
    httpRequestDelete = Color(red = 0.9f, green = 0.3f, blue = 0.3f),
    httpRequestOthers = Color(red = 0.35f, green = 0.35f, blue = 0.35f),

    websocketRequest = Color(red = 0.3f, green = 0.3f, blue = 0.45f),

    grpcRequest = Color(red = 0.5f, green = 0.5f, blue = 0.5f),
    graphqlRequest = Color(red = 204, green = 67, blue = 162),

    variableTextColor = Color.Yellow,
    variableBackgroundColor = Color(red = 0.5f, green = 0.5f, blue = 0.8f),
    variableErrorBackgroundColor = Color(red = 0.9f, green = 0.2f, blue = 0.3f),

    functionTextColor = Color.Black,
    functionBackgroundColor = Color(red = 0.3f, green = 0.8f, blue = 0.3f),

    pendingResponseBackground = Color(red = 0.7f, green = 0.7f, blue = 0.7f),
    successfulResponseBackground = Color(red = 0.2f, green = 0.8f, blue = 0.2f),
    errorResponseBackground = Color(red = 1f, green = 0.45f, blue = 0.45f),

    scrollBarUnhover = Color(red = 0.4f, green = 0.4f, blue = 0.4f).copy(alpha = 0.25f),
    scrollBarHover = Color(red = 0.4f, green = 0.4f, blue = 0.4f).copy(alpha = 0.50f),

    syntaxColor = SyntaxColor(
        stringLiteral = Color(red = 0.9f, green = 0.45f, blue = 0.12f),
        numberLiteral = Color(red = 0.3f, green = 0.3f, blue = 0.9f),
        booleanTrueLiteral = Color(red = 0f, green = 0.6f, blue = 0f),
        booleanFalseLiteral = Color(red = 1f, green = 0.1f, blue = 0.1f),
        nothingLiteral = Color(red = 0.4f, green = 0.4f, blue = 0.4f),
        objectKey = Color(red = 0.4f, green = 0.4f, blue = 0.6f),

        comment = Color(red = 0.68f, green = 0.68f, blue = 0.68f),
        keyword = Color(red = 0.25f, green = 0.5f, blue = 0.22f),
        type = Color(red = 0.3f, green = 0.1f, blue = 0.64f),
        variable = Color(red = 0.88f, green = 0.35f, blue = 0f),
        field = Color(red = 0.25f, green = 0.5f, blue = 0.88f),
        otherName = Color(red = 0.5f, green = 0.3f, blue = 0.1f),
        directive = Color(red = 0.16f, green = 0.5f, blue = 0.4f),
    ),
)

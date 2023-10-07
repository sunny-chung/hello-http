package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalColor.current.text,
    fontSize: TextUnit = LocalFont.current.bodyFontSize,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    isFitContent: Boolean = false,
    hasHoverHighlight: Boolean = false,
    hoverColor: Color = LocalColor.current.highlight,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    var textStyle by remember { mutableStateOf(style.copy(fontSize = fontSize)) }
    var isReadyToRender by remember { mutableStateOf(!isFitContent) }
    var isHover by remember { mutableStateOf(false) }


    Text(
        text = text,
        modifier = modifier
            .run {
                @OptIn(ExperimentalComposeUiApi::class)
                if (hasHoverHighlight) {
                     this.onPointerEvent(eventType = PointerEventType.Enter, onEvent = {_ -> isHover = true})
                         .onPointerEvent(eventType = PointerEventType.Exit, onEvent = {_ -> isHover = false})
                } else {
                    this
                }
            }
        ,//.drawWithContent { if (isReadyToRender) drawContent() },
        color = if (hasHoverHighlight && isHover) hoverColor else color,
        fontSize = textStyle.fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = { textLayoutResult ->
            if (isFitContent) {
                if (textLayoutResult.hasVisualOverflow) {
                    println("> t = ${textStyle.fontSize}")
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.8)
                    println("> t2 = ${textStyle.fontSize}")
                } else {
                    isReadyToRender = true
                }
                println("hasVisualOverflow=${textLayoutResult.hasVisualOverflow} isReadyToRender=$isReadyToRender")
            }
            onTextLayout?.invoke(textLayoutResult)
        },
        style = textStyle
    )
}

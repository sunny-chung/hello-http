package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.EnvironmentVariableTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.LineToParagraphTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.MultipleVisualTransformation
import kotlinx.coroutines.launch

@Composable
fun CodeEditorView(
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    text: String,
    onTextChange: ((String) -> Unit)? = null,
    textColor: Color = LocalColor.current.text,
    transformations: List<VisualTransformation> = emptyList(),
    isEnableVariables: Boolean = false,
    knownVariables: Set<String> = setOf(),
    ) {
    val colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField
    )

    Box(modifier = modifier) {
//        val lazyListState = rememberLazyListState()
//        val coroutineScope = rememberCoroutineScope()
//        val scrollState = rememberScrollableState { delta ->
//            coroutineScope.launch {
//                lazyListState.dispatchRawDelta(-delta)
//            }
//            delta
//        }
        val scrollState = rememberScrollState()
        var lastTextHash by remember { mutableStateOf(text.hashCode()) }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
//        log.v { "CodeEditorView text=$text" }
        Row {
            LineNumberView(
                textLayoutResult = textLayoutResult,
//                lazyListState = lazyListState,
                scrollState = scrollState,
            )
            AppTextField(
                value = text,
                onValueChange = { onTextChange?.invoke(it) },
                visualTransformation = (transformations + if (isEnableVariables) {
                    listOf(
                        EnvironmentVariableTransformation(
                            themeColors = LocalColor.current,
                            knownVariables = knownVariables
                        )
                    )
                } else {
                    emptyList()
                } + LineToParagraphTransformation()).let {
                    if (it.size > 1) {
                        MultipleVisualTransformation(it)
                    } else if (it.size == 1) {
                        it.first()
                    } else {
                        VisualTransformation.None
                    }
                },
                onTextLayout = {
                    if (textLayoutResult == null || lastTextHash != text.hashCode()) {
                        textLayoutResult = it
                    }
                },
                readOnly = isReadOnly,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                colors = colors,
//                modifier = Modifier.fillMaxSize().scrollable(scrollState, Orientation.Vertical),
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            )
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState),
//            adapter = rememberScrollbarAdapter(lazyListState),
        )
    }
}

@Composable
fun LineNumberView(modifier: Modifier = Modifier,/* lazyListState: LazyListState,*/ scrollState: ScrollState, textLayoutResult: TextLayoutResult?) {
    with(LocalDensity.current) {
        // unfortunately LazyColumn cannot be used again, due to bugs:
        // https://github.com/JetBrains/compose-multiplatform/issues/1883
//        LazyColumn(
//            state = lazyListState,
//            modifier = Modifier
//                .defaultMinSize(minWidth = 20.dp)
//                .fillMaxHeight()
//                .background(LocalColor.current.backgroundLight)
//                .padding(top = 6.dp), // see AppTextField
//            userScrollEnabled = false,
//        ) {
//            items(count = textLayoutResult?.lineCount ?: 0) { i ->
        Box(
            modifier = Modifier
//                .verticalScroll(scrollState)
                .width(20.dp)
//                .defaultMinSize(minWidth = 20.dp)
                .fillMaxHeight()
                .background(LocalColor.current.backgroundLight)
                .padding(top = 6.dp), // see AppTextField
        ) {
            if (textLayoutResult != null) {
                val firstLine = textLayoutResult.getLineForVerticalPosition(scrollState.value.toFloat())
                for (i in firstLine until (firstLine + 10)) {
                    textLayoutResult!!
                    AppText(
                        text = "${i + 1}",
                        modifier = Modifier.offset(y = ((i - firstLine) * (textLayoutResult.getLineBottom(i) - textLayoutResult.getLineTop(i))).toDp())
                    )
                }
            }
        }
    }
}

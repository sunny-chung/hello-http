package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.bigtext.annotation.ExperimentalBigTextUiApi
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation
import com.sunnychung.lib.multiplatform.bigtext.platform.AsyncOperation
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState
import com.sunnychung.lib.multiplatform.bigtext.ux.ContextMenuItemEntry
import com.sunnychung.lib.multiplatform.bigtext.ux.CoreBigTextField
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberConcurrentLargeAnnotatedBigTextFieldState
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalBigTextUiApi::class)
@Composable
fun AppTextField(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    backgroundColor: Color = LocalColor.current.backgroundInputField,
//    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    isError: Boolean = false,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
    transformation: IncrementalTextTransformation<*>? = null,
    decorator: BigTextDecorator? = null,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
//    minLines: Int = 1,
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = LocalColor.current.text,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = backgroundColor,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    hasIndicatorLine: Boolean = false,
    onPointerEvent: ((event: PointerEvent, charIndex: Int, tag: String?) -> Unit)? = null,
    onFinishInit: BigTextFieldStateScope.() -> Unit = {},
) {
    val textState by rememberConcurrentLargeAnnotatedBigTextFieldState(value, key)

    AppTextField(
        textState = textState,
        onValueChange = {
            val newStringValue = it.buildString()
            log.d { "onTextChange: new = $newStringValue" }
            onValueChange(newStringValue)
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        transformation = transformation,
        decorator = decorator,
        singleLine = singleLine,
        colors = colors,
        contentPadding = contentPadding,
        hasIndicatorLine = hasIndicatorLine,
        onPointerEvent = onPointerEvent,
        onFinishInit = {
            BigTextFieldStateScope(textState).onFinishInit()
        }
    )

//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = modifier
//            .background(colors.backgroundColor(enabled).value)
//            .debugConstraints("$key tf row")
//            .height(IntrinsicSize.Min),
//    ) {
//        if (leadingIcon != null) {
//            leadingIcon()
//        }
//        Box(modifier = Modifier.weight(1f)/*.width(IntrinsicSize.Min).height(IntrinsicSize.Min)*/.padding(contentPadding).debugConstraints("$key tf box"), contentAlignment = Alignment.CenterStart, propagateMinConstraints = true) {
//            CoreBigTextField(
//                text = textState.text,
//                viewState = textState.viewState,
//                onTextChange = {
//                    val newStringValue = it.bigText.buildString()
//                    log.w { "onTextChange: new = $newStringValue" }
////                    onValueChange(newStringValue)
//                },
//                isEditable = enabled && !readOnly,
//                isSelectable = enabled,
//                fontSize = textStyle.fontSize,
//                fontFamily = textStyle.fontFamily ?: FontFamily.SansSerif,
//                color = colors.textColor(enabled).value,
//                cursorColor = colors.cursorColor(false).value,
//                textTransformation = transformation,
//                textDecorator = decorator,
//                isSingleLineInput = singleLine,
////            modifier = modifier
//                modifier = Modifier //.fillMaxWidth()
////                    .fillMaxSize()
////                    .wrapContentSize()
//                    .useMaxSizeOfParentAndChild()
//                    .debugConstraints("$key tf core")
//                    .onGloballyPositioned { log.w { "[$key] tf size = ${it.size}" } }
//            )
//
//            if (placeholder != null && textState.text.isEmpty) {
//                placeholder()
//            }
//        }
//    }

    return

//    /** copy from implementation of TextField **/
//
//    // If color is not provided via the text style, use content color as a default
//    val textColor = textStyle.color.takeOrElse {
//        colors.textColor(enabled).value
//    }
//    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
//
//    @OptIn(ExperimentalMaterialApi::class)
//    BasicTextField(
//        value = value,
//        modifier = modifier
//            .background(colors.backgroundColor(enabled).value, shape)
////            .indicatorLine(false, isError, interactionSource, colors) /** difference here **/
//            .defaultMinSize(
//                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
//                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
//            ),
//        onValueChange = onValueChange,
//        enabled = enabled,
//        readOnly = readOnly,
//        textStyle = mergedTextStyle,
//        cursorBrush = SolidColor(colors.cursorColor(isError).value),
//        visualTransformation = visualTransformation,
//        keyboardOptions = keyboardOptions,
//        keyboardActions = keyboardActions,
//        interactionSource = interactionSource,
//        singleLine = singleLine,
//        maxLines = maxLines,
//        minLines = minLines,
//        decorationBox = @Composable { innerTextField ->
//            // places leading icon, text field with label and placeholder, trailing icon
//            TextFieldDefaults.TextFieldDecorationBox( /* difference */
//                value = value,
//                visualTransformation = visualTransformation,
//                innerTextField = innerTextField,
//                placeholder = placeholder,
//                label = label,
//                leadingIcon = leadingIcon,
//                trailingIcon = trailingIcon,
//                singleLine = singleLine,
//                enabled = enabled,
//                isError = isError,
//                interactionSource = interactionSource,
//                colors = colors,
//                contentPadding = contentPadding /** difference here **/
//            )
//        }
//    )
}

@OptIn(ExperimentalBigTextUiApi::class)
@Composable
fun AppTextField(
    textState: BigTextFieldState,
    onValueChange: (BigText) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
//    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    isError: Boolean = false,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
    transformation: IncrementalTextTransformation<*>? = null,
    decorator: BigTextDecorator? = null,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
//    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
//    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    onTextLayout: (TextLayoutResult) -> Unit = {},
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = LocalColor.current.text,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    hasIndicatorLine: Boolean = false,
    onPointerEvent: ((event: PointerEvent, charIndex: Int, tag: String?) -> Unit)? = null,
    isAsynchronous: Boolean = false,

    /**
     * This parameter exists to work around the weird limitation of `Modifier.semantics(mergeDescendants = true)` that
     * nested nodes with `semantics(mergeDescendants = true)` would not be merged.
     */
    isDisableMerging: Boolean = false,
    onFinishInit: () -> Unit = {},
) {
    var modifier = modifier
    var textFieldModifier: Modifier = Modifier
    if (hasIndicatorLine) {
        var isFocused by remember { mutableStateOf(false) }

        modifier = modifier.onFocusChanged { isFocused = it.hasFocus }

        if (isFocused) {
            modifier = modifier.drawIndicatorLine(BorderStroke(2.dp, Color(98, 0, 238, 222)))
        }
    }
    BigTextFieldLayout(
        modifier = modifier
//            .debugConstraints("BigTextFieldLayout")
            .background(colors.backgroundColor(enabled).value)
            .padding(contentPadding),
        textField = {
            CoreBigTextField(
                text = textState.text,
                viewState = textState.viewState,
                onTextChange = {
                    onValueChange(it.bigText)
                },
                isEditable = enabled && !readOnly,
                isSelectable = enabled,
                isSoftWrapEnabled = !singleLine,
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily ?: FontFamily.SansSerif,
                color = colors.textColor(enabled).value,
                cursorColor = colors.cursorColor(false).value,
                textTransformation = transformation,
                textDecorator = decorator,
                isSingleLineInput = singleLine,
                contextMenu = AppBigTextFieldContextMenu,
                onPointerEvent = onPointerEvent,
//                interactionSource = interactionSource,
                onFinishInit = onFinishInit,
                onHeavyComputation = if (isAsynchronous) AsyncOperation.Asynchronous else AsyncOperation.Synchronous,
                padding = PaddingValues(0.dp),
//                modifier = Modifier.debugConstraints("core tf"),
            )
        },
        leadingIcon = {
            leadingIcon?.invoke()
        },
        placeholder = {
            if (placeholder != null && textState.text.isEmpty) {
                placeholder()
            }
        },
        isDisableMerging = isDisableMerging,
    )

//    /** copy from implementation of TextField **/
//
//    // If color is not provided via the text style, use content color as a default
//    val textColor = textStyle.color.takeOrElse {
//        colors.textColor(enabled).value
//    }
//    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
//
//    @OptIn(ExperimentalMaterialApi::class)
//    BasicTextField(
//        value = value,
//        modifier = modifier
//            .background(colors.backgroundColor(enabled).value, shape)
////            .indicatorLine(false, isError, interactionSource, colors) /** difference here **/
//            .defaultMinSize(
//                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
//                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
//            ),
//        onValueChange = onValueChange,
//        enabled = enabled,
//        readOnly = readOnly,
//        textStyle = mergedTextStyle,
//        cursorBrush = SolidColor(colors.cursorColor(isError).value),
//        visualTransformation = visualTransformation,
//        keyboardOptions = keyboardOptions,
//        keyboardActions = keyboardActions,
//        interactionSource = interactionSource,
//        singleLine = singleLine,
//        maxLines = maxLines,
//        minLines = minLines,
//        decorationBox = @Composable { innerTextField ->
//            // places leading icon, text field with label and placeholder, trailing icon
//            TextFieldDefaults.TextFieldDecorationBox( /* difference */
//                value = value.text,
//                visualTransformation = visualTransformation,
//                innerTextField = innerTextField,
//                placeholder = placeholder,
//                label = label,
//                leadingIcon = leadingIcon,
//                trailingIcon = trailingIcon,
//                singleLine = singleLine,
//                enabled = enabled,
//                isError = isError,
//                interactionSource = interactionSource,
//                colors = colors,
//                contentPadding = contentPadding /** difference here **/
//            )
//        },
//        onTextLayout = onTextLayout,
//    )
}

@Composable
fun AppTextFieldWithPlaceholder(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
//    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    isError: Boolean = false,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
    transformation: IncrementalTextTransformation<*>? = null,
    decorator: BigTextDecorator? = null,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
//    minLines: Int = 1,
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    textColor: Color = LocalColor.current.text,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
//    hasIndicatorLine: Boolean = false,
    onFinishInit: BigTextFieldStateScope.() -> Unit = {},
) {
    return AppTextField(
        key = key,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        backgroundColor = LocalColor.current.backgroundInputField,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        transformation = transformation,
        decorator = decorator,
        singleLine = singleLine,
        maxLines = maxLines,
        colors = colors,
        contentPadding = contentPadding,
        onFinishInit = onFinishInit,
    )

//    /** copy from implementation of TextField **/
//
//    // If color is not provided via the text style, use content color as a default
//    val textColor = textStyle.color.takeOrElse {
//        colors.textColor(enabled).value
//    }
//    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
//
//    @OptIn(ExperimentalMaterialApi::class)
//    BasicTextField(
//        value = value,
//        modifier = modifier
//            .background(colors.backgroundColor(enabled).value, shape)
//            .run {
//                if (hasIndicatorLine) {
//                    this.indicatorLine(enabled, isError, interactionSource, colors)
//                } else {
//                    this
//                }
//            }
//            .defaultMinSize(
//                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
//                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
//            ),
//        onValueChange = onValueChange,
//        enabled = enabled,
//        readOnly = readOnly,
//        textStyle = mergedTextStyle,
//        cursorBrush = SolidColor(colors.cursorColor(isError).value),
//        visualTransformation = visualTransformation,
//        keyboardOptions = keyboardOptions,
//        keyboardActions = keyboardActions,
//        interactionSource = interactionSource,
//        singleLine = singleLine,
//        maxLines = maxLines,
//        minLines = minLines,
//        decorationBox = @Composable { innerTextField ->
//            // places leading icon, text field with label and placeholder, trailing icon
//            androidx.compose.material.TextFieldDefaults.TextFieldDecorationBox( /* difference */
//                value = value,
//                visualTransformation = visualTransformation,
//                innerTextField = innerTextField,
//                placeholder = placeholder,
//                label = label,
//                leadingIcon = leadingIcon,
//                trailingIcon = trailingIcon,
//                singleLine = singleLine,
//                enabled = enabled,
//                isError = isError,
//                interactionSource = interactionSource,
//                colors = colors,
//                contentPadding = contentPadding /** difference here **/
//            )
//        }
//    )
}

@Composable
fun AppTextFieldWithPlaceholder(
    value: BigTextFieldState,
    onValueChange: (BigText) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
//    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    isError: Boolean = false,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
//    minLines: Int = 1,
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    textColor: Color = LocalColor.current.text,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    hasIndicatorLine: Boolean = false,
    onPointerEvent: ((event: PointerEvent, charIndex: Int, tag: String?) -> Unit)? = null,
    onFinishInit: () -> Unit = {},
) {
    return AppTextField(
        textState = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        singleLine = singleLine,
//        maxLines = maxLines,
        colors = colors,
        contentPadding = contentPadding,
        hasIndicatorLine = hasIndicatorLine,
        onPointerEvent = onPointerEvent,
        onFinishInit = onFinishInit,
    )

//    /** copy from implementation of TextField **/
//
//    // If color is not provided via the text style, use content color as a default
//    val textColor = textStyle.color.takeOrElse {
//        colors.textColor(enabled).value
//    }
//    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
//
//    @OptIn(ExperimentalMaterialApi::class)
//    BasicTextField(
//        value = value,
//        modifier = modifier
//            .background(colors.backgroundColor(enabled).value, shape)
//            .run {
//                if (hasIndicatorLine) {
//                    this.indicatorLine(enabled, isError, interactionSource, colors)
//                } else {
//                    this
//                }
//            }
//            .defaultMinSize(
//                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
//                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
//            ),
//        onValueChange = onValueChange,
//        enabled = enabled,
//        readOnly = readOnly,
//        textStyle = mergedTextStyle,
//        cursorBrush = SolidColor(colors.cursorColor(isError).value),
//        visualTransformation = visualTransformation,
//        keyboardOptions = keyboardOptions,
//        keyboardActions = keyboardActions,
//        interactionSource = interactionSource,
//        singleLine = singleLine,
//        maxLines = maxLines,
//        minLines = minLines,
//        decorationBox = @Composable { innerTextField ->
//            // places leading icon, text field with label and placeholder, trailing icon
//            androidx.compose.material.TextFieldDefaults.TextFieldDecorationBox( /* difference */
//                value = value.text,
//                visualTransformation = visualTransformation,
//                innerTextField = innerTextField,
//                placeholder = placeholder,
//                label = label,
//                leadingIcon = leadingIcon,
//                trailingIcon = trailingIcon,
//                singleLine = singleLine,
//                enabled = enabled,
//                isError = isError,
//                interactionSource = interactionSource,
//                colors = colors,
//                contentPadding = contentPadding /** difference here **/
//            )
//        }
//    )
}

fun Modifier.drawIndicatorLine(indicatorBorder: BorderStroke): Modifier {
    val strokeWidthDp = indicatorBorder.width
    return drawWithContent {
        drawContent()
        if (strokeWidthDp == Dp.Hairline) return@drawWithContent
        val strokeWidth = strokeWidthDp.value * density
        val y = size.height - strokeWidth / 2
        drawLine(
            indicatorBorder.brush,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth
        )
    }
}

val AppBigTextFieldContextMenu =
    @Composable { isVisible: Boolean, onDismiss: () -> Unit, entries: List<ContextMenuItemEntry>, testTag: String ->
        ContextMenuView(
            isShowContextMenu = isVisible,
            onDismissRequest = onDismiss,
            colors = LocalColor.current,
            testTagParts = arrayOf(testTag),
            populatedItems = entries.map {
                when (it.type) {
                    ContextMenuItemEntry.Type.Button -> DropDownKeyValueAction(
                        key = it.displayText,
                        displayText = it.displayText,
                        isEnabled = it.isEnabled,
                        action = it.action,
                    )

                    ContextMenuItemEntry.Type.Divider -> DropDownDivider
                }
            },
            onClickItem = onClickItem@{
                if (!it.isEnabled) {
                    return@onClickItem false
                }
                (it as? DropDownKeyValueAction<*>)?.action?.invoke()
                true
            },
            selectedItem = null,
            isClickable = true,
        )
    }

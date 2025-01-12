package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldColors
import com.sunnychung.application.multiplatform.hellohttp.ux.compose.TextFieldDefaults
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.bigtext.annotation.ExperimentalBigTextUiApi
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation
import com.sunnychung.lib.multiplatform.bigtext.ux.CoreBigTextField
import com.sunnychung.lib.multiplatform.bigtext.ux.compose.debugConstraints
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberConcurrentLargeAnnotatedBigTextFieldState

/**
 * This file is essential a clone of Compose TextField.kt, TetFieldImpl.kt, TextFieldDefaults.kt, OutlinedTextField.kt, with
 * some modifications to padding values and styles.
 */

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
    contentPadding: PaddingValues = PaddingValues(2.dp),
) {
    val textState by rememberConcurrentLargeAnnotatedBigTextFieldState(value, key)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(colors.backgroundColor(enabled).value)
            .debugConstraints("$key tf row")
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Box(modifier = Modifier.padding(contentPadding).debugConstraints("$key tf box"), contentAlignment = Alignment.CenterStart) {
            CoreBigTextField(
                text = textState.text,
                viewState = textState.viewState,
                onTextChange = {
                    val newStringValue = it.bigText.buildString()
                    log.w { "onTextChange: new = $newStringValue" }
//                    onValueChange(newStringValue)
                },
                isEditable = enabled && !readOnly,
                isSelectable = enabled,
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily ?: FontFamily.SansSerif,
                color = colors.textColor(enabled).value,
                cursorColor = colors.cursorColor(false).value,
                textTransformation = transformation,
                textDecorator = decorator,
                isSingleLineInput = singleLine,
//            modifier = modifier
                modifier = Modifier.fillMaxWidth()
                    .debugConstraints("$key tf core")
                    .onGloballyPositioned { log.w { "[$key] tf size = ${it.size}" } }
            )

            if (placeholder != null && textState.text.isEmpty) {
                placeholder()
            }
        }
    }

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

@Composable
fun AppTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onTextLayout: (TextLayoutResult) -> Unit = {},
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = LocalColor.current.text,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp)
) {
    /** copy from implementation of TextField **/

    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier = modifier
            .background(colors.backgroundColor(enabled).value, shape)
//            .indicatorLine(false, isError, interactionSource, colors) /** difference here **/
            .defaultMinSize(
                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
            ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            // places leading icon, text field with label and placeholder, trailing icon
            TextFieldDefaults.TextFieldDecorationBox( /* difference */
                value = value.text,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = contentPadding /** difference here **/
            )
        },
        onTextLayout = onTextLayout,
    )
}

@Composable
fun AppTextFieldWithPlaceholder(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.TextFieldShape,
    textColor: Color = LocalColor.current.text,
    colors: androidx.compose.material.TextFieldColors = androidx.compose.material.TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    hasIndicatorLine: Boolean = false,
) {
    /** copy from implementation of TextField **/

    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier = modifier
            .background(colors.backgroundColor(enabled).value, shape)
            .run {
                if (hasIndicatorLine) {
                    this.indicatorLine(enabled, isError, interactionSource, colors)
                } else {
                    this
                }
            }
            .defaultMinSize(
                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
            ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            // places leading icon, text field with label and placeholder, trailing icon
            androidx.compose.material.TextFieldDefaults.TextFieldDecorationBox( /* difference */
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = contentPadding /** difference here **/
            )
        }
    )
}

@Composable
fun AppTextFieldWithPlaceholder(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.TextFieldShape,
    textColor: Color = LocalColor.current.text,
    colors: androidx.compose.material.TextFieldColors = androidx.compose.material.TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    hasIndicatorLine: Boolean = false,
) {
    /** copy from implementation of TextField **/

    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier = modifier
            .background(colors.backgroundColor(enabled).value, shape)
            .run {
                if (hasIndicatorLine) {
                    this.indicatorLine(enabled, isError, interactionSource, colors)
                } else {
                    this
                }
            }
            .defaultMinSize(
                minWidth = 1.dp, /** difference here **/  // TextFieldDefaults.MinWidth,
                minHeight = 1.dp /** difference here **/  //TextFieldDefaults.MinHeight
            ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            // places leading icon, text field with label and placeholder, trailing icon
            androidx.compose.material.TextFieldDefaults.TextFieldDecorationBox( /* difference */
                value = value.text,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = contentPadding /** difference here **/
            )
        }
    )
}

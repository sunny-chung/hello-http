package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.AppUX.ENV_VAR_VALUE_MAX_DISPLAY_LENGTH
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableDecorator
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.EnvironmentVariableIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.FunctionIncrementalTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MultipleIncrementalTransformation
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation
import com.sunnychung.lib.multiplatform.bigtext.util.weakRefOf
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberLargeAnnotatedBigTextFieldState

@Composable
fun AppTextFieldWithVariables(
    textState: BigTextFieldState,
    onValueChange: (BigText) -> Unit,
    modifier: Modifier = Modifier,
    variables: Map<String, String>,
    isSupportVariables: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    hasIndicatorLine: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
//    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    isError: Boolean = false,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
//    transformation: IncrementalTextTransformation<*>? = null,
//    decorator: BigTextDecorator? = null,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
//    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    onTextLayout: (TextLayoutResult) -> Unit = {},
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    textColor: Color = LocalColor.current.text,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    onFinishInit: () -> Unit = {},
) {
    val themeColors = LocalColor.current
    val fonts = LocalFont.current
    val variableKeys = variables.keys

    var mouseHoverVariable by remember(weakRefOf(textState)) { mutableStateOf<String?>(null) }

    AppTooltipArea(
        isVisible = mouseHoverVariable != null && mouseHoverVariable in variables,
        tooltipText = mouseHoverVariable?.let {
            val s = variables[it] ?: return@let null
            if (s.length > ENV_VAR_VALUE_MAX_DISPLAY_LENGTH) {
                s.substring(0, ENV_VAR_VALUE_MAX_DISPLAY_LENGTH) + " ..."
            } else {
                s
            }
        } ?: "",
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        AppTextField(
            textState = textState,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            hasIndicatorLine = hasIndicatorLine,
            transformation = if (isSupportVariables) {
                remember(fonts, themeColors) {
                    MultipleIncrementalTransformation(listOf(
                        EnvironmentVariableIncrementalTransformation(font = fonts),
                        FunctionIncrementalTransformation(themeColors, fonts),
                    ))
                }
            } else {
                null
            },
            decorator = if (isSupportVariables) {
                remember(themeColors, fonts) {
                    // reuse the same decorator even if variableKeys is changed
                    // to avoid text field blinking due to re-initialization
                    EnvironmentVariableDecorator(
                        themeColors = themeColors,
                        font = fonts,
                        knownVariables = variableKeys
                    )
                }.also {
                    it.knownVariables = variableKeys // update the cache
                }
            } else {
                null
            },
            singleLine = singleLine,
            colors = colors,
            contentPadding = contentPadding,
            onPointerEvent = if (isSupportVariables) {
                { event, tag ->
                    log.v { "onPointerEventOnAnnotatedTag $tag $event" }
                    mouseHoverVariable =
                        if (tag?.startsWith(EnvironmentVariableIncrementalTransformation.TAG_PREFIX) == true) {
                            tag.replaceFirst(EnvironmentVariableIncrementalTransformation.TAG_PREFIX, "")
                        } else {
                            null
                        }
                }
            } else {
                null
            },
            interactionSource = interactionSource,
            onFinishInit = onFinishInit,
            isDisableMerging = true,
            modifier = if (!singleLine) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        )

    }
}

@Composable
fun AppTextFieldWithVariables(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    variables: Map<String, String>,
    isSupportVariables: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    hasIndicatorLine: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = LocalColor.current.text,
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    onTextLayout: (TextLayoutResult) -> Unit = {},
//    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = textColor,
        placeholderColor = LocalColor.current.placeholder,
        cursorColor = LocalColor.current.cursor,
        backgroundColor = LocalColor.current.backgroundInputField,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
    onFinishInit: (BigTextFieldState) -> Unit = {},
) {
    val textState by rememberLargeAnnotatedBigTextFieldState(value, key)

    AppTextFieldWithVariables(
        textState = textState,
        onValueChange = {
            onValueChange(it.buildString())
        },
        modifier = modifier,
        variables = variables,
        isSupportVariables = isSupportVariables,
        enabled = enabled,
        readOnly = readOnly,
        hasIndicatorLine = hasIndicatorLine,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        singleLine = singleLine,
        maxLines = maxLines,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        onFinishInit = {
            onFinishInit(textState)
        },
    )
}

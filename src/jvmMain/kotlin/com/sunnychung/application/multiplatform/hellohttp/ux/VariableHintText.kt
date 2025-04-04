package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun VariableHintText(modifier: Modifier = Modifier
    .fillMaxWidth()
    .padding(start = 4.dp, bottom = 12.dp)
) {
    val colour = LocalColor.current
    AppText(
        text = buildAnnotatedString {
            append("To use a variable defined here, type ")
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = colour.highlight)) {
                append("\${{name}}")
            }
            append(" at the use-site.")
        },
        color = colour.primary,
        modifier = modifier
    )
}

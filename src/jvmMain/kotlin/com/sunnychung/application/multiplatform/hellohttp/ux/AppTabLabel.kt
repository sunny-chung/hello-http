package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppTabLabel(modifier: Modifier = Modifier, text: String) {
    AppText(text = text, modifier = modifier.padding(8.dp))
}

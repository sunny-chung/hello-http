package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont

@Composable
fun RequestEditorEmptyView(modifier: Modifier = Modifier, isShowCreateRequest: Boolean, onClickCreateRequest: () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isShowCreateRequest) {
            AppText(
                text = "Click to create request",
                fontSize = LocalFont.current.largeInfoSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onClickCreateRequest() },
            )
        }
    }
}

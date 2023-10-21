package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun StatusBarView(modifier: Modifier = Modifier) {
    val colors = LocalColor.current
    val metadataManager = AppContext.MetadataManager
    Box(modifier = modifier.fillMaxWidth().height(24.dp).background(colors.backgroundLight).padding(4.dp)) {
        AppText(
            text = "v${metadataManager.version} (${metadataManager.gitCommitHash})",
            fontSize = 12.sp,
            modifier = Modifier.align(
                Alignment.CenterStart
            )
        )
    }
}

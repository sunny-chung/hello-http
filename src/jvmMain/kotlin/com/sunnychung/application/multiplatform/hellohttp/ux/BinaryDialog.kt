package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun BinaryDialog(key: String, isVisible: Boolean, content: String, positiveButtonCaption: String = "Yes", negativeButtonCaption: String = "Cancel", positiveButtonColor: Color = LocalColor.current.backgroundButton, onClickPositiveButton: () -> Unit, onDismiss: () -> Unit) {
    MainWindowDialog(key, isVisible, onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp, end = 12.dp)
            ) {
                AppText(content)
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    AppTextButton(backgroundColor = LocalColor.current.backgroundCancelButton, text = negativeButtonCaption) { onDismiss() }
                    AppTextButton(backgroundColor = positiveButtonColor, text = positiveButtonCaption) { onClickPositiveButton(); onDismiss() }
                }
            }
        }
    }
}

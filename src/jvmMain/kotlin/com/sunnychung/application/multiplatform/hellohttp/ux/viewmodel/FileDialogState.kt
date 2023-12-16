package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This class exists as a workaround to the bug that
 * "Modifier.clickable" is invoked twice in the Windows OS.
 *
 * Might be related to https://github.com/JetBrains/compose-multiplatform/issues/3892
 */
class FileDialogState {
    val lastCloseTime = MutableStateFlow<KInstant?>(null)
}

@Composable
fun rememberFileDialogState() = remember { FileDialogState() }

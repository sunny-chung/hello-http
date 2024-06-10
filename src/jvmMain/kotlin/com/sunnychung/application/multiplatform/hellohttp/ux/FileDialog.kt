package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.AwtWindow
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel.FileDialogState
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * For UX test only
 */
var testChooseFile: File? = null

/**
 * Due to the bug stated in {@link FileDialogState}, result of onCloseRequest has 3 cases:
 * 1. non-empty list -> user selected a file
 * 2. empty list -> user clicked cancel
 * 3. null -> internally cancel
 */
@Composable
fun FileDialog(
    parent: Frame? = null,
    state: FileDialogState,
    mode: Int = FileDialog.LOAD,
    title: String = "Choose a file",
    filename: String? = null,
    onCloseRequest: (result: List<File>?) -> Unit
) {
    log.d { "FileDialog 1" }

    testChooseFile?.let {
        rememberCoroutineScope().launch {
            delay(50L)
            testChooseFile = null
            onCloseRequest(listOf(it))
        }
        return
    }

    val lastCloseTime = state.lastCloseTime.value
    if (lastCloseTime != null && KInstant.now() - lastCloseTime < KDuration.Companion.of(1, KFixedTimeUnit.Second)) {
        onCloseRequest(null)
        return
    }
    log.d { "FileDialog 2 $lastCloseTime" }
    AwtWindow(
        create = {
            log.d { "FileDialog create" }
            object : FileDialog(parent, title, mode) {
                init {
                    if (filename != null) {
                        file = filename
                    }
                }

                override fun setVisible(b: Boolean) {
                    super.setVisible(b)
                    log.d { "FileDialog setVisible $b" }
                    if (b) {
                        onCloseRequest(this.files.toList())
                        state.lastCloseTime.value = KInstant.now()
                    }
                }
            }
        },
        dispose = FileDialog::dispose
    )
}

package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun FileDialog(
    parent: Frame? = null,
    mode: Int = FileDialog.LOAD,
    filename: String? = null,
    onCloseRequest: (result: List<File>) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", mode) {
            init {
                if (filename != null) {
                    file = filename
                }
            }

            override fun setVisible(b: Boolean) {
                super.setVisible(b)
                if (b) {
                    onCloseRequest(this.files.toList())
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

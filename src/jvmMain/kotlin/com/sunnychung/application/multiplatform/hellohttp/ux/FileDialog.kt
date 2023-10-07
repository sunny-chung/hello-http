package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun FileDialog(
    parent: Frame? = null,
    onCloseRequest: (result: List<File>) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
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

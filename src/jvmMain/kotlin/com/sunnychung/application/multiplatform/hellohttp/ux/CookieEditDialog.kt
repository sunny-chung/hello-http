package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.network.util.Cookie
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.toJavaInstant
import com.sunnychung.lib.multiplatform.kdatetime.toKInstant
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val headerColumnWidth = 180.dp

@Composable
fun CookieEditDialog(cookie: Cookie, knownVariables: Map<String, String>, isVisible: Boolean, onSave: (Cookie) -> Unit, onDismiss: () -> Unit) {
    var editing: Cookie by remember(cookie) { mutableStateOf(cookie) }
    MainWindowDialog(
        key = "CookieEditDialog",
        isEnabled = isVisible,
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.widthIn(max = 520.dp)) {
            val focusRequester = remember { FocusRequester() }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText("Domain", modifier = Modifier.width(headerColumnWidth))
                AppTextField(
                    key = "Cookie/${cookie.hashCode()}/Domain",
                    value = editing.domain,
                    onValueChange = { editing = editing.copy(domain = it) },
                    singleLine = true,
                    onFinishInit = { focusRequester.requestFocus() },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText("Path", modifier = Modifier.width(headerColumnWidth))
                AppTextField(
                    key = "Cookie/${cookie.hashCode()}/Path",
                    value = editing.path.takeIf { it.isNotBlank() } ?: "/",
                    onValueChange = { editing = editing.copy(path = it.takeIf { it.isNotBlank() } ?: "/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText("Name", modifier = Modifier.width(headerColumnWidth))
                AppTextField(
                    key = "Cookie/${cookie.hashCode()}/Name",
                    value = editing.name,
                    onValueChange = { editing = editing.copy(name = it) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText("Value", modifier = Modifier.width(headerColumnWidth))
                AppTextFieldWithVariables(
                    key = "Cookie/${cookie.hashCode()}/Value",
                    value = editing.value,
                    onValueChange = { editing = editing.copy(value = it) },
                    variables = knownVariables,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AppText("Expires (e.g. 01 Jan 2000 00:00:00 +0800)", modifier = Modifier.width(headerColumnWidth))
                AppTextField(
                    key = "Cookie/${cookie.hashCode()}/Expires",
                    value = editing.expires?.let {
                        DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.systemDefault())
                            .format(it.toJavaInstant())
                    } ?: "",
                    onValueChange = {
                        editing = editing.copy(
                            expires = try {
                                Instant.from(
                                    DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.systemDefault()).parse(it.trim())
                                ).toKInstant()
                            } catch (e: Throwable) {
                                if (it.isNotBlank()) {
                                    log.w("Cannot parse input date time", e)
                                }
                                null
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                AppText("Secure", modifier = Modifier.width(headerColumnWidth))
                AppCheckbox(
                    checked = editing.secure,
                    onCheckedChange = { editing = editing.copy(secure = it) },
                    size = 16.dp
                )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterEnd)) {
                    AppTextButton(text = "Cancel", backgroundColor = LocalColor.current.backgroundStopButton) {
                        onDismiss()
                    }
                    AppTextButton(text = "OK", isEnabled = editing.name.isNotBlank()) {
                        onSave(editing)
                        onDismiss()
                    }
                }
            }
        }
    }
}

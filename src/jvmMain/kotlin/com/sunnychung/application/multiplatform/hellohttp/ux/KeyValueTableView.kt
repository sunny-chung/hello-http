package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor

@Composable
fun KeyValueTableView(modifier: Modifier = Modifier, keyValues: List<Pair<String, String>>) {
    val colors = LocalColor.current

    Column(modifier) {
        Row {
            AppText(text = "Key", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f).border(width = 1.dp, color = colors.placeholder, RectangleShape).padding(all = 8.dp))
            AppText(text = "Value", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f).border(width = 1.dp, color = colors.placeholder, RectangleShape).padding(all = 8.dp))
        }
        LazyColumn {
            items(items = keyValues) {
                Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                    AppTextField(value = it.first, readOnly = true, onValueChange = {}, backgroundColor = Color.Transparent, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(0.4f).fillMaxHeight().border(width = 1.dp, color = colors.placeholder, RectangleShape).padding(all = 8.dp))
                    AppTextField(value = it.second, readOnly = true, onValueChange = {}, backgroundColor = Color.Transparent, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(0.6f).fillMaxHeight().border(width = 1.dp, color = colors.placeholder, RectangleShape).padding(all = 8.dp))
                }
            }
        }
    }
}

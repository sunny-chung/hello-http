package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset

private val DATE_TIME_FORMAT = KDateTimeFormat("HH:mm:ss.lll")
private val TIMESTAMP_COLUMN_WIDTH_DP = 120.dp

@Composable
fun TransportTimelineView(modifier: Modifier = Modifier, exchange: RawExchange) {
    val timestampColumnWidthDp = TIMESTAMP_COLUMN_WIDTH_DP

    Box(modifier = modifier) {
        Box(
            Modifier
                .width(timestampColumnWidthDp + 6.dp)
                .fillMaxHeight()
                .background(LocalColor.current.backgroundSemiLight)
        )
        Box(
            Modifier
                .offset(x = timestampColumnWidthDp + 6.dp)
                .width(1.dp)
                .fillMaxHeight()
                .background(LocalColor.current.line)
        )

        LazyColumn {
            items(items = exchange.exchanges) {
                Row(modifier = Modifier.height(IntrinsicSize.Max).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    TimestampColumn(
                        time = it.instant,
                        modifier = Modifier.width(TIMESTAMP_COLUMN_WIDTH_DP).fillMaxHeight().padding(end = 1.dp)
                    )
                    AppText(
                        text = when (it.direction) {
                            RawExchange.Direction.Outgoing -> "> "
                            RawExchange.Direction.Incoming -> "< "
                        },
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    AppText(text = it.detail, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TimestampColumn(modifier: Modifier = Modifier, time: KInstant) {
    val text = DATE_TIME_FORMAT.format(time.atZoneOffset(KZoneOffset.local()))

    AppText(text = text, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Right, modifier = modifier.padding(end = 4.dp))
}

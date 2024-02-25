package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.extension.lastOrNull
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestResponse
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestResult

private val FIRST_COLUMN_WIDTH = 160.dp

@Composable
fun LoadTestReportView(modifier: Modifier = Modifier, loadTestResult: LoadTestResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        with(loadTestResult) {
            Row {
                AppText("Number of requests", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText(numRequestsSent.toString())
            }
            Row {
                AppText("Number of pending responses", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((numRequestsSent - numResponses).toString())
            }
            Row {
                AppText("Number of Success responses", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((numCatagorizedResponsesOverTime.lastOrNull()?.get(LoadTestResponse.Category.Success)).toString())
            }
            Row {
                AppText("Number of Server Error responses", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((numCatagorizedResponsesOverTime.lastOrNull()?.get(LoadTestResponse.Category.ServerError)).toString())
            }
            Row {
                AppText("Number of Client Error responses", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((numCatagorizedResponsesOverTime.lastOrNull()?.get(LoadTestResponse.Category.ClientError)).toString())
            }
            Row {
                AppText("Latency Min", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.min).toString())
            }
            Row {
                AppText("Latency Max", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.max).toString())
            }
            Row {
                AppText("Latency Average", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.average).toString())
            }
            Row {
                AppText("Latency Median", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.median).toString())
            }
            Row {
                AppText("Latency 90%", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.at90Percent).toString())
            }
            Row {
                AppText("Latency 95%", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.at95Percent).toString())
            }
            Row {
                AppText("Latency 99%", modifier = Modifier.width(FIRST_COLUMN_WIDTH))
                AppText((latenciesMsOverTime.lastOrNull()?.at99Percent).toString())
            }
        }
    }
}

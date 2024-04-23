package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key.Companion.Symbol
import androidx.compose.ui.unit.dp
//import co.yml.kmm.charts.axis.AxisData
//import co.yml.kmm.charts.common.extensions.formatToSinglePrecision
//import co.yml.kmm.charts.common.model.Point
//import co.yml.kmm.charts.ui.linechart.LineChart
//import co.yml.kmm.charts.ui.linechart.model.GridLines
//import co.yml.kmm.charts.ui.linechart.model.IntersectionPoint
//import co.yml.kmm.charts.ui.linechart.model.Line
//import co.yml.kmm.charts.ui.linechart.model.LineChartData
//import co.yml.kmm.charts.ui.linechart.model.LinePlotData
//import co.yml.kmm.charts.ui.linechart.model.LineType
//import co.yml.kmm.charts.ui.linechart.model.SelectionHighlightPoint
//import co.yml.kmm.charts.ui.linechart.model.SelectionHighlightPopUp
//import co.yml.kmm.charts.ui.linechart.model.ShadowUnderLine
//import com.aay.compose.lineChart.LineChart
//import com.aay.compose.lineChart.model.LineParameters
//import com.aay.compose.lineChart.model.LineType
import com.fasterxml.jackson.module.kotlin.jsonMapper
//import com.netguru.multiplatform.charts.ChartAnimation
//import com.netguru.multiplatform.charts.line.LineChart
//import com.netguru.multiplatform.charts.line.LineChartData
//import com.netguru.multiplatform.charts.line.LineChartPoint
//import com.netguru.multiplatform.charts.line.LineChartSeries
import com.sunnychung.application.multiplatform.hellohttp.extension.lastOrNull
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestResponse
import com.sunnychung.application.multiplatform.hellohttp.model.LoadTestResult
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
//import io.github.koalaplot.core.Symbol
//import io.github.koalaplot.core.style.LineStyle
//import io.github.koalaplot.core.xygraph.LinearAxisModel
//import io.github.koalaplot.core.xygraph.XYGraph
//import io.github.koalaplot.core.xygraph.rememberAxisStyle

import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.DoubleLinearAxisModel
import io.github.koalaplot.core.xygraph.LongLinearAxisModel
//import io.github.koalaplot.core.xygraph.LinearAxisModel
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.autoScaleXRange
import io.github.koalaplot.core.xygraph.autoScaleYRange
import io.github.koalaplot.core.xygraph.rememberAxisStyle

private val FIRST_COLUMN_WIDTH = 160.dp

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun LoadTestReportView(modifier: Modifier = Modifier, loadTestResult: LoadTestResult) {
    val colours = LocalColor.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.verticalScroll(rememberScrollState())) {
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

            // kmm ycharts
            // Issue: Incompatible with latest jetpack compose
//            val maxValue = latenciesMsOverTime.maxByOrNull { it.value.at95Percent }?.value?.at95Percent ?: 1.0
//            val steps = maxValue.toInt() / 100 + 1
//            val xAxisData = AxisData.Builder()
//                .axisStepSize(10.dp)
//                .steps((latenciesMsOverTime.size - 1) / 5)
//                .labelData { i -> i.toString() }
//                .labelAndAxisLinePadding(15.dp)
//                .build()
//            val yAxisData = AxisData.Builder()
//                .steps(steps)
//                .labelAndAxisLinePadding(30.dp)
//                .labelData { i ->
//                    (i * 100).toString()
//                }.build()
//            val data = LineChartData(
//                linePlotData = LinePlotData(
//                    lines = listOf(
//                        Line(
//                            dataPoints = latenciesMsOverTime.map { Point(it.key.toFloat(), it.value.at95Percent.toFloat()) },
//                            co.yml.kmm.charts.ui.linechart.model.LineStyle(lineType = LineType.Straight()),
//                            IntersectionPoint(),
//                            SelectionHighlightPoint(),
//                            ShadowUnderLine(),
//                            SelectionHighlightPopUp()
//                        )
//                    )
//                ),
//                isZoomAllowed = false,
//                xAxisData = xAxisData,
//                yAxisData = yAxisData,
//                gridLines = GridLines()
//            )
//            if (latenciesMsOverTime.isNotEmpty()) {
//                LineChart(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(300.dp),
//                    lineChartData = data
//                )
//            }

            ChartLayout(title = { AppText("Latencies over Time (95%)") }, modifier = Modifier.heightIn(max = 300.dp)) {
                val points = latenciesMsOverTime.map { (timestamp, result) ->
                    DefaultPoint(timestamp, result.at95Percent)
                }
                val minimumTimeDifference = (points.lastOrNull()?.x ?: 0) - (points.firstOrNull()?.x ?: 0)
                val pseudoPoints = points.ifEmpty {
                    val now = KInstant.now().toMilliseconds()
                    listOf(DefaultPoint(now - 2000, 0.0), DefaultPoint(now, 0.0))
                }.let {
                    if (it.size == 1) {
                        listOf(DefaultPoint(it.first().x - 2000, it.first().y), it.first())
                    } else {
                        it
                    }
                }

//                val floatPoints = points.map { DefaultPoint(it.x.toFloat(), it.y) }
                XYGraph(
                    xAxisModel = LongLinearAxisModel(
                        points.autoScaleXRange(),
//                        pseudoPoints.first().x .. pseudoPoints.last().x,
                        minorTickCount = 0,
                        minimumMajorTickIncrement = minOf(5000, minimumTimeDifference),
                    ),
                    yAxisModel = DoubleLinearAxisModel(
                        points.autoScaleYRange(),
                        minorTickCount = 2,
                    ),
                    xAxisLabels = { KZonedInstant(it, KZoneOffset.local()).format("HH:mm:ss") },
                    yAxisLabels = { String.format("%.1f", it) },
                    xAxisStyle = rememberAxisStyle(colours.primary, 20.dp, 0.dp),
                    yAxisStyle = rememberAxisStyle(colours.primary, 20.dp, 0.dp),
                    xAxisTitle = "Time",
                    yAxisTitle = "Latency (ms)",
                ) {
                    if (points.isNotEmpty()) {
//                        if (points.size > 1) {
                            LinePlot(
                                points,
                                lineStyle = LineStyle(strokeWidth = 1.dp, brush = SolidColor(colours.primary))
                            )
//                        }
                        LinePlot(
                            points,
                            symbol = {
                                Symbol(
                                    fillBrush = SolidColor(colours.highlight),
                                    outlineBrush = SolidColor(colours.primary),
                                    shape = CircleShape,
                                )
                            }
                        )
                    }
                }
            }

            // thechance101:chart
            // Issue: Chart is condensing when more data is added
            // https://github.com/TheChance101/AAY-chart/issues/93
//            LineChart(
//                modifier = Modifier.heightIn(max = 300.dp),
//                linesParameters = listOf(LineParameters(
//                    label = "Latency 95%",
//                    data = latenciesMsOverTime.map { (timestamp, result) -> result.at95Percent }.ifEmpty { listOf(0.0) },
//                    lineColor = colours.primary,
//                    lineType = LineType.DEFAULT_LINE,
//                    lineShadow = true,
//                )),
//                xAxisData = latenciesMsOverTime.map { (timestamp, result) -> KInstant(timestamp).format("HH:mm:ss") }.ifEmpty { listOf("-") },
//            )
            log.v { "latenciesMsOverTime = ${jsonMapper().writeValueAsString(latenciesMsOverTime)}" }

//            if (latenciesMsOverTime.size >= 2) {
//                LineChart(
//                    lineChartData = LineChartData(
//                        listOf(
////                            LineChartSeries("", lineWidth = 0.dp, listOfPoints = listOf(
////                                LineChartPoint(latenciesMsOverTime.firstKey(), 0f), LineChartPoint(latenciesMsOverTime.lastKey(), 1f)
////                            ), lineColor = Color.Transparent),
//                            LineChartSeries(
//                                "Latency 95%",
//                                listOfPoints = latenciesMsOverTime.map { (timestamp, result) ->
//                                    LineChartPoint(x = timestamp, y = result.at95Percent.toFloat())
//                                },
//                                lineColor = colours.primary
//                            ),
//                            LineChartSeries(
//                                "Latency 99%",
//                                listOfPoints = latenciesMsOverTime.map { (timestamp, result) ->
//                                    LineChartPoint(x = timestamp, y = result.at99Percent.toFloat())
//                                },
//                                lineColor = colours.primary
//                            ),
////                            LineChartSeries(
////                                "Latency Max",
////                                listOfPoints = latenciesMsOverTime.map { (timestamp, result) ->
////                                    LineChartPoint(x = timestamp, y = result.max.toFloat())
////                                },
////                                lineColor = colours.primary
////                            ),
//                            LineChartSeries(
//                                "Latency Min",
//                                listOfPoints = latenciesMsOverTime.map { (timestamp, result) ->
//                                    LineChartPoint(x = timestamp, y = result.min.toFloat())
//                                },
//                                lineColor = colours.primary
//                            ),
//                        )
//                    ),
//                    xAxisLabel = { AppText(text = KInstant(it as Long).atZoneOffset(KZoneOffset.local()).format("HH:mm:ss")) },
//                    yAxisLabel = { AppText(it.toString()) },
//                    roundMinMaxClosestTo = 1,
////                    animation = ChartAnimation.Disabled,
////                    animation = ChartAnimation.Sequenced(),
//                    modifier = Modifier.height(300.dp).padding(bottom = 60.dp, end = 20.dp),
//                )
//                Spacer(modifier = Modifier.height(60.dp))
//            }

        }
    }
}

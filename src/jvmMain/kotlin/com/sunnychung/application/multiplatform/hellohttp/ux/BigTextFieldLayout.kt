package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

@Composable
fun BigTextFieldLayout(
    modifier: Modifier = Modifier,
    textField: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit = {},
    placeholder: @Composable () -> Unit = {},
) {
    var index = 0
    val TextFieldId = index++
    val LeadingId = index++
    val PlaceholderId = index++
    val contents = MutableList<@Composable () -> Unit>(index) { {} }
    contents[TextFieldId] = textField
    contents[LeadingId] = leadingIcon
    contents[PlaceholderId] = placeholder
    Layout(
        modifier = modifier,
        content = {
            contents.forEach {
                Box(propagateMinConstraints = true) {
                    it()
                }
            }
        },
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                var consumedWidth = 0
                var remainWidth = constraints.maxWidth

                var myHeight = 0

                fun consume(width: Int) {
                    consumedWidth += width
                    if (remainWidth != Constraints.Infinity) {
                        remainWidth -= width
                    }
                }

                fun enlarge(item: Placeable) {
//                    myWidth = maxOf(myWidth, item.width)
                    myHeight = maxOf(myHeight, item.height)
                }

                fun Placeable.yOffsetToCenter(): Int {
                    return (myHeight - height) / 2
                }

                val leadingPlaceable = measurables[LeadingId].measure(
                    Constraints(
                        0,
                        remainWidth,
                        constraints.minHeight,
                        constraints.maxHeight
                    )
                ).also {
//                    println("leading size = ${it.width} * ${it.height}, ${it.measuredWidth} * ${it.measuredHeight}")
                    consume(it.width)
                    enlarge(it)
                }

                var remainMinWidth = maxOf(0, constraints.minWidth - consumedWidth)
                var remainMaxWidth = remainWidth

                val textFieldPlaceable = measurables[TextFieldId].measure(
                    Constraints(
                        remainMinWidth,
                        remainMaxWidth,
                        constraints.minHeight,
                        constraints.maxHeight
                    )
                ).also {
                    enlarge(it)
                }

                val placeholderPlaceable = measurables[PlaceholderId].measure(
                    Constraints(
                        remainMinWidth,
                        remainMaxWidth,
                        constraints.minHeight,
                        constraints.maxHeight
                    )
                ).also {
                    enlarge(it)
                }

                consume(maxOf(textFieldPlaceable.width, placeholderPlaceable.height))

                return layout(consumedWidth, myHeight) {
                    leadingPlaceable.placeRelative(0, leadingPlaceable.yOffsetToCenter())
                    textFieldPlaceable.placeRelative(leadingPlaceable.width, textFieldPlaceable.yOffsetToCenter())
                    placeholderPlaceable.placeRelative(leadingPlaceable.width, placeholderPlaceable.yOffsetToCenter())
                }
            }
        }
    )
}

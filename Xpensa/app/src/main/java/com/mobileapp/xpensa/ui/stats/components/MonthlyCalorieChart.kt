package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.DayKcalResponse
import kotlin.math.max
import androidx.compose.runtime.LaunchedEffect

@Composable
fun MonthlyCalorieChart(
    days: List<DayKcalResponse>,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val orderedDays = days.sortedBy { it.date }
    var selectedDay by remember {
        mutableStateOf<DayKcalResponse?>(null)
    }

    val scrollState = rememberScrollState()

    val maxKcal = max(
        orderedDays.maxOfOrNull {
            it.kcal.toFloatOrNull() ?: 0f
        } ?: 0f,
        threshold.toFloat()
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor =
        MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val secondaryContainerColor =
        MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceVariant =
        MaterialTheme.colorScheme.onSurfaceVariant

    /*
     * Each day gets 44.dp of horizontal space.
     *
     * This gives approximately 10 bars on screen.
     */
    val barSlotWidth = 44.dp

    /*
     * Make the complete chart wide enough for every day.
     *
     * If there are fewer than 10 days, we still use
     * enough width for approximately 10 bars.
     */
    val chartWidth = if (orderedDays.size <= 10) {
        10 * barSlotWidth.value
    } else {
        orderedDays.size * barSlotWidth.value
    }.dp

    LaunchedEffect(days.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monthly calories",
                style = MaterialTheme.typography.titleMedium
            )

            /*
             * Show information about the selected day.
             */
            selectedDay?.let { day ->
                Surface(
                    color = secondaryContainerColor,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDate(day.date),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "${day.kcal} kcal",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            /*
             * The chart and the dates are inside the same
             * horizontally scrollable area.
             *
             * This means the bars and their dates always
             * move together.
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(chartWidth)
                        .height(310.dp)
                ) {

                    /*
                     * BAR CHART
                     */
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(265.dp)
                            .pointerInput(orderedDays) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()

                                        val position = event.changes
                                            .firstOrNull()
                                            ?.position
                                            ?: continue

                                        if (orderedDays.isEmpty()) {
                                            continue
                                        }

                                        val index =
                                            (
                                                    position.x /
                                                            barSlotWidth.toPx()
                                                    ).toInt()

                                        if (index in orderedDays.indices) {
                                            selectedDay = orderedDays[index]
                                        }
                                    }
                                }
                            }
                    ) {
                        if (orderedDays.isEmpty() || maxKcal <= 0f) {
                            return@Canvas
                        }

                        val topPadding = 20.dp.toPx()
                        val bottomPadding = 20.dp.toPx()

                        val graphHeight =
                            size.height -
                                    topPadding -
                                    bottomPadding

                        val slotWidth =
                            barSlotWidth.toPx()

                        /*
                         * Width of the actual bar.
                         *
                         * The remaining space in each slot
                         * creates a small gap between bars.
                         */
                        val barWidth = 30.dp.toPx()

                        fun yForKcal(kcal: Float): Float {
                            return topPadding +
                                    graphHeight -
                                    (kcal / maxKcal) * graphHeight
                        }

                        /*
                         * Calorie limit line.
                         */
                        val thresholdY =
                            yForKcal(threshold.toFloat())

                        drawLine(
                            color = errorColor,
                            start = Offset(
                                0f,
                                thresholdY
                            ),
                            end = Offset(
                                size.width,
                                thresholdY
                            ),
                            strokeWidth = 2.dp.toPx()
                        )

                        /*
                         * Draw every day's bar.
                         */
                        orderedDays.forEachIndexed { index, day ->

                            val kcal =
                                day.kcal.toFloatOrNull() ?: 0f

                            val barHeight =
                                (kcal / maxKcal) * graphHeight

                            val x =
                                index * slotWidth +
                                        (slotWidth - barWidth) / 2

                            val y =
                                topPadding +
                                        graphHeight -
                                        barHeight

                            val isSelected =
                                selectedDay?.date == day.date

                            val isOverLimit =
                                kcal > threshold

                            val barColor = when {
                                isSelected -> primaryColor
                                isOverLimit -> errorColor
                                else -> primaryContainerColor
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(
                                    x,
                                    y
                                ),
                                size = Size(
                                    barWidth,
                                    barHeight
                                ),
                                cornerRadius = CornerRadius(
                                    5.dp.toPx(),
                                    5.dp.toPx()
                                )
                            )
                        }
                    }

                    /*
                     * DATE LABELS
                     *
                     * Each date is positioned underneath
                     * its corresponding bar.
                     *
                     * Example:
                     *
                     * 2026-09-10 -> 10/9
                     */
                    orderedDays.forEachIndexed { index, day ->

                        Text(
                            text = formatDate(day.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant,
                            modifier = Modifier
                                .width(barSlotWidth)
                                .offset(
                                    x = barSlotWidth * index,
                                    y = 270.dp
                                )
                        )
                    }
                }
            }

            /*
             * Calorie limit explanation.
             */
            Text(
                text = "Limit: $threshold kcal",
                style = MaterialTheme.typography.labelMedium,
                color = errorColor
            )
        }
    }
}

/*
 * Converts dates to the short format used underneath
 * the bars.
 *
 * Examples:
 *
 * 2026-09-10 -> 10/9
 * 2026-01-05 -> 5/1
 * 10-9-2026  -> 10/9
 */
private fun formatDate(date: String): String {

    val parts = date.split("-")

    if (parts.size != 3) {
        return date
    }

    /*
     * API format:
     *
     * YYYY-MM-DD
     */
    if (parts[0].length == 4) {

        val month = parts[1].toIntOrNull()
        val day = parts[2].toIntOrNull()

        if (month != null && day != null) {
            return "$day/$month"
        }
    }

    /*
     * Also support:
     *
     * DD-MM-YYYY
     */
    val day = parts[0].toIntOrNull()
    val month = parts[1].toIntOrNull()

    if (day != null && month != null) {
        return "$day/$month"
    }

    return date
}

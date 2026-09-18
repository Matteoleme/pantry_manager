package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.DayKcalResponse
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.background

@Composable
fun MonthlyCalorieChart(
    days: List<DayKcalResponse>,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember {
        mutableStateOf<DayKcalResponse?>(null)
    }

    val scrollState = rememberScrollState()

    // Keep the days in chronological order.
    // The scroll position will start at the newest days.
    val orderedDays = days.sortedBy { it.date }

    val maxKcal = max(
        orderedDays.maxOfOrNull {
            it.kcal.toFloatOrNull() ?: 0f
        } ?: 0f,
        threshold.toFloat()
    )

    val withinLimitCount = orderedDays.count {
        (it.kcal.toFloatOrNull() ?: 0f) <= threshold
    }

    val exceededCount = orderedDays.count {
        (it.kcal.toFloatOrNull() ?: 0f) > threshold
    }

    // Colors
    val withinLimitColor = Color(0xFF00897B)
    val exceededColor = Color(0xFFE53935)
    val selectedColor = MaterialTheme.colorScheme.primary

    val backgroundBarColor =
        MaterialTheme.colorScheme.surfaceVariant

    val textColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    // Width occupied by one day.
    // Smaller bars = more days visible at once.
    val barSlotWidth = 64.dp

    val barWidth = 20.dp

    val chartWidth = max(
        10 * barSlotWidth.value,
        orderedDays.size * barSlotWidth.value
    ).dp

    /*
     * When the chart opens, move the horizontal scroll
     * all the way to the right so the newest days are visible.
     */
    LaunchedEffect(orderedDays.size) {
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

            // -------------------------------------------------
            // TITLE + LIMIT
            // -------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly calories",
                    style = MaterialTheme.typography.titleMedium
                )

                Surface(
                    color = exceededColor.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Limit: $threshold kcal",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        color = exceededColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // -------------------------------------------------
            // SELECTED DAY
            // -------------------------------------------------

            selectedDay?.let { day ->

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDate(day.date),
                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "${day.kcal} kcal",
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // -------------------------------------------------
            // CHART
            // -------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {

                Box(
                    modifier = Modifier
                        .width(chartWidth)
                        .height(270.dp)
                ) {

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .pointerInput(orderedDays) {

                                detectTapGestures { position ->

                                    if (orderedDays.isEmpty()) {
                                        return@detectTapGestures
                                    }

                                    val slotWidth =
                                        barSlotWidth.toPx()

                                    val index =
                                        (
                                                position.x / slotWidth
                                                ).toInt()

                                    if (index in orderedDays.indices) {
                                        selectedDay =
                                            orderedDays[index]
                                    }
                                }
                            }
                    ) {

                        if (
                            orderedDays.isEmpty() ||
                            maxKcal <= 0f
                        ) {
                            return@Canvas
                        }

                        val topPadding = 10.dp.toPx()
                        val bottomPadding = 15.dp.toPx()

                        val graphHeight =
                            size.height -
                                    topPadding -
                                    bottomPadding

                        val slotWidth =
                            barSlotWidth.toPx()

                        val actualBarWidth =
                            barWidth.toPx()

                        // Convert kcal into a Y position.
                        fun yForKcal(kcal: Float): Float {
                            return topPadding +
                                    graphHeight -
                                    (
                                            kcal / maxKcal
                                            ) * graphHeight
                        }

                        // -----------------------------------------
                        // THRESHOLD LINE
                        // -----------------------------------------

                        val thresholdY =
                            yForKcal(threshold.toFloat())

                        drawLine(
                            color = exceededColor,
                            start = Offset(
                                0f,
                                thresholdY
                            ),
                            end = Offset(
                                size.width,
                                thresholdY
                            ),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        10.dp.toPx(),
                                        7.dp.toPx()
                                    ),
                                    0f
                                )
                        )

                        // -----------------------------------------
                        // BARS
                        // -----------------------------------------

                        orderedDays.forEachIndexed {
                                index,
                                day ->

                            val kcal =
                                day.kcal
                                    .toFloatOrNull()
                                    ?: 0f

                            val barHeight =
                                (kcal / maxKcal) *
                                        graphHeight

                            val x =
                                index * slotWidth +
                                        (
                                                slotWidth -
                                                        actualBarWidth
                                                ) / 2f

                            val y =
                                topPadding +
                                        graphHeight -
                                        barHeight

                            val isSelected =
                                selectedDay?.date ==
                                        day.date

                            val isExceeded =
                                kcal > threshold

                            val barColor = when {
                                isSelected ->
                                    selectedColor

                                isExceeded ->
                                    exceededColor

                                else ->
                                    withinLimitColor
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(
                                    x,
                                    y
                                ),
                                size = Size(
                                    actualBarWidth,
                                    barHeight
                                ),
                                cornerRadius =
                                    CornerRadius(
                                        6.dp.toPx(),
                                        6.dp.toPx()
                                    )
                            )
                        }
                    }

                    // -------------------------------------------------
                    // DATE LABELS
                    // -------------------------------------------------

                    orderedDays.forEachIndexed { index, day ->

                        Text(
                            text = formatDate(day.date),
                            style = MaterialTheme
                                .typography
                                .labelSmall,
                            color = textColor,
                            modifier = Modifier
                                .width(barSlotWidth)
                                .offset(
                                    x = barSlotWidth * index,
                                    y = 225.dp
                                )
                        )
                    }
                }
            }

            // -------------------------------------------------
            // LEGEND
            // -------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                LegendItem(
                    color = withinLimitColor,
                    text =
                        "Within Limit (${withinLimitCount} days)"
                )

                LegendItem(
                    color = exceededColor,
                    text =
                        "Exceeded (${exceededCount} days)"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(date: String): String {

    val parts = date.split("-")

    if (parts.size != 3) {
        return date
    }

    // API format: YYYY-MM-DD
    if (parts[0].length == 4) {

        val month =
            parts[1].toIntOrNull()

        val day =
            parts[2].toIntOrNull()

        if (
            month != null &&
            day != null
        ) {
            return "$day/$month"
        }
    }

    // Also support DD-MM-YYYY
    val day =
        parts[0].toIntOrNull()

    val month =
        parts[1].toIntOrNull()

    if (
        day != null &&
        month != null
    ) {
        return "$day/$month"
    }

    return date
}
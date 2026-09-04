package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.DayKcalResponse
import kotlin.math.max

@Composable
fun MonthlyCalorieChart(
    days: List<DayKcalResponse>,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember {
        mutableStateOf<DayKcalResponse?>(null)
    }

    val maxKcal = max(
        days.maxOfOrNull {
            it.kcal.toFloatOrNull() ?: 0f
        } ?: 0f,
        threshold.toFloat()
    )

    // Read theme colors here, inside the Composable.
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor =
        MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val secondaryContainerColor =
        MaterialTheme.colorScheme.secondaryContainer

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

            // Selected day information
            selectedDay?.let { day ->
                Surface(
                    color = secondaryContainerColor,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "${day.kcal} kcal",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(days) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                val position = event.changes
                                    .firstOrNull()
                                    ?.position
                                    ?: continue

                                if (days.isEmpty()) {
                                    continue
                                }

                                val horizontalPadding = 12.dp.toPx()

                                val availableWidth =
                                    size.width -
                                            horizontalPadding * 2

                                val slotWidth =
                                    availableWidth / days.size

                                val index =
                                    ((position.x -
                                            horizontalPadding) /
                                            slotWidth)
                                        .toInt()

                                if (index in days.indices) {
                                    selectedDay = days[index]
                                }
                            }
                        }
                    }
            ) {
                if (days.isEmpty()) {
                    return@Canvas
                }

                val leftPadding = 12.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 20.dp.toPx()
                val bottomPadding = 35.dp.toPx()

                val graphWidth =
                    size.width -
                            leftPadding -
                            rightPadding

                val graphHeight =
                    size.height -
                            topPadding -
                            bottomPadding

                val slotWidth =
                    graphWidth / days.size

                // Width of each bar.
                // Smaller percentage = more space between bars.
                val barWidth =
                    (slotWidth * 0.65f)
                        .coerceAtLeast(2.dp.toPx())

                fun yForKcal(kcal: Float): Float {
                    return topPadding +
                            graphHeight -
                            (kcal / maxKcal) * graphHeight
                }

                // -----------------------------
                // Threshold line
                // -----------------------------

                val thresholdY =
                    yForKcal(threshold.toFloat())

                drawLine(
                    color = errorColor,
                    start = Offset(
                        leftPadding,
                        thresholdY
                    ),
                    end = Offset(
                        size.width - rightPadding,
                        thresholdY
                    ),
                    strokeWidth = 2.dp.toPx()
                )

                // -----------------------------
                // Daily bars
                // -----------------------------

                days.forEachIndexed { index, day ->

                    val kcal =
                        day.kcal.toFloatOrNull() ?: 0f

                    val barHeight =
                        (kcal / maxKcal) * graphHeight

                    val x =
                        leftPadding +
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

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = days.firstOrNull()?.date ?: "",
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "Limit: $threshold kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = errorColor
                )

                Text(
                    text = days.lastOrNull()?.date ?: "",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.CategoryStatsResponse
import kotlin.math.max

@Composable
fun DailyCategoryChart(
    categories: List<CategoryStatsResponse>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember {
        mutableStateOf<CategoryStatsResponse?>(null)
    }

    val maxKcal = max(
        categories.maxOfOrNull {
            it.kcal.toFloatOrNull() ?: 0f
        } ?: 0f,
        1f
    )

    // Get MaterialTheme colors HERE, in the @Composable context.
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val outlineColor = MaterialTheme.colorScheme.outline
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

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
                text = "Calories by category",
                style = MaterialTheme.typography.titleMedium
            )

            selectedCategory?.let { category ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = secondaryContainerColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.category,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "${category.kcal} kcal",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .pointerInput(categories) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                val position = event.changes
                                    .firstOrNull()
                                    ?.position
                                    ?: continue

                                if (categories.isEmpty()) {
                                    continue
                                }

                                val barWidth =
                                    size.width / categories.size

                                val index =
                                    (position.x / barWidth).toInt()

                                if (index in categories.indices) {
                                    selectedCategory = categories[index]
                                }
                            }
                        }
                    }
            ) {
                drawDailyBars(
                    categories = categories,
                    maxKcal = maxKcal,
                    selectedCategory = selectedCategory,
                    primaryColor = primaryColor,
                    primaryContainerColor = primaryContainerColor,
                    //outlineColor = outlineColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                categories.forEach {
                    Text(
                        text = it.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDailyBars(
    categories: List<CategoryStatsResponse>,
    maxKcal: Float,
    selectedCategory: CategoryStatsResponse?,
    primaryColor: androidx.compose.ui.graphics.Color,
    primaryContainerColor: androidx.compose.ui.graphics.Color,
    //outlineColor: androidx.compose.ui.graphics.Color
) {
    if (categories.isEmpty()) return

    val horizontalPadding = 16.dp.toPx()
    val bottomPadding = 35.dp.toPx()

    val chartHeight = size.height - bottomPadding

    val availableWidth =
        size.width - horizontalPadding * 2

    val slotWidth =
        availableWidth / categories.size

    val barWidth =
        slotWidth * 0.55f

    categories.forEachIndexed { index, category ->

        val kcal =
            category.kcal.toFloatOrNull() ?: 0f

        val barHeight =
            (kcal / maxKcal) *
                    (chartHeight - 20.dp.toPx())

        val left =
            horizontalPadding +
                    index * slotWidth +
                    (slotWidth - barWidth) / 2

        val top =
            chartHeight - barHeight

        val isSelected =
            selectedCategory?.category == category.category

        drawRoundRect(
            color = if (isSelected) {
                primaryColor
            } else {
                primaryContainerColor
            },
            topLeft = Offset(left, top),
            size = Size(
                barWidth,
                barHeight
            ),
            cornerRadius = CornerRadius(
                10.dp.toPx(),
                10.dp.toPx()
            )
        )

        /*
        drawRoundRect(
            color = outlineColor.copy(alpha = 0.25f),
            topLeft = Offset(left, top),
            size = Size(
                barWidth,
                barHeight
            ),
            cornerRadius = CornerRadius(
                10.dp.toPx(),
                10.dp.toPx()
            ),
            style = Stroke(
                width = 1.dp.toPx()
            )
        )

         */
    }
}
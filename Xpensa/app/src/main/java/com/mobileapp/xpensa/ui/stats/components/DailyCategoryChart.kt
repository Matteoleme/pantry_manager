package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.CategoryStatsResponse
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.background

@Composable
fun DailyCategoryChart(
    categories: List<CategoryStatsResponse>,
    modifier: Modifier = Modifier
) {
    val categoryColors = listOf(
        Color(0xFF00897B), // Teal
        Color(0xFF12B886), // Green
        Color(0xFF20C997), // Light green
        Color(0xFFFFA000), // Orange
        Color(0xFFE53935)  // Red
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium
            )

            categories.forEachIndexed { index, category ->

                val percentage = parsePercentage(category.percentage)

                val progress = min(
                    max(percentage / 100f, 0f),
                    1f
                )

                val barColor =
                    categoryColors[index % categoryColors.size]

                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    // Category name + percentage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.category,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "${percentage.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = barColor
                        )
                    }

                    // Background + progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    ) {
                        // Background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                        )

                        // Progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

private fun parsePercentage(value: String): Float {
    return value
        .replace("%", "")
        .trim()
        .toFloatOrNull()
        ?: 0f
}
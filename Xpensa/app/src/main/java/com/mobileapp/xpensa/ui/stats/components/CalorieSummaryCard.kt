package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.min


//creates a card with the actual kcal and the threshold and a progress bar related to the threshold for the daily kcal
@Composable
fun CalorieSummaryCard(
    totalKcal: Float,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (threshold > 0) {
        min(totalKcal / threshold.toFloat(), 1f)
    } else {
        0f
    }

    val percentage = if (threshold > 0) {
        ((totalKcal / threshold.toFloat()) * 100).toInt()
    } else {
        0
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Calories",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${totalKcal.toInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "consumed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = "$threshold kcal",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "daily limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Text(
                text = "$percentage% of daily limit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
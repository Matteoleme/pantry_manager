package com.mobileapp.xpensa.ui.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.ui.stats.StatisticsPeriod

//creates the period selector (day/month)
@Composable
fun StatsPeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = selectedPeriod == StatisticsPeriod.DAY,
            onClick = {
                onPeriodSelected(StatisticsPeriod.DAY)
            },
            label = {
                Text("Today")
            },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors()
        )

        FilterChip(
            selected = selectedPeriod == StatisticsPeriod.MONTH,
            onClick = {
                onPeriodSelected(StatisticsPeriod.MONTH)
            },
            label = {
                Text("Past 30 Days")
            },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors()
        )
    }
}
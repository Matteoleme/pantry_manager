package com.mobileapp.xpensa.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobileapp.xpensa.ui.stats.components.CalorieSummaryCard
import com.mobileapp.xpensa.ui.stats.components.DailyCategoryChart
import com.mobileapp.xpensa.ui.stats.components.MonthlyCalorieChart
import com.mobileapp.xpensa.ui.stats.components.StatsPeriodSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Statistics")
                },
                actions = {
                    IconButton(
                        onClick = viewModel::retry
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh statistics"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                StatsPeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::selectPeriod,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()

                        Text(
                            text = "Loading statistics...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = viewModel::retry
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            when (uiState.selectedPeriod) {

                StatisticsPeriod.DAY -> {
                    val stats = uiState.dayStats

                    if (!uiState.isLoading && stats != null) {

                        item {
                            Text(
                                text = stats.date,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        item {
                            CalorieSummaryCard(
                                totalKcal =
                                    stats.totalKcal.toFloatOrNull()
                                        ?: 0f,
                                threshold = stats.threshold
                            )
                        }

                        item {
                            DailyCategoryChart(
                                categories = stats.categories
                            )
                        }

                        item {
                            Text(
                                text = "Category breakdown",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        items(stats.categories) { category ->
                            CategoryStatRow(
                                category = category.category,
                                kcal = category.kcal,
                                percentage = category.percentage
                            )
                        }
                    }
                }

                StatisticsPeriod.MONTH -> {
                    val stats = uiState.monthStats

                    if (!uiState.isLoading && stats != null) {

                        item {
                            Text(
                                text = "${stats.startDate} → ${stats.endDate}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        item {
                            MonthlyCalorieChart(
                                days = stats.days,
                                threshold = stats.threshold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryStatRow(
    category: String,
    kcal: String,
    percentage: String
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$kcal kcal",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
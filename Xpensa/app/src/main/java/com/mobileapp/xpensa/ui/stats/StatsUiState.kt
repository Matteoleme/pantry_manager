package com.mobileapp.xpensa.ui.stats

import com.mobileapp.xpensa.data.api.DayStatsResponse
import com.mobileapp.xpensa.data.api.MonthStatsResponse

//possible stats periods (day/month)
enum class StatisticsPeriod {
    DAY,
    MONTH
}

// state of the statistics screen
data class StatsUiState(
    // stats period selected by the user
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.DAY,
    //contains response from get/stats/day from server
    val dayStats: DayStatsResponse? = null,
    //contains response from get/stats/month from server
    val monthStats: MonthStatsResponse? = null,

    val isLoading: Boolean = false,

    val error: String? = null
)
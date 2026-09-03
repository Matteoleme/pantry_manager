package com.mobileapp.xpensa.data.api

import com.mobileapp.xpensa.data.Category
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayStatsResponse(
    val date: String,

    @SerialName("total_kcal")
    val totalKcal: String,

    val threshold: Int,

    val categories: List<CategoryStatsResponse>

)

@Serializable
data class CategoryStatsResponse(

    val category: String,
    val kcal: String,
    val percentage: String
)

@Serializable
data class MonthStatsResponse(
    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    val threshold: Int,

    val days: List<DayKcalResponse>
)

@Serializable
data class DayKcalResponse(
    val date: String,
    val kcal: String
)
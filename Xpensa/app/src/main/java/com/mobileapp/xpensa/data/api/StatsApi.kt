package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.GET

interface StatsApi {

    @GET("stats/day")
    suspend fun getDayStats(): Response<DayStatsResponse>

    @GET("stats/month")
    suspend fun getMonthStats(): Response<MonthStatsResponse>
}
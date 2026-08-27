package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.GET

interface PantryApi {
    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

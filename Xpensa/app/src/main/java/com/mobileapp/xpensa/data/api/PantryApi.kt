package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PantryApi {
    @GET("pantry")
    suspend fun getPantry(): Response<PantryResponse>

    @POST("products")
    suspend fun createProduct(@Body product: ProductCreate): Response<ProductResponse>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

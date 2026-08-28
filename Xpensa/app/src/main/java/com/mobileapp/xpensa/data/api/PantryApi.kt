package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PantryApi {
    @GET("pantry")
    suspend fun getPantry(): Response<PantryResponse>

    @GET("all_products")
    suspend fun getAllProducts(): Response<List<ProductResponse>>

    @POST("products")
    suspend fun createProduct(@Body product: ProductCreate): Response<ProductResponse>

    @POST("categories")
    suspend fun createCategory(@Body category: CategoryCreate): Response<CategoryResponse>

    @POST("eat")
    suspend fun eat(@Body event: EventCreate): Response<PantryResponse>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

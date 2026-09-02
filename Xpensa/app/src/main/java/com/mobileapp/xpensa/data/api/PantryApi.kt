package com.mobileapp.xpensa.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PantryApi {
    @GET("pantry")
    suspend fun getPantry(): Response<PantryResponse>

    @GET("all_products")
    suspend fun getAllProducts(): Response<List<ProductResponse>>

    @POST("add_product")
    suspend fun createProduct(@Body product: ProductCreate): Response<ProductResponse>

    @POST("add_category")
    suspend fun createCategory(@Body category: CategoryCreate): Response<CategoryResponse>

    @POST("update-threshold")
    suspend fun updateThreshold(@Body request: ThresholdUpdate): Response<PantryResponse>

    @POST("eat")
    suspend fun eat(@Body event: EventCreate): Response<PantryResponse>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryResponse>>

    @DELETE("delete_category/{category_name}")
    suspend fun deleteCategory(@Path("category_name") categoryName: String): Response<Unit>

    @DELETE("delete_product/{product_id}")
    suspend fun deleteProduct(@Path("product_id") productId: Int): Response<Unit>

    @POST("products/{product_id}/quantity")
    suspend fun updateProductQuantity(
        @Path("product_id") productId: Int,
        @Body request: QuantityUpdate
    ): Response<Unit>

    @POST("pantry-share-requests")
    suspend fun createShareRequest(@Body request: PantryShareRequestCreate): Response<Unit>

    @GET("retrieve-pantry-share-requests")
    suspend fun getShareRequests(): Response<ResponseBody>

    @POST("pantry-share-requests/{request_id}/approve")
    suspend fun approveShareRequest(@Path("request_id") requestId: Int): Response<Unit>

    @POST("pantry-share-requests/{request_id}/reject")
    suspend fun rejectShareRequest(@Path("request_id") requestId: Int): Response<Unit>

    @POST("pantry/leave")
    suspend fun leavePantry(): Response<Unit>

    @POST("pantry/remove/{username}")
    suspend fun removeUser(@Path("username") username: String): Response<Unit>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

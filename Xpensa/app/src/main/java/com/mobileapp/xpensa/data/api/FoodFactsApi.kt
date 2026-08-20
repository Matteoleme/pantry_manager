package com.mobileapp.xpensa.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class FoodFactsResponse(
    val product: Product? = null,
    val status: Int? = null,
    val code: String? = null
)

@Serializable
data class Product(
    @SerialName("product_name") val productName: String? = null,
    val nutriments: Nutriments? = null,
    val categories: String? = null
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null
)

interface FoodFactsApi {
    @GET("product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): FoodFactsResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/api/v2/"
    }
}

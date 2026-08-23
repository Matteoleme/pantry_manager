package com.mobileapp.xpensa.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

@Serializable
data class NominatimSearchResult(
    @SerialName("place_id") val placeId: Long,
    val lat: String,
    val lon: String,
    val name: String? = null,
    @SerialName("display_name") val displayName: String,
    val address: NominatimAddress? = null
)

@Serializable
data class NominatimAddress(
    val shop: String? = null,
    val supermarket: String? = null,
    val amenity: String? = null,
    val road: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postcode: String? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countryCodes: String = "it",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Header("User-Agent") userAgent: String = "Xpensa-PantryManager/1.0"
    ): List<NominatimSearchResult>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}

package com.mobileapp.xpensa.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    @SerialName("EAN") val ean: String?,
    val unit: String,
    val quantity: String,
    val category: String,
    val kcal: Int
)

@Serializable
data class QuantityUpdate(
    val quantity: Double
)

@Serializable
data class ProductCreate(
    val name: String,
    @SerialName("EAN") val ean: String?,
    val unit: String,
    val quantity: String,
    val category: String,
    val kcal: Int
)

@Serializable
data class CategoryCreate(
    val name: String
)

@Serializable
data class EventProduct(
    @SerialName("product_id") val productId: Int,
    val quantity: String
)

@Serializable
data class EventCreate(
    val products: List<EventProduct>
)

@Serializable
data class ThresholdUpdate(
    @SerialName("kcal_threshold") val kcalThreshold: Int
)

@Serializable
data class PantryUser(
    val username: String
)

@Serializable
data class PantryResponse(
    val id: Int,
    val creator: String,
    @SerialName("kcal_threshold") val kcalThreshold: Int,
    val users: List<PantryUser> = emptyList()
)

@Serializable
data class PantryShareRequestCreate(
    val username: String
)

@Serializable
data class PantryShareRequestResponse(
    val id: Int = 0,
    @SerialName("requester_username") val requesterUsername: String? = null,
    @SerialName("requester_name") val requesterName: String? = null,
    val username: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val displayUsername: String
        get() = requesterUsername ?: username ?: requesterName ?: "Utente"
}



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
data class PantryResponse(
    val id: Int,
    val creator: String,
    @SerialName("kcal_threshold") val kcalThreshold: Int
)

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
data class PantryResponse(
    val id: Int,
    val creator: Int,
    @SerialName("kcal_threshold") val kcalThreshold: Int,
    val products: List<ProductResponse>
)

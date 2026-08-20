package com.mobileapp.xpensa.data

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val displayName: String) {
    CARNE("Carne"),
    PESCE("Pesce"),
    VERDURE("Verdure"),
    FRUTTA("Frutta"),
    LATTICINI("Latticini"),
    BEVANDE("Bevande"),
    ALTRO("Altro")
}

@Serializable
enum class MeasurementUnit(val symbol: String) {
    KG("Kg"),
    L("L"),
    UNIT("unità")
}

@Serializable
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: MeasurementUnit,
    val isFavorite: Boolean = false,
    val kcal: Int? = null,
    val ean: String? = null
)

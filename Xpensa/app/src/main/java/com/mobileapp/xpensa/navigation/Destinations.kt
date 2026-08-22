package com.mobileapp.xpensa.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface PantryDestination : NavKey {
    @Serializable
    data object Home : PantryDestination
    
    @Serializable
    data object Trends : PantryDestination
    
    @Serializable
    data object Favorites : PantryDestination
    
    @Serializable
    data object Consuma : PantryDestination
    
    @Serializable
    data object NewProduct : PantryDestination
    @Serializable
    data object Scanner : PantryDestination

    @Serializable
    data object Stores : PantryDestination

    @Serializable
    data class EditProduct(val productId: String) : PantryDestination
}

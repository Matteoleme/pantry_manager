package com.mobileapp.xpensa.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileapp.xpensa.data.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pantry_prefs")

class DataStoreManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val PRODUCTS_KEY = stringPreferencesKey("products_json")
        val CATEGORIES_KEY = stringPreferencesKey("categories_json")
        val DAILY_CALORIES_KEY = intPreferencesKey("daily_calories")
        val LAST_CALORIES_DATE_KEY = stringPreferencesKey("last_calories_date")
        val SHOW_OUT_OF_STOCK_KEY = booleanPreferencesKey("show_out_of_stock")
        val LAST_SYNC_TIMESTAMP_KEY = longPreferencesKey("last_sync_timestamp")
    }

    val productsFlow: Flow<List<Product>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[PRODUCTS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<Product>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val categoriesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[CATEGORIES_KEY] ?: "[]"
        try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val dailyCaloriesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_CALORIES_KEY] ?: 0
    }

    val lastCaloriesDateFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_CALORIES_DATE_KEY]
    }

    val showOutOfStockFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_OUT_OF_STOCK_KEY] ?: false
    }

    val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIMESTAMP_KEY] ?: 0L
    }

    suspend fun saveProducts(products: List<Product>) {
        context.dataStore.edit { preferences ->
            preferences[PRODUCTS_KEY] = json.encodeToString(products)
        }
    }

    suspend fun saveCategories(categories: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[CATEGORIES_KEY] = json.encodeToString(categories)
        }
    }

    suspend fun saveDailyCalories(calories: Int, date: String) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_CALORIES_KEY] = calories
            preferences[LAST_CALORIES_DATE_KEY] = date
        }
    }

    suspend fun saveShowOutOfStock(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_OUT_OF_STOCK_KEY] = show
        }
    }

    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP_KEY] = timestamp
        }
    }
}

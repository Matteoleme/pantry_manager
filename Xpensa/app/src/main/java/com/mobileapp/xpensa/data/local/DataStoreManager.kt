package com.mobileapp.xpensa.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.data.Store
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
        val STORES_KEY = stringPreferencesKey("stores_json")
        val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")

        val MOCK_STORES = listOf(
            Store(
                id = "1",
                name = "Todis",
                address = "Via San Gordiano 7, Civitavecchia, Rome, Italy",
                latitude = 42.0737,
                longitude = 11.8160
            ),
            Store(
                id = "2",
                name = "Conad",
                address = "Via Monsignor Giuseppe Papacchini, Civitavecchia, Rome, Italy",
                latitude = 42.0865,
                longitude = 11.8022
            )
        )
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

    val fcmTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN_KEY]
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    val tokenTypeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_TYPE_KEY]
    }

    val storesFlow: Flow<List<Store>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[STORES_KEY]
        if (jsonString == null) {
            MOCK_STORES
        } else {
            try {
                json.decodeFromString<List<Store>>(jsonString)
            } catch (e: Exception) {
                MOCK_STORES
            }
        }
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

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    suspend fun saveAuthToken(accessToken: String, refreshToken: String, type: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[TOKEN_TYPE_KEY] = type
        }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
            // Possiamo anche pulire prodotti e categorie se vogliamo una logout pulita
            preferences.remove(PRODUCTS_KEY)
            preferences.remove(CATEGORIES_KEY)
        }
    }

    suspend fun saveStores(stores: List<Store>) {
        context.dataStore.edit { preferences ->
            preferences[STORES_KEY] = json.encodeToString(stores)
        }
    }
}

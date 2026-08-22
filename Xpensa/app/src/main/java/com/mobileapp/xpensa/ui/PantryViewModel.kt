package com.mobileapp.xpensa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.data.api.FoodFactsApi
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate

class PantryViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val dataStoreManager = DataStoreManager(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val api: FoodFactsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(FoodFactsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FoodFactsApi::class.java)
    }

    init {
        loadDataFromStorage()
    }

    private fun loadDataFromStorage() {
        viewModelScope.launch {
            // Leggiamo tutto in un colpo solo per efficienza e sicurezza
            try {
                val products = dataStoreManager.productsFlow.first()
                val categories = dataStoreManager.categoriesFlow.first()
                val storedDailyCalories = dataStoreManager.dailyCaloriesFlow.first()
                val lastDate = dataStoreManager.lastCaloriesDateFlow.first()
                val showOutOfStock = dataStoreManager.showOutOfStockFlow.first()

                val today = LocalDate.now().toString()
                
                val dailyCalories = if (lastDate != today) 0 else storedDailyCalories

                val finalCategories = if (categories.isEmpty()) {
                    Category.entries.map { it.displayName }
                } else {
                    categories
                }

                _uiState.update { state ->
                    state.copy(
                        products = products,
                        allCategories = finalCategories,
                        dailyCalories = dailyCalories,
                        showOnlyOutOfStock = showOutOfStock
                    )
                }

                if (lastDate != today) {
                    dataStoreManager.saveDailyCalories(0, today)
                }
                
                if (categories.isEmpty()) {
                    dataStoreManager.saveCategories(finalCategories)
                }
            } catch (e: Exception) {
                // In caso di errore estremo, carichiamo almeno le categorie di default
                _uiState.update { state ->
                    state.copy(allCategories = Category.entries.map { it.displayName })
                }
            }
        }
    }

    fun addProduct(product: Product) {
        _uiState.update { state ->
            val newProducts = state.products + product
            viewModelScope.launch { dataStoreManager.saveProducts(newProducts) }
            state.copy(products = newProducts)
        }
    }

    fun updateProduct(updatedProduct: Product) {
        _uiState.update { state ->
            val newProducts = state.products.map { 
                if (it.id == updatedProduct.id) updatedProduct else it 
            }
            viewModelScope.launch { dataStoreManager.saveProducts(newProducts) }
            state.copy(products = newProducts)
        }
    }

    fun consumeProducts(consumptions: Map<String, Double>): Int {
        var mealKcal = 0
        val today = LocalDate.now().toString()
        
        _uiState.update { state ->
            val updatedProducts = state.products.map { product ->
                val consumeQty = consumptions[product.id] ?: 0.0
                if (consumeQty > 0) {
                    val effectiveConsumeQty = minOf(product.quantity, consumeQty)
                    
                    val kcalContribution = product.kcal?.let { kcal ->
                        if (product.unit == MeasurementUnit.UNIT) {
                            (kcal * effectiveConsumeQty).toInt()
                        } else {
                            (kcal * effectiveConsumeQty * 10).toInt()
                        }
                    } ?: 0
                    mealKcal += kcalContribution
                    product.copy(quantity = maxOf(0.0, product.quantity - effectiveConsumeQty))
                } else {
                    product
                }
            }
            
            val newDailyCalories = state.dailyCalories + mealKcal
            
            viewModelScope.launch {
                dataStoreManager.saveProducts(updatedProducts)
                dataStoreManager.saveDailyCalories(newDailyCalories, today)
            }

            state.copy(
                products = updatedProducts,
                dailyCalories = newDailyCalories
            )
        }
        return mealKcal
    }

    fun addCategory(categoryName: String) {
        _uiState.update { state ->
            if (categoryName.isNotBlank() && !state.allCategories.contains(categoryName)) {
                val newCategories = state.allCategories + categoryName
                viewModelScope.launch { dataStoreManager.saveCategories(newCategories) }
                state.copy(allCategories = newCategories)
            } else {
                state
            }
        }
    }

    fun incrementQuantity(productId: String) {
        _uiState.update { state ->
            val newProducts = state.products.map { product ->
                if (product.id == productId) {
                    val delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                    product.copy(quantity = product.quantity + delta)
                } else {
                    product
                }
            }
            viewModelScope.launch { dataStoreManager.saveProducts(newProducts) }
            state.copy(products = newProducts)
        }
    }

    fun decrementQuantity(productId: String) {
        _uiState.update { state ->
            val newProducts = state.products.map { product ->
                if (product.id == productId && product.quantity > 0) {
                    val delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                    product.copy(quantity = maxOf(0.0, product.quantity - delta))
                } else {
                    product
                }
            }
            viewModelScope.launch { dataStoreManager.saveProducts(newProducts) }
            state.copy(products = newProducts)
        }
    }

    fun updateQuantity(productId: String, newQuantity: Double) {
        _uiState.update { state ->
            val newProducts = state.products.map { product ->
                if (product.id == productId) {
                    product.copy(quantity = maxOf(0.0, newQuantity))
                } else {
                    product
                }
            }
            viewModelScope.launch { dataStoreManager.saveProducts(newProducts) }
            state.copy(products = newProducts)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun selectProduct(product: Product?) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun setShowConsumptionModal(show: Boolean) {
        _uiState.update { it.copy(showConsumptionModal = show) }
    }

    fun setShowStatsModal(show: Boolean) {
        _uiState.update { it.copy(showStatsModal = show) }
    }

    fun toggleCategory(category: String) {
        _uiState.update { state ->
            val newCategories = if (state.selectedCategories.contains(category)) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = newCategories, showOnlyOutOfStock = false)
        }
    }

    fun clearCategoryFilters() {
        _uiState.update { it.copy(selectedCategories = emptySet(), showOnlyOutOfStock = false) }
    }

    fun toggleConsumptionCategory(category: String) {
        _uiState.update { state ->
            val newCategories = if (state.consumptionSelectedCategories.contains(category)) {
                state.consumptionSelectedCategories - category
            } else {
                state.consumptionSelectedCategories + category
            }
            state.copy(consumptionSelectedCategories = newCategories)
        }
    }

    fun clearConsumptionCategoryFilters() {
        _uiState.update { it.copy(consumptionSelectedCategories = emptySet()) }
    }

    fun toggleOutOfStockFilter() {
        _uiState.update { state ->
            val newState = !state.showOnlyOutOfStock
            viewModelScope.launch { dataStoreManager.saveShowOutOfStock(newState) }
            state.copy(
                showOnlyOutOfStock = newState,
                selectedCategories = if (newState) emptySet() else state.selectedCategories
            )
        }
    }

    fun toggleAllCategories(all: Boolean) {
        _uiState.update { state ->
            val newCategories = if (all) {
                state.allCategories.toSet()
            } else {
                emptySet()
            }
            state.copy(selectedCategories = newCategories)
        }
    }

    fun fetchProductFromEan(ean: String) {
        if (ean.isBlank()) return

        _uiState.update { it.copy(isFetchingProduct = true, fetchError = null, lastScannedProduct = null) }
        
        viewModelScope.launch {
            try {
                val response = api.getProduct(ean)
                if (response.status == 1 && response.product != null) {
                    val p = response.product
                    val scanned = ScannedProduct(
                        name = p.productName ?: "Prodotto sconosciuto",
                        unit = MeasurementUnit.UNIT,
                        category = Category.ALTRO.displayName,
                        kcal = p.nutriments?.energyKcal100g?.toInt()
                    )
                    _uiState.update { it.copy(isFetchingProduct = false, lastScannedProduct = scanned) }
                } else {
                    _uiState.update { it.copy(isFetchingProduct = false, fetchError = "Prodotto non trovato") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingProduct = false, fetchError = "Errore di rete: ${e.localizedMessage}") }
            }
        }
    }

    fun clearFetchState() {
        _uiState.update { it.copy(fetchError = null, lastScannedProduct = null) }
    }

    fun setScannedEan(ean: String) {
        _uiState.update { it.copy(scannedEan = ean) }
    }
}

data class ScannedProduct(
    val name: String,
    val unit: MeasurementUnit,
    val category: String,
    val kcal: Int?
)

data class PantryUiState(
    val products: List<Product> = emptyList(),
    val allCategories: List<String> = emptyList(),
    val selectedProduct: Product? = null,
    val showConsumptionModal: Boolean = false,
    val showStatsModal: Boolean = false,
    val selectedCategories: Set<String> = emptySet(),
    val consumptionSelectedCategories: Set<String> = emptySet(),
    val dailyCalories: Int = 0,
    val showOnlyOutOfStock: Boolean = false,
    val searchQuery: String = "",
    val isFetchingProduct: Boolean = false,
    val fetchError: String? = null,
    val lastScannedProduct: ScannedProduct? = null,
    val scannedEan: String? = null
) {
    val filteredProducts: List<Product>
        get() = products.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true)
            
            val matchesFilters = if (showOnlyOutOfStock) {
                product.quantity <= 0.0
            } else {
                val isAvailable = product.quantity > 0.0
                val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(product.category)
                isAvailable && matchesCategory
            }
            
            matchesSearch && matchesFilters
        }
}

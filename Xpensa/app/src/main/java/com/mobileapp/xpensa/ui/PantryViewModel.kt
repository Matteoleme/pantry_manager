package com.mobileapp.xpensa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.data.api.FoodFactsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID

class PantryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

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
        // Mock data
        _uiState.update { 
            it.copy(
                allCategories = Category.entries.map { c -> c.displayName },
                products = listOf(
                    Product(UUID.randomUUID().toString(), "Prodotto 1", Category.ALTRO.displayName, 5.0, MeasurementUnit.UNIT),
                    Product(UUID.randomUUID().toString(), "Prodotto 2", Category.VERDURE.displayName, 2.5, MeasurementUnit.KG),
                    Product(UUID.randomUUID().toString(), "Latte", Category.LATTICINI.displayName, 1.0, MeasurementUnit.L),
                    Product(UUID.randomUUID().toString(), "Mele", Category.FRUTTA.displayName, 3.2, MeasurementUnit.KG)
                )
            )
        }
    }

    fun addProduct(product: Product) {
        _uiState.update { state ->
            state.copy(products = state.products + product)
        }
    }

    fun updateProduct(updatedProduct: Product) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map { 
                    if (it.id == updatedProduct.id) updatedProduct else it 
                }
            )
        }
    }

    fun consumeProducts(consumptions: Map<String, Double>): Int {
        var totalKcal = 0
        _uiState.update { state ->
            val updatedProducts = state.products.map { product ->
                val consumeQty = consumptions[product.id] ?: 0.0
                if (consumeQty > 0) {
                    val kcalContribution = product.kcal?.let { 
                        // Kcal are usually per 100g or per unit. 
                        // Assuming kcal is per unit or per 100g/ml based on unit
                        if (product.unit == MeasurementUnit.UNIT) {
                            (it * consumeQty).toInt()
                        } else {
                            // If KG or L, assume kcal is per 100g/ml
                            (it * consumeQty * 10).toInt() 
                        }
                    } ?: 0
                    totalKcal += kcalContribution
                    product.copy(quantity = maxOf(0.0, product.quantity - consumeQty))
                } else {
                    product
                }
            }
            state.copy(products = updatedProducts)
        }
        return totalKcal
    }

    fun addCategory(categoryName: String) {
        _uiState.update { state ->
            if (categoryName.isNotBlank() && !state.allCategories.contains(categoryName)) {
                state.copy(allCategories = state.allCategories + categoryName)
            } else {
                state
            }
        }
    }

    fun incrementQuantity(productId: String) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map { product ->
                    if (product.id == productId) {
                        val delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                        product.copy(quantity = product.quantity + delta)
                    } else {
                        product
                    }
                }
            )
        }
    }

    fun decrementQuantity(productId: String) {
        _uiState.update { state ->
            val updatedProducts = state.products.map { product ->
                if (product.id == productId && product.quantity > 0) {
                    val delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                    product.copy(quantity = maxOf(0.0, product.quantity - delta))
                } else {
                    product
                }
            }
            state.copy(products = updatedProducts)
        }
    }

    fun updateQuantity(productId: String, newQuantity: Double) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map { product ->
                    if (product.id == productId) {
                        product.copy(quantity = maxOf(0.0, newQuantity))
                    } else {
                        product
                    }
                }
            )
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
            // Se seleziono una categoria, disattivo il filtro "Finiti"
            state.copy(selectedCategories = newCategories, showOnlyOutOfStock = false)
        }
    }

    fun clearCategoryFilters() {
        _uiState.update { it.copy(selectedCategories = emptySet(), showOnlyOutOfStock = false) }
    }

    fun toggleOutOfStockFilter() {
        _uiState.update { state ->
            val newState = !state.showOnlyOutOfStock
            state.copy(
                showOnlyOutOfStock = newState,
                // Se attivo "Finiti", svuoto le categorie selezionate
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
                        unit = MeasurementUnit.UNIT, // Default to unit
                        category = Category.ALTRO.displayName, // Mapping categories can be complex, default to Altro
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
                // Modalità "Finiti": solo quantità <= 0
                product.quantity <= 0.0
            } else {
                // Modalità standard: escludi esauriti E filtra per categoria se presente
                val isAvailable = product.quantity > 0.0
                val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(product.category)
                isAvailable && matchesCategory
            }
            
            matchesSearch && matchesFilters
        }
}

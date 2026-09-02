package com.mobileapp.xpensa.ui

import android.app.Application
import android.annotation.SuppressLint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.data.Store
import com.mobileapp.xpensa.data.UserLocation
import com.mobileapp.xpensa.data.StoreSearchResult
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.data.api.FoodFactsApi
import com.mobileapp.xpensa.data.api.NominatimApi
import com.mobileapp.xpensa.data.api.PantryApi
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.data.api.CategoryCreate
import com.mobileapp.xpensa.data.api.EventCreate
import com.mobileapp.xpensa.data.api.EventProduct
import com.mobileapp.xpensa.data.api.PantryResponse
import com.mobileapp.xpensa.data.api.ProductCreate
import com.mobileapp.xpensa.data.api.ProductResponse
import com.mobileapp.xpensa.data.api.QuantityUpdate
import com.mobileapp.xpensa.data.api.ThresholdUpdate
import com.mobileapp.xpensa.data.api.PantryShareRequestCreate
import com.mobileapp.xpensa.data.api.PantryShareRequestResponse
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.time.LocalDate
import java.util.*

class PantryViewModel(
    application: Application,
    private val pantryApi: PantryApi
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val dataStoreManager = DataStoreManager(application)
    private val json = Json { ignoreUnknownKeys = true }
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

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

    private val nominatimApi: NominatimApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(NominatimApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NominatimApi::class.java)
    }

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                val localProducts = dataStoreManager.productsFlow.first()
                val storedDailyCalories = dataStoreManager.dailyCaloriesFlow.first()
                val lastDate = dataStoreManager.lastCaloriesDateFlow.first()
                val showOutOfStock = dataStoreManager.showOutOfStockFlow.first()
                val stores = dataStoreManager.storesFlow.first()

                val today = LocalDate.now().toString()
                val dailyCalories = if (lastDate != today) 0 else storedDailyCalories

                // Recuperiamo la dispensa dal backend per info generali
                val pantryResponse = try {
                    pantryApi.getPantry()
                } catch (e: Exception) {
                    android.util.Log.e("PantryViewModel", "Errore chiamata pantry", e)
                    null
                }

                // Recuperiamo i prodotti dal backend (nuovo endpoint dedicato)
                val productsResponse = try {
                    pantryApi.getAllProducts()
                } catch (e: Exception) {
                    android.util.Log.e("PantryViewModel", "Errore chiamata all_products", e)
                    null
                }

                val remoteProducts = if (productsResponse?.isSuccessful == true) {
                    val body = productsResponse.body()
                    android.util.Log.d("PantryViewModel", "Prodotti ricevuti: ${body?.size}")
                    body?.map { mapProductResponse(it) } ?: emptyList()
                } else {
                    android.util.Log.w("PantryViewModel", "Errore fetch prodotti backend. Code: ${productsResponse?.code()}")
                    null
                }

                // Recuperiamo le categorie dal backend
                val categoriesResponse = try {
                    pantryApi.getCategories()
                } catch (e: Exception) {
                    android.util.Log.e("PantryViewModel", "Errore chiamata categorie", e)
                    null
                }

                val finalCategories = if (categoriesResponse?.isSuccessful == true) {
                    val body = categoriesResponse.body()
                    val remoteCategories = body?.map { it.name } ?: emptyList()
                    
                    if (remoteCategories.isNotEmpty()) {
                        remoteCategories
                    } else {
                        Category.entries.map { it.displayName }
                    }
                } else {
                    val localCategories = dataStoreManager.categoriesFlow.first()
                    if (localCategories.isEmpty()) {
                        Category.entries.map { it.displayName }
                    } else {
                        localCategories
                    }
                }

                val finalProducts = remoteProducts ?: localProducts

                _uiState.update { state ->
                    state.copy(
                        products = finalProducts,
                        allCategories = finalCategories,
                        dailyCalories = dailyCalories,
                        showOnlyOutOfStock = showOutOfStock,
                        stores = stores,
                        pantryId = pantryResponse?.body()?.id,
                        pantryCreatorId = pantryResponse?.body()?.creator,
                        kcalThreshold = pantryResponse?.body()?.kcalThreshold,
                        pantryUsers = pantryResponse?.body()?.users?.map { it.username } ?: emptyList()
                    )
                }

                fetchShareRequests()

                if (lastDate != today) {
                    dataStoreManager.saveDailyCalories(0, today)
                }
                
                if (categoriesResponse?.isSuccessful == true) {
                    dataStoreManager.saveCategories(finalCategories)
                }

                // Opzionale: aggiorniamo la cache locale con i prodotti remoti
                if (remoteProducts != null) {
                    dataStoreManager.saveProducts(remoteProducts)
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore fatale loadData", e)
                _uiState.update { state ->
                    state.copy(allCategories = Category.entries.map { it.displayName })
                }
            }
        }
    }

    private fun mapProductResponse(res: ProductResponse): Product {
        val unit = when (res.unit.uppercase()) {
            "KG" -> MeasurementUnit.KG
            "L" -> MeasurementUnit.L
            "UNIT", "UNITÀ", "UNITA" -> MeasurementUnit.UNIT
            else -> {
                MeasurementUnit.entries.find { it.symbol.equals(res.unit, ignoreCase = true) } 
                    ?: MeasurementUnit.UNIT
            }
        }
        return Product(
            id = res.id.toString(),
            name = res.name,
            category = res.category,
            quantity = res.quantity.toDoubleOrNull() ?: 0.0,
            unit = unit,
            kcal = res.kcal,
            ean = res.ean
        )
    }

    fun addProduct(product: Product) {
        _uiState.update { it.copy(isAddingProduct = true, addProductError = null, addProductSuccess = false) }
        viewModelScope.launch {
            try {
                val createRequest = mapToCreate(product)
                val response = pantryApi.createProduct(createRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val savedProduct = mapProductResponse(response.body()!!)
                    
                    _uiState.update { state ->
                        val alreadyExists = state.products.any { it.id == savedProduct.id }
                        val newProducts = if (alreadyExists) {
                            state.products.map { if (it.id == savedProduct.id) savedProduct else it }
                        } else {
                            state.products + savedProduct
                        }
                        state.copy(
                            products = newProducts,
                            isAddingProduct = false,
                            addProductSuccess = true
                        )
                    }
                    dataStoreManager.saveProducts(uiState.value.products)
                } else {
                    val errorMessage = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isAddingProduct = false,
                        addProductError = errorMessage
                    ) }
                    android.util.Log.e("PantryViewModel", "Errore creazione prodotto: $errorMessage")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isAddingProduct = false,
                    addProductError = e.message ?: "Errore imprevisto durante l'aggiunta"
                ) }
                android.util.Log.e("PantryViewModel", "Eccezione creazione prodotto", e)
            }
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "Errore sconosciuto (${response.code()})"
            val jsonElement = json.parseToJsonElement(errorBody)
            val jsonObject = jsonElement.jsonObject
            
            // Cerca "detail" (FastAPI standard)
            val detail = jsonObject["detail"]
            if (detail != null) {
                when (detail) {
                    is kotlinx.serialization.json.JsonArray -> {
                        detail.joinToString { 
                            try { it.jsonObject["msg"]?.jsonPrimitive?.content ?: "Errore" } catch(e: Exception) { "Errore" }
                        }
                    }
                    is kotlinx.serialization.json.JsonObject -> {
                        detail["msg"]?.jsonPrimitive?.content ?: detail.toString()
                    }
                    else -> detail.jsonPrimitive.content
                }
            } else {
                "Errore: ${response.code()}"
            }
        } catch (e: Exception) {
            "Errore: ${response.code()}"
        }
    }

    fun resetAddProductState() {
        _uiState.update { it.copy(addProductSuccess = false, addProductError = null) }
    }

    private fun mapToCreate(p: Product): ProductCreate {
        return ProductCreate(
            name = p.name,
            ean = p.ean,
            unit = p.unit.name, // O p.unit.symbol? Lo schema dice "string". Uso name (KG, L, UNIT)
            quantity = p.quantity.toString(),
            category = p.category,
            kcal = p.kcal ?: 0
        )
    }

    private fun updateProductQuantityOnServer(productId: String, delta: Double) {
        viewModelScope.launch {
            try {
                val numericId = productId.toIntOrNull() ?: return@launch
                android.util.Log.d("PantryViewModel", "Invio variazione al server: id=$numericId, delta=$delta")
                val response = pantryApi.updateProductQuantity(numericId, QuantityUpdate(delta))
                if (!response.isSuccessful) {
                    android.util.Log.e("PantryViewModel", "Errore invio variazione server: ${response.code()}")
                } else {
                    android.util.Log.d("PantryViewModel", "Variazione salvata con successo")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione invio variazione server", e)
            }
        }
    }

    fun updateProduct(updatedProduct: Product) {
        val oldProduct = uiState.value.products.find { it.id == updatedProduct.id }
        val delta = updatedProduct.quantity - (oldProduct?.quantity ?: 0.0)

        _uiState.update { state ->
            val newProducts = state.products.map { 
                if (it.id == updatedProduct.id) updatedProduct else it 
            }
            state.copy(products = newProducts)
        }
        
        viewModelScope.launch {
            dataStoreManager.saveProducts(uiState.value.products)
            if (delta != 0.0) {
                updateProductQuantityOnServer(updatedProduct.id, delta)
            }
        }
    }

    fun consumeProducts(consumptions: Map<String, Double>): Int {
        var mealKcal = 0
        val today = LocalDate.now().toString()
        
        // Calcolo locale immediato per la UI (dialog)
        uiState.value.products.forEach { product ->
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
            }
        }

        viewModelScope.launch {
            android.util.Log.d("PantryViewModel", "Inizio chiamata /eat con: $consumptions")
            try {
                val currentProducts = uiState.value.products
                val eventProducts = consumptions.mapNotNull { (id, qty) ->
                    val product = currentProducts.find { it.id == id }
                    val numericId = id.toIntOrNull()
                    
                    if (numericId == null) {
                        android.util.Log.e("PantryViewModel", "ERRORE: ID prodotto NON numerico: '$id' (Nome: ${product?.name})")
                        null
                    } else {
                        EventProduct(productId = numericId, quantity = qty.toString())
                    }
                }
                
                if (eventProducts.isEmpty() && consumptions.isNotEmpty()) {
                    android.util.Log.e("PantryViewModel", "Interruzione: Nessun prodotto valido da inviare al server")
                    return@launch
                }
                
                android.util.Log.d("PantryViewModel", "Payload preparato per /eat: $eventProducts")
                val response = pantryApi.eat(EventCreate(eventProducts))
                
                if (response.isSuccessful) {
                    android.util.Log.d("PantryViewModel", "Chiamata /eat riuscita!")
                    
                    refreshData()
                    
                    _uiState.update { state ->
                        state.copy(dailyCalories = state.dailyCalories + mealKcal)
                    }
                    dataStoreManager.saveDailyCalories(uiState.value.dailyCalories, today)
                } else {
                    android.util.Log.e("PantryViewModel", "Errore registrazione pasto: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione registrazione pasto", e)
            }
        }
        
        return mealKcal
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                val response = pantryApi.createCategory(CategoryCreate(categoryName))
                if (response.isSuccessful && response.body() != null) {
                    val serverCategoryName = response.body()!!.name
                    _uiState.update { state ->
                        if (!state.allCategories.contains(serverCategoryName)) {
                            state.copy(allCategories = state.allCategories + serverCategoryName)
                        } else {
                            state
                        }
                    }
                    dataStoreManager.saveCategories(uiState.value.allCategories)
                } else {
                    android.util.Log.e("PantryViewModel", "Errore creazione categoria: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione creazione categoria", e)
            }
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                val response = pantryApi.deleteCategory(categoryName)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(allCategories = state.allCategories.filter { it != categoryName })
                    }
                    dataStoreManager.saveCategories(uiState.value.allCategories)
                } else {
                    android.util.Log.e("PantryViewModel", "Errore eliminazione categoria: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione eliminazione categoria", e)
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                val numericId = productId.toIntOrNull() ?: return@launch
                val response = pantryApi.deleteProduct(numericId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(products = state.products.filter { it.id != productId })
                    }
                    dataStoreManager.saveProducts(uiState.value.products)
                } else {
                    android.util.Log.e("PantryViewModel", "Errore eliminazione prodotto: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione eliminazione prodotto", e)
            }
        }
    }

    fun incrementQuantity(productId: String) {
        var delta = 0.0
        _uiState.update { state ->
            val product = state.products.find { it.id == productId }
            if (product != null) {
                delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                val newQty = product.quantity + delta
                val newProducts = state.products.map { p ->
                    if (p.id == productId) p.copy(quantity = newQty) else p
                }
                state.copy(products = newProducts)
            } else {
                state
            }
        }
        if (delta > 0.0) {
            viewModelScope.launch {
                dataStoreManager.saveProducts(uiState.value.products)
                updateProductQuantityOnServer(productId, delta)
            }
        }
    }

    fun decrementQuantity(productId: String) {
        var delta = 0.0
        _uiState.update { state ->
            val product = state.products.find { it.id == productId }
            if (product != null && product.quantity > 0) {
                delta = if (product.unit == MeasurementUnit.UNIT) 1.0 else 0.5
                // Non possiamo decrementare più di quanto abbiamo
                val effectiveDelta = minOf(delta, product.quantity)
                val newQty = product.quantity - effectiveDelta
                
                // Salviamo il delta negativo da inviare al server
                delta = -effectiveDelta
                
                val newProducts = state.products.map { p ->
                    if (p.id == productId) p.copy(quantity = newQty) else p
                }
                state.copy(products = newProducts)
            } else {
                state
            }
        }
        if (delta < 0.0) {
            viewModelScope.launch {
                dataStoreManager.saveProducts(uiState.value.products)
                updateProductQuantityOnServer(productId, delta)
            }
        }
    }

    fun updateQuantity(productId: String, newQuantity: Double) {
        val oldProduct = uiState.value.products.find { it.id == productId }
        val delta = newQuantity - (oldProduct?.quantity ?: 0.0)

        _uiState.update { state ->
            val newProducts = state.products.map { product ->
                if (product.id == productId) {
                    product.copy(quantity = maxOf(0.0, newQuantity))
                } else {
                    product
                }
            }
            state.copy(products = newProducts)
        }
        viewModelScope.launch {
            dataStoreManager.saveProducts(uiState.value.products)
            if (delta != 0.0) {
                updateProductQuantityOnServer(productId, delta)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateThreshold(newThreshold: Int) {
        viewModelScope.launch {
            try {
                val response = pantryApi.updateThreshold(ThresholdUpdate(newThreshold))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update { state ->
                        state.copy(kcalThreshold = response.body()!!.kcalThreshold)
                    }
                } else {
                    android.util.Log.e("PantryViewModel", "Errore aggiornamento soglia: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Eccezione aggiornamento soglia", e)
            }
        }
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
            state.copy(
                showOnlyOutOfStock = newState,
                selectedCategories = if (newState) emptySet() else state.selectedCategories
            )
        }
        viewModelScope.launch { dataStoreManager.saveShowOutOfStock(uiState.value.showOnlyOutOfStock) }
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
                        category = Category.OTHER.displayName,
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

    @SuppressLint("MissingPermission")
    fun updateLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    _uiState.update { state ->
                        state.copy(userLocation = UserLocation(it.latitude, it.longitude))
                    }
                }
            }
    }

    fun addStore(store: Store) {
        _uiState.update { state ->
            state.copy(stores = state.stores + store)
        }
        viewModelScope.launch { dataStoreManager.saveStores(uiState.value.stores) }
    }

    fun deleteStore(storeId: String) {
        _uiState.update { state ->
            state.copy(stores = state.stores.filter { it.id != storeId })
        }
        viewModelScope.launch { dataStoreManager.saveStores(uiState.value.stores) }
    }

    fun searchStores(query: String, nearMe: Boolean) {
        if (query.isBlank()) return

        _uiState.update { it.copy(isSearchingStores = true, storeSearchError = null, storeSearchResults = emptyList()) }

        viewModelScope.launch {
            try {
                val userLoc = if (nearMe) uiState.value.userLocation else null
                val results = nominatimApi.search(
                    query = query,
                    lat = userLoc?.latitude,
                    lon = userLoc?.longitude
                )

                if (results.isEmpty()) {
                    _uiState.update { it.copy(isSearchingStores = false, storeSearchError = "Nessun risultato trovato") }
                } else {
                    val mappedResults = results.map { res ->
                        StoreSearchResult(
                            name = res.name ?: res.displayName.split(",").first(),
                            address = res.displayName,
                            latitude = res.lat.toDouble(),
                            longitude = res.lon.toDouble()
                        )
                    }
                    _uiState.update { it.updateLocationSortedSearchResults(mappedResults) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingStores = false, storeSearchError = "Errore: ${e.localizedMessage}") }
            }
        }
    }

    fun clearStoreSearch() {
        _uiState.update { it.copy(storeSearchResults = emptyList(), storeSearchError = null) }
    }

    fun fetchShareRequests() {
        _uiState.update { it.copy(isFetchingShareRequests = true) }
        viewModelScope.launch {
            try {
                val response = pantryApi.getShareRequests()
                val rawString = response.body()?.string()
                android.util.Log.d("PantryViewModel", "fetchShareRequests [code=${response.code()}]: $rawString")

                if (response.isSuccessful && !rawString.isNullOrBlank()) {
                    val requests = parseShareRequestsJson(rawString)
                    android.util.Log.d("PantryViewModel", "Parsed share requests (${requests.size}): $requests")
                    _uiState.update { state ->
                        state.copy(
                            shareRequests = requests,
                            isFetchingShareRequests = false
                        )
                    }
                } else {
                    android.util.Log.w("PantryViewModel", "Errore fetchShareRequests: ${response.code()}")
                    _uiState.update { it.copy(isFetchingShareRequests = false) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore recupero richieste condivisione", e)
                _uiState.update { it.copy(isFetchingShareRequests = false) }
            }
        }
    }

    private fun parseShareRequestsJson(rawJson: String): List<PantryShareRequestResponse> {
        return try {
            val jsonElement = json.parseToJsonElement(rawJson)
            val jsonArray = when (jsonElement) {
                is kotlinx.serialization.json.JsonArray -> jsonElement
                is kotlinx.serialization.json.JsonObject -> {
                    val arrayKey = listOf("requests", "share_requests", "pantry_share_requests", "data", "items")
                        .find { key -> jsonElement[key] is kotlinx.serialization.json.JsonArray }
                    if (arrayKey != null) {
                        jsonElement[arrayKey] as kotlinx.serialization.json.JsonArray
                    } else {
                        kotlinx.serialization.json.buildJsonArray { add(jsonElement) }
                    }
                }
                else -> return emptyList()
            }

            jsonArray.mapNotNull { element ->
                try {
                    val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                    
                    val id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() 
                        ?: obj["request_id"]?.jsonPrimitive?.content?.toIntOrNull() 
                        ?: 0

                    val reqUsername = obj["requester_username"]?.jsonPrimitive?.content
                        ?: obj["requester"]?.jsonPrimitive?.content
                        ?: obj["username"]?.jsonPrimitive?.content
                        ?: obj["sender_username"]?.jsonPrimitive?.content
                        ?: obj["from_username"]?.jsonPrimitive?.content

                    val reqName = obj["requester_name"]?.jsonPrimitive?.content
                        ?: obj["sender_name"]?.jsonPrimitive?.content
                        ?: obj["name"]?.jsonPrimitive?.content

                    val status = obj["status"]?.jsonPrimitive?.content ?: "pending"
                    val createdAt = obj["created_at"]?.jsonPrimitive?.content

                    PantryShareRequestResponse(
                        id = id,
                        requesterUsername = reqUsername,
                        requesterName = reqName,
                        status = status,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PantryViewModel", "Errore parsing elemento richiesta", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PantryViewModel", "Errore parsing JSON richieste", e)
            emptyList()
        }
    }

    fun sendShareRequest(targetUsername: String) {
        val trimmed = targetUsername.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(shareActionError = "Inserisci un nome utente valido") }
            return
        }
        if (uiState.value.pantryUsers.any { it.equals(trimmed, ignoreCase = true) }) {
            _uiState.update { it.copy(shareActionError = "L'utente fa già parte della dispensa") }
            return
        }

        _uiState.update { it.copy(isSendingShareRequest = true, shareActionError = null, shareActionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val response = pantryApi.createShareRequest(PantryShareRequestCreate(trimmed))
                if (response.isSuccessful) {
                    _uiState.update { it.copy(
                        isSendingShareRequest = false,
                        shareActionSuccessMessage = "Richiesta inviata a $trimmed"
                    ) }
                    fetchShareRequests()
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isSendingShareRequest = false,
                        shareActionError = errorMsg
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore invio richiesta condivisione", e)
                _uiState.update { it.copy(
                    isSendingShareRequest = false,
                    shareActionError = e.localizedMessage ?: "Errore di rete"
                ) }
            }
        }
    }

    fun approveShareRequest(requestId: Int) {
        _uiState.update { it.copy(isProcessingShareRequest = true, shareActionError = null, shareActionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val response = pantryApi.approveShareRequest(requestId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(
                        isProcessingShareRequest = false,
                        shareActionSuccessMessage = "Richiesta approvata"
                    ) }
                    refreshData()
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isProcessingShareRequest = false,
                        shareActionError = errorMsg
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore approvazione richiesta", e)
                _uiState.update { it.copy(
                    isProcessingShareRequest = false,
                    shareActionError = e.localizedMessage ?: "Errore di rete"
                ) }
            }
        }
    }

    fun rejectShareRequest(requestId: Int) {
        _uiState.update { it.copy(isProcessingShareRequest = true, shareActionError = null, shareActionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val response = pantryApi.rejectShareRequest(requestId)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(
                        isProcessingShareRequest = false,
                        shareActionSuccessMessage = "Richiesta rifiutata"
                    ) }
                    fetchShareRequests()
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isProcessingShareRequest = false,
                        shareActionError = errorMsg
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore rifiuto richiesta", e)
                _uiState.update { it.copy(
                    isProcessingShareRequest = false,
                    shareActionError = e.localizedMessage ?: "Errore di rete"
                ) }
            }
        }
    }

    fun removeUserFromPantry(username: String) {
        _uiState.update { it.copy(isRemovingUser = true, shareActionError = null, shareActionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val response = pantryApi.removeUser(username)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(
                        isRemovingUser = false,
                        shareActionSuccessMessage = "Utente $username rimosso dalla dispensa"
                    ) }
                    refreshData()
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isRemovingUser = false,
                        shareActionError = errorMsg
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore rimozione utente", e)
                _uiState.update { it.copy(
                    isRemovingUser = false,
                    shareActionError = e.localizedMessage ?: "Errore di rete"
                ) }
            }
        }
    }

    fun leavePantry(onSuccess: (() -> Unit)? = null) {
        _uiState.update { it.copy(isLeavingPantry = true, shareActionError = null, shareActionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val response = pantryApi.leavePantry()
                if (response.isSuccessful) {
                    _uiState.update { it.copy(
                        isLeavingPantry = false,
                        shareActionSuccessMessage = "Hai lasciato la dispensa"
                    ) }
                    refreshData()
                    onSuccess?.invoke()
                } else {
                    val errorMsg = parseErrorMessage(response)
                    _uiState.update { it.copy(
                        isLeavingPantry = false,
                        shareActionError = errorMsg
                    ) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PantryViewModel", "Errore uscita dispensa", e)
                _uiState.update { it.copy(
                    isLeavingPantry = false,
                    shareActionError = e.localizedMessage ?: "Errore di rete"
                ) }
            }
        }
    }

    fun clearShareActionMessages() {
        _uiState.update { it.copy(shareActionError = null, shareActionSuccessMessage = null) }
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
    val scannedEan: String? = null,
    val stores: List<Store> = emptyList(),
    val userLocation: UserLocation? = null,
    val storeSearchResults: List<StoreSearchResult> = emptyList(),
    val isSearchingStores: Boolean = false,
    val storeSearchError: String? = null,
    val pantryId: Int? = null,
    val pantryCreatorId: String? = null,
    val kcalThreshold: Int? = null,
    val pantryUsers: List<String> = emptyList(),
    val isAddingProduct: Boolean = false,
    val addProductSuccess: Boolean = false,
    val addProductError: String? = null,
    val shareRequests: List<PantryShareRequestResponse> = emptyList(),
    val isFetchingShareRequests: Boolean = false,
    val isSendingShareRequest: Boolean = false,
    val isProcessingShareRequest: Boolean = false,
    val isRemovingUser: Boolean = false,
    val isLeavingPantry: Boolean = false,
    val shareActionError: String? = null,
    val shareActionSuccessMessage: String? = null
) {
    fun updateLocationSortedSearchResults(results: List<StoreSearchResult>): PantryUiState {
        val sorted = if (userLocation != null) {
            results.sortedBy { res ->
                val dist = FloatArray(1)
                android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    res.latitude, res.longitude,
                    dist
                )
                dist[0]
            }
        } else {
            results
        }
        return copy(isSearchingStores = false, storeSearchResults = sorted)
    }

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


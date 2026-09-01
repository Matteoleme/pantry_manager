package com.mobileapp.xpensa.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobileapp.xpensa.navigation.PantryDestination
import com.mobileapp.xpensa.scanner.BarcodeScannerScreen
import com.mobileapp.xpensa.ui.components.PantryScaffold
import com.mobileapp.xpensa.ui.consumption.MealConsumptionScreen
import com.mobileapp.xpensa.ui.categories.ManageCategoriesScreen
import com.mobileapp.xpensa.ui.home.HomeScreen
import com.mobileapp.xpensa.ui.products.EditProductScreen
import com.mobileapp.xpensa.ui.products.NewProductScreen
import com.mobileapp.xpensa.ui.profile.PantryInfoScreen
import com.mobileapp.xpensa.ui.profile.ProfileScreen
import com.mobileapp.xpensa.ui.stores.StoresScreen
import com.mobileapp.xpensa.ui.theme.XpensaTheme

import androidx.compose.ui.platform.LocalContext
import android.app.Application
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

import com.mobileapp.xpensa.data.AuthRepository
import com.mobileapp.xpensa.data.api.AuthApi
import com.mobileapp.xpensa.data.api.AuthInterceptor
import com.mobileapp.xpensa.data.api.TokenAuthenticator
import com.mobileapp.xpensa.data.api.PantryApi
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.auth.AuthViewModel
import com.mobileapp.xpensa.ui.auth.LoginScreen
import com.mobileapp.xpensa.ui.auth.RegisterScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PantryApp() {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context.applicationContext) }
    val json = remember { Json { ignoreUnknownKeys = true } }

    // Client di base per il refresh (senza authenticator per evitare loop)
    val baseClient = remember {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Repository dedicato al refresh
    val authRepositoryForRefresh = remember {
        val api = Retrofit.Builder()
            .baseUrl(AuthApi.BASE_URL)
            .client(baseClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
        AuthRepository(api, dataStoreManager)
    }

    val sharedClient = remember {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(dataStoreManager))
            .authenticator(TokenAuthenticator(authRepositoryForRefresh, dataStoreManager))
            .build()
    }

    val authApi = remember {
        Retrofit.Builder()
            .baseUrl(AuthApi.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    val pantryApi = remember {
        Retrofit.Builder()
            .baseUrl(PantryApi.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PantryApi::class.java)
    }

    val authRepository = remember { AuthRepository(authApi, dataStoreManager) }

    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(authRepository) as T
            }
        }
    )

    // Gestione permesso notifiche per Android 13+ tramite Accompanist
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        if (!notificationPermissionState.status.isGranted) {
            LaunchedEffect(Unit) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    val pantryViewModel: PantryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PantryViewModel(context.applicationContext as Application, pantryApi) as T
            }
        }
    )

    val initialDestination = remember {
        val token = runBlocking { dataStoreManager.authTokenFlow.first() }
        if (token != null) PantryDestination.Home else PantryDestination.Login
    }

    val backStack = rememberNavBackStack(initialDestination)

    val uiState by pantryViewModel.uiState.collectAsState()

    PantryScaffold(
        currentDestination = backStack.last() as PantryDestination,
        onNavigate = { destination ->
            if (backStack.last() != destination) {
                // If navigating to a main tab, we might want to clear the stack up to home
                if (destination == PantryDestination.Home) {
                    while (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                } else if (destination in listOf(PantryDestination.Consuma, PantryDestination.Trends, PantryDestination.Profile)) {
                    // Logic for main tabs: keep home at base
                    while (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                    backStack.add(destination)
                } else {
                    backStack.add(destination)
                }
            }
        },
        onStatsClick = {
            pantryViewModel.setShowStatsModal(true)
        },
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = { pantryViewModel.onSearchQueryChange(it) }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                val current = backStack.last()
                if (backStack.size > 1) {
                    when (current) {
                        PantryDestination.PantryInfo -> {
                            // Detal to Parent
                            backStack.removeAt(backStack.size - 1)
                            // If parent is not Profile, we might want to ensure it is
                            if (backStack.last() != PantryDestination.Profile) {
                                backStack.add(PantryDestination.Profile)
                            }
                        }
                        PantryDestination.Consuma, PantryDestination.Trends, PantryDestination.Profile -> {
                            // Main tabs to Home
                            while (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                        else -> {
                            // Default back
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { key ->
            when (key) {
                PantryDestination.Login -> NavEntry(key) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToRegister = {
                            backStack.add(PantryDestination.Register)
                        },
                        onLoginSuccess = {
                            // Refresh data in pantryViewModel now that we have a token
                            pantryViewModel.refreshData()
                            
                            // Clear backstack and go home
                            while (backStack.size > 0) {
                                backStack.removeAt(backStack.size - 1)
                            }
                            backStack.add(PantryDestination.Home)
                        }
                    )
                }
                PantryDestination.Register -> NavEntry(key) {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                PantryDestination.Home -> NavEntry(key) { 
                    HomeScreen(
                        viewModel = pantryViewModel,
                        onNavigateToEdit = { productId ->
                            backStack.add(PantryDestination.EditProduct(productId))
                        }
                    ) 
                }
                PantryDestination.Consuma -> NavEntry(key) {
                    MealConsumptionScreen(
                        viewModel = pantryViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                PantryDestination.Profile -> NavEntry(key) {
                    ProfileScreen(
                        viewModel = authViewModel,
                        onNavigateToPantryInfo = {
                            backStack.add(PantryDestination.PantryInfo)
                        },
                        onLogout = {
                            // Clear backstack and go to login
                            while (backStack.size > 0) {
                                backStack.removeAt(backStack.size - 1)
                            }
                            backStack.add(PantryDestination.Login)
                        }
                    )
                }
                PantryDestination.ManageCategories -> NavEntry(key) {
                    ManageCategoriesScreen(
                        viewModel = pantryViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                PantryDestination.PantryInfo -> NavEntry(key) {
                    PantryInfoScreen(
                        viewModel = pantryViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                PantryDestination.Trends -> NavEntry(key) { TrendsScreen() }
                PantryDestination.Favorites -> NavEntry(key) { FavoritesScreen() }
                PantryDestination.Stores -> NavEntry(key) { 
                    StoresScreen(viewModel = pantryViewModel) 
                }
                PantryDestination.NewProduct -> NavEntry(key) { 
                    NewProductScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        },
                        onNavigateToScanner = {
                            backStack.add(PantryDestination.Scanner)
                        },
                        viewModel = pantryViewModel
                    ) 
                }
                is PantryDestination.EditProduct -> NavEntry(key) {
                    EditProductScreen(
                        productId = key.productId,
                        viewModel = pantryViewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }
                PantryDestination.Scanner -> NavEntry(key) {
                    BarcodeScannerScreen(
                        onBarcodeDetected = { code ->
                            // 1. Salva il codice nell'UI state
                            pantryViewModel.setScannedEan(code)
                            // 2. Lancia in automatico la chiamata a Open Food Facts
                            pantryViewModel.fetchProductFromEan(code)
                            // Esempio: torna indietro dopo aver scansionato
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        },
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        }
                    )
                }

                else -> NavEntry(key) { Text("Unknown Destination") }
            }
        }
    }
}

@Composable
fun TrendsScreen() {
    Text("Trends")
}

@Composable
fun FavoritesScreen() {
    Text("Favorites")
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,navigation=buttons")
@Composable
fun PantryAppPreview() {
    XpensaTheme {
        PantryApp()
    }
}

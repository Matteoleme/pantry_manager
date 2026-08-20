package com.mobileapp.xpensa.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mobileapp.xpensa.navigation.PantryDestination
import com.mobileapp.xpensa.ui.components.PantryScaffold
import com.mobileapp.xpensa.ui.consumption.MealConsumptionScreen
import com.mobileapp.xpensa.ui.home.HomeScreen
import com.mobileapp.xpensa.ui.products.EditProductScreen
import com.mobileapp.xpensa.ui.products.NewProductScreen
import com.mobileapp.xpensa.ui.theme.XpensaTheme

@Composable
fun PantryApp() {
    val backStack = rememberNavBackStack(PantryDestination.Home)
    val pantryViewModel: PantryViewModel = viewModel()

    val uiState by pantryViewModel.uiState.collectAsState()

    PantryScaffold(
        currentDestination = backStack.last() as PantryDestination,
        onNavigate = { destination ->
            if (backStack.last() != destination) {
                backStack.add(destination)
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
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { key ->
            when (key) {
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
                PantryDestination.Trends -> NavEntry(key) { TrendsScreen() }
                PantryDestination.Favorites -> NavEntry(key) { FavoritesScreen() }
                PantryDestination.NewProduct -> NavEntry(key) { 
                    NewProductScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
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

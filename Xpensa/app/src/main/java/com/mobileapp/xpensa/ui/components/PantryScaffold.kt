package com.mobileapp.xpensa.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.navigation.PantryDestination
import kotlinx.coroutines.launch

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val destination: PantryDestination? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScaffold(
    currentDestination: PantryDestination,
    onNavigate: (PantryDestination) -> Unit,
    onStatsClick: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }

    val drawerItems = listOf(
        DrawerItem("New Product", Icons.Default.Add, PantryDestination.NewProduct),
        DrawerItem("Manage Products", Icons.Default.Inventory, PantryDestination.ManageProducts),
        DrawerItem("Manage Categories", Icons.Default.Category, PantryDestination.ManageCategories),
        DrawerItem("Pantry Settings", Icons.Default.Settings, PantryDestination.PantryInfo),
        DrawerItem("My Stores", Icons.Default.Store, PantryDestination.Stores),
        DrawerItem("Daily Stats", Icons.Default.BarChart),
        DrawerItem("Shopping List", Icons.Default.ShoppingCart),
        DrawerItem("Share", Icons.Default.Share)
    )

    val isAuthScreen = currentDestination == PantryDestination.Login || currentDestination == PantryDestination.Register

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isAuthScreen,
        drawerContent = {
            if (!isAuthScreen) {
                ModalDrawerSheet {
                    drawerItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.label) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.label == "Daily Stats") {
                                    onStatsClick()
                                } else {
                                    item.destination?.let { onNavigate(it) }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isAuthScreen) {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search product...") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                    )
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Kitchen,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text("Home Pantry")
                                }
                            }
                        },
                        navigationIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    onSearchQueryChange("")
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isAuthScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = currentDestination == PantryDestination.Home,
                            onClick = { onNavigate(PantryDestination.Home) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Restaurant, contentDescription = "Consume") },
                            label = { Text("Consume") },
                            selected = currentDestination == PantryDestination.Consuma,
                            onClick = { onNavigate(PantryDestination.Consuma) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Trend") },
                            label = { Text("Trend") },
                            selected = currentDestination == PantryDestination.Trends,
                            onClick = { onNavigate(PantryDestination.Trends) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            selected = currentDestination == PantryDestination.Profile,
                            onClick = { onNavigate(PantryDestination.Profile) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isAuthScreen && (currentDestination == PantryDestination.Home || currentDestination == PantryDestination.Consuma)) {
                    FloatingActionButton(onClick = { onNavigate(PantryDestination.NewProduct) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .imePadding()
            ) {
                content()
            }
        }
    }
}

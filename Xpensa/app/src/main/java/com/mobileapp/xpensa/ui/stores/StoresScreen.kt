package com.mobileapp.xpensa.ui.stores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.location.Location
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mobileapp.xpensa.data.Store
import com.mobileapp.xpensa.data.UserLocation
import com.mobileapp.xpensa.data.StoreSearchResult
import com.mobileapp.xpensa.ui.PantryViewModel
import com.mobileapp.xpensa.ui.theme.LightGreen
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StoresScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.updateLocation()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Negozio")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "I Miei Negozi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Location Info Card
            LocationStatusCard(
                userLocation = uiState.userLocation,
                isPermissionGranted = locationPermissionState.status.isGranted,
                shouldShowRationale = locationPermissionState.status.shouldShowRationale,
                onRequestPermission = { locationPermissionState.launchPermissionRequest() },
                onRefreshLocation = { viewModel.updateLocation() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.stores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun negozio salvato")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.stores) { store ->
                        StoreItem(
                            store = store,
                            userLocation = uiState.userLocation,
                            onDelete = { viewModel.deleteStore(store.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStoreDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun LocationStatusCard(
    userLocation: UserLocation?,
    isPermissionGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Posizione Attuale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isPermissionGranted) {
                    IconButton(onClick = onRefreshLocation, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna posizione",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isPermissionGranted -> {
                    if (userLocation != null) {
                        Text(
                            text = "Lat: ${userLocation.latitude}, Long: ${userLocation.longitude}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Recupero posizione...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                shouldShowRationale -> {
                    Column {
                        Text(
                            text = "L'app ha bisogno della posizione per calcolare le distanze dai negozi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Concedi Permesso")
                        }
                    }
                }
                else -> {
                    Button(onClick = onRequestPermission) {
                        Text("Attiva GPS")
                    }
                }
            }
        }
    }
}

@Composable
fun StoreItem(
    store: Store,
    userLocation: UserLocation?,
    onDelete: () -> Unit
) {
    val distance = remember(store, userLocation) {
        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                store.latitude, store.longitude,
                results
            )
            results[0]
        } else {
            null
        }
    }

    val distanceText = distance?.let {
        if (it < 1000) {
            "${it.roundToInt()} m"
        } else {
            "%.1f km".format(it / 1000f)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (distanceText != null) {
                            Text(
                                text = " • $distanceText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = store.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Coord: ${store.latitude}, ${store.longitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddStoreDialog(
    viewModel: PantryViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearStoreSearch()
            onDismiss()
        },
        title = { Text("Aggiungi Negozio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Nome o Indirizzo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.searchStores(query, nearMe = true) },
                        modifier = Modifier.weight(1f),
                        enabled = query.isNotBlank() && !uiState.isSearchingStores && uiState.userLocation != null,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Vicino a me", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { viewModel.searchStores(query, nearMe = false) },
                        modifier = Modifier.weight(1f),
                        enabled = query.isNotBlank() && !uiState.isSearchingStores,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Per Indirizzo", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (uiState.isSearchingStores) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                uiState.storeSearchError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (uiState.storeSearchResults.isNotEmpty()) {
                    Text(
                        "Risultati:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.storeSearchResults.take(3)) { result ->
                            SearchResultItem(
                                result = result,
                                userLocation = uiState.userLocation,
                                onSelect = {
                                    val newStore = Store(
                                        id = UUID.randomUUID().toString(),
                                        name = result.name,
                                        address = result.address,
                                        latitude = result.latitude,
                                        longitude = result.longitude
                                    )
                                    viewModel.addStore(newStore)
                                    viewModel.clearStoreSearch()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearStoreSearch()
                onDismiss()
            }) {
                Text("Annulla")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    result: StoreSearchResult,
    userLocation: UserLocation?,
    onSelect: () -> Unit
) {
    val distance = remember(result, userLocation) {
        if (userLocation != null) {
            val res = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                result.latitude, result.longitude,
                res
            )
            res[0]
        } else null
    }

    val distText = distance?.let {
        if (it < 1000) "${it.roundToInt()}m" else "%.1fkm".format(it / 1000f)
    }

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = result.address,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            distText?.let {
                Text(
                    text = "Distanza: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

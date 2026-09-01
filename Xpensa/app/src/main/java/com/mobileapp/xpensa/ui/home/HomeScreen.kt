package com.mobileapp.xpensa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.ui.PantryViewModel
import com.mobileapp.xpensa.ui.components.StatisticsModal
import com.mobileapp.xpensa.ui.theme.LightGreen
import com.mobileapp.xpensa.ui.theme.LightRed
import com.mobileapp.xpensa.ui.theme.XpensaTheme

import androidx.compose.foundation.lazy.LazyRow
import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.MeasurementUnit

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = viewModel(),
    showCategoryFilter: Boolean = true,
    onNavigateToEdit: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showCategoryFilter) {
            CategoryFilterRow(
                selectedCategories = uiState.selectedCategories,
                allCategories = uiState.allCategories,
                showOnlyOutOfStock = uiState.showOnlyOutOfStock,
                onToggleCategory = { viewModel.toggleCategory(it) },
                onToggleOutOfStock = { viewModel.toggleOutOfStockFilter() },
                onClearFilters = { viewModel.clearCategoryFilters() }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.filteredProducts) { product ->
                ProductRow(
                    product = product,
                    onIncrement = { viewModel.incrementQuantity(product.id) },
                    onDecrement = { viewModel.decrementQuantity(product.id) },
                    onClick = { viewModel.selectProduct(product) }
                )
            }

            // Placeholder rows if list is short
            if (uiState.filteredProducts.size < 5) {
                items(5 - uiState.filteredProducts.size) {
                    PlaceholderRow()
                }
            }
        }
    }

    uiState.selectedProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { viewModel.selectProduct(null) },
            onEdit = { 
                viewModel.selectProduct(null)
                onNavigateToEdit(product.id)
            },
            onDelete = {
                viewModel.deleteProduct(product.id)
                viewModel.selectProduct(null)
            }
        )
    }

    if (uiState.showStatsModal) {
        StatisticsModal(
            dailyCalories = uiState.dailyCalories,
            onDismiss = { viewModel.setShowStatsModal(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterRow(
    selectedCategories: Set<String>,
    allCategories: List<String>,
    showOnlyOutOfStock: Boolean,
    onToggleCategory: (String) -> Unit,
    onToggleOutOfStock: () -> Unit,
    onClearFilters: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedCategories.isEmpty() && !showOnlyOutOfStock,
                onClick = onClearFilters,
                label = { Text("All") }
            )
        }
        items(allCategories) { category ->
            FilterChip(
                selected = selectedCategories.contains(category),
                onClick = { onToggleCategory(category) },
                label = { Text(category) }
            )
        }
        item {
            FilterChip(
                selected = showOnlyOutOfStock,
                onClick = onToggleOutOfStock,
                label = { Text("Out of stock") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatQuantity(product.quantity, product.unit)} ${product.unit.symbol}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LightRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LightGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderRow() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = if (showDeleteConfirm) ({}) else onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (showDeleteConfirm) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Delete Product",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Are you sure you want to permanently delete '${product.name}' from your pantry?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Product",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    DetailItem(label = "Name", value = product.name)
                    DetailItem(
                        label = "Quantity", 
                        value = "${formatQuantity(product.quantity, product.unit)} ${product.unit.symbol}"
                    )
                    DetailItem(label = "Category", value = product.category)
                    val kcalLabel = when(product.unit) {
                        MeasurementUnit.KG -> "Kcal/100g"
                        MeasurementUnit.L -> "Kcal/100ml"
                        MeasurementUnit.UNIT -> "Kcal/unit"
                    }
                    DetailItem(label = kcalLabel, value = product.kcal?.toString() ?: "N/A")
                    DetailItem(label = "EAN", value = product.ean ?: "N/A")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatQuantity(quantity: Double, unit: MeasurementUnit): String {
    return if (unit == MeasurementUnit.UNIT) {
        quantity.toInt().toString()
    } else {
        if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            "%.2f".format(quantity).trimEnd('0').trimEnd('.')
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    XpensaTheme {
        HomeScreen()
    }
}

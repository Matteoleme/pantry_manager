package com.mobileapp.xpensa.ui.consumption

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.ui.PantryViewModel
import com.mobileapp.xpensa.ui.home.formatQuantity
import com.mobileapp.xpensa.ui.theme.LightGreen
import com.mobileapp.xpensa.ui.theme.LightRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealConsumptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Map to track selected products and their consumption quantities
    var consumptions by remember { mutableStateOf(mapOf<String, String>()) }
    var showSummary by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Registra Pasto",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.products.filter { it.quantity > 0 }) { product ->
                val isSelected = consumptions.containsKey(product.id)
                val currentQtyStr = consumptions[product.id] ?: ""

                ConsumptionProductRow(
                    product = product,
                    isSelected = isSelected,
                    quantityStr = currentQtyStr,
                    onToggle = {
                        consumptions = if (isSelected) {
                            consumptions - product.id
                        } else {
                            consumptions + (product.id to "")
                        }
                    },
                    onQuantityChange = { newQty ->
                        consumptions = consumptions + (product.id to newQty)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val finalConsumptions = consumptions
                    .mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                    .filterValues { it > 0.0 }
                
                if (finalConsumptions.isNotEmpty()) {
                    val totalKcal = viewModel.consumeProducts(finalConsumptions)
                    showSummary = totalKcal
                }
            },
            enabled = consumptions.any { it.value.toDoubleOrNull() ?: 0.0 > 0.0 },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Conferma Pasto", style = MaterialTheme.typography.titleMedium)
        }
    }

    showSummary?.let { kcal ->
        SummaryDialog(
            kcal = kcal,
            onDismiss = {
                showSummary = null
                onNavigateBack()
            }
        )
    }
}

@Composable
fun ConsumptionProductRow(
    product: Product,
    isSelected: Boolean,
    quantityStr: String,
    onToggle: () -> Unit,
    onQuantityChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disp: ${formatQuantity(product.quantity, product.unit)} ${product.unit.symbol}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Consuma:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { onQuantityChange(it) },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (product.unit == MeasurementUnit.UNIT) KeyboardType.Number else KeyboardType.Decimal
                        ),
                        singleLine = true,
                        suffix = { Text(product.unit.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryDialog(
    kcal: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pasto Registrato",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LightGreen
                )

                Text(
                    text = "Totale Kcal consumate:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "$kcal Kcal",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ottimo!")
                }
            }
        }
    }
}

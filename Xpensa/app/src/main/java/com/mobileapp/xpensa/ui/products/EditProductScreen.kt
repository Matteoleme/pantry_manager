package com.mobileapp.xpensa.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.ui.PantryViewModel
import com.mobileapp.xpensa.ui.theme.LightGreen
import com.mobileapp.xpensa.ui.theme.LightRed
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.products.find { it.id == productId }

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Product not found")
        }
        return
    }

    var name by remember { mutableStateOf(product.name) }
    var quantity by remember { mutableStateOf(product.quantity) }
    var kcal by remember { mutableStateOf(product.kcal?.toString() ?: "") }
    var ean by remember { mutableStateOf(product.ean ?: "") }
    var selectedUnit by remember { mutableStateOf(product.unit) }
    var selectedCategoryName by remember { mutableStateOf(product.category) }

    var deltaAmount by remember { mutableStateOf("1") }
    
    val isFormValid = name.isNotBlank() && quantity >= 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit Product",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Read-only fields for restyling
        OutlinedTextField(
            value = name,
            onValueChange = { },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )

        OutlinedTextField(
            value = selectedCategoryName,
            onValueChange = { },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )

        // Quantity Section with big +/- buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Quantity",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "${if (selectedUnit == MeasurementUnit.UNIT) quantity.toInt() else quantity} ${selectedUnit.name.lowercase()}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val delta = deltaAmount.toDoubleOrNull() ?: 0.0
                            val newQty = (quantity - delta).coerceAtLeast(0.0)
                            quantity = if (selectedUnit == MeasurementUnit.UNIT) newQty.toInt().toDouble() else newQty
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .weight(1f),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = LightRed)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.Black)
                    }

                    OutlinedTextField(
                        value = deltaAmount,
                        onValueChange = { input ->
                            if (selectedUnit == MeasurementUnit.UNIT) {
                                if (input.all { it.isDigit() }) deltaAmount = input
                            } else {
                                if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") deltaAmount = input
                            }
                        },
                        label = { Text("Variation") },
                        modifier = Modifier.weight(2f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (selectedUnit == MeasurementUnit.UNIT) KeyboardType.Number else KeyboardType.Decimal
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    IconButton(
                        onClick = {
                            val delta = deltaAmount.toDoubleOrNull() ?: 0.0
                            val newQty = quantity + delta
                            quantity = if (selectedUnit == MeasurementUnit.UNIT) newQty.toInt().toDouble() else newQty
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .weight(1f),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = LightGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.Black)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = selectedUnit.name,
                onValueChange = { },
                label = { Text("Unit") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                enabled = false
            )

            OutlinedTextField(
                value = kcal,
                onValueChange = { },
                label = { Text("Kcal") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                enabled = false
            )
        }

        OutlinedTextField(
            value = ean,
            onValueChange = { },
            label = { Text("EAN Code") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onNavigateBack() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        val updatedProduct = product.copy(
                            quantity = quantity
                        )
                        viewModel.updateProduct(updatedProduct)
                        onNavigateBack()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightGreen,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Confirm")
            }
        }
    }
}

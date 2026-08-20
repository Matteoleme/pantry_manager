package com.mobileapp.xpensa.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.ui.PantryViewModel
import com.mobileapp.xpensa.ui.theme.LightGreen
import com.mobileapp.xpensa.ui.theme.LightRed
import com.mobileapp.xpensa.ui.theme.XpensaTheme
import java.text.SimpleDateFormat
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
            Text("Prodotto non trovato")
        }
        return
    }

    var name by remember { mutableStateOf(product.name) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var kcal by remember { mutableStateOf(product.kcal?.toString() ?: "") }
    var ean by remember { mutableStateOf(product.ean ?: "") }
    var expiryDate by remember { mutableStateOf(product.expiryDate ?: "") }
    var selectedUnit by remember { mutableStateOf(product.unit) }
    var selectedCategoryName by remember { mutableStateOf(product.category) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    
    val datePickerState = rememberDatePickerState()

    // Validation
    val isNameValid = name.isNotBlank()
    val isQuantityValid = quantity.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) >= 0

    val isFormValid = isNameValid && isQuantityValid

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Date(it)
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        expiryDate = formatter.format(date)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Nuova Categoria") },
            text = {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("Nome Categoria") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryInput.isNotBlank()) {
                        viewModel.addCategory(newCategoryInput)
                        selectedCategoryName = newCategoryInput
                        newCategoryInput = ""
                        showNewCategoryDialog = false
                    }
                }) {
                    Text("Aggiungi")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNewCategoryDialog = false
                    newCategoryInput = ""
                }) {
                    Text("Annulla")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Modifica Prodotto",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome Prodotto *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = !isNameValid && name.isNotEmpty()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantità *") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (selectedUnit == MeasurementUnit.UNIT) KeyboardType.Number else KeyboardType.Decimal
                ),
                singleLine = true,
                isError = !isQuantityValid && quantity.isNotEmpty()
            )

            UnitDropdown(
                selectedUnit = selectedUnit,
                onUnitSelected = { selectedUnit = it },
                modifier = Modifier.weight(1f)
            )
        }

        CategoryDropdown(
            selectedCategoryName = selectedCategoryName,
            allCategories = uiState.allCategories,
            onCategorySelected = { selectedCategoryName = it },
            onAddNewCategory = { showNewCategoryDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = expiryDate,
            onValueChange = { },
            label = { Text("Data Scadenza") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Scegli Data")
                }
            }
        )

        OutlinedTextField(
            value = kcal,
            onValueChange = { kcal = it },
            label = { Text("Kcal/100g") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        OutlinedTextField(
            value = ean,
            onValueChange = { ean = it },
            label = { Text("Codice EAN") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { /* In Edit mode scanning might not be primary but still possible */ }) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Scannerizza")
                }
            }
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
                    containerColor = LightRed,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Annulla")
            }

            Button(
                onClick = {
                    if (isFormValid) {
                        val updatedProduct = product.copy(
                            name = name,
                            category = selectedCategoryName,
                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                            unit = selectedUnit,
                            expiryDate = expiryDate.ifBlank { null },
                            kcal = kcal.toIntOrNull(),
                            ean = ean.ifBlank { null }
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
                Text("Conferma")
            }
        }
    }
}

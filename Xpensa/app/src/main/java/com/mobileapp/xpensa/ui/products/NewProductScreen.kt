package com.mobileapp.xpensa.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobileapp.xpensa.ui.theme.LightGreen
import com.mobileapp.xpensa.ui.theme.LightRed
import com.mobileapp.xpensa.ui.theme.XpensaTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(MeasurementUnit.UNIT) }
    var selectedCategoryName by remember { mutableStateOf(Category.OTHER.displayName) }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    
    // Validation
    val isNameValid = name.isNotBlank()
    val isQuantityValid = quantity.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0

    val isFormValid = isNameValid && isQuantityValid

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.addProductSuccess) {
        if (uiState.addProductSuccess) {
            viewModel.resetAddProductState()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.addProductError) {
        uiState.addProductError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.resetAddProductState()
        }
    }

    LaunchedEffect(uiState.lastScannedProduct) {
        uiState.lastScannedProduct?.let { scanned ->
            name = scanned.name
            selectedUnit = scanned.unit
            selectedCategoryName = scanned.category
            kcal = scanned.kcal?.toString() ?: ""
            viewModel.clearFetchState()
        }
    }

    LaunchedEffect(uiState.fetchError) {
        uiState.fetchError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearFetchState()
        }
    }

    LaunchedEffect(uiState.scannedEan) {
        uiState.scannedEan?.let { scannedCode ->
        ean = scannedCode
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nuovo Prodotto",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // EAN Section First
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = ean,
                            onValueChange = { ean = it },
                            label = { Text("Codice EAN") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = onNavigateToScanner) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Apri Scanner")
                                }
                            }
                        )

                        Button(
                            onClick = { viewModel.fetchProductFromEan(ean) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = MaterialTheme.shapes.medium,
                            enabled = ean.isNotBlank() && !uiState.isFetchingProduct
                        ) {
                            Text("Cerca Prodotto da EAN")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Prodotto *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isNameValid && name.isNotEmpty(),
                    supportingText = {
                        if (!isNameValid && name.isNotEmpty()) {
                            Text("Il nome è obbligatorio", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            if (selectedUnit == MeasurementUnit.UNIT) {
                                if (it.all { char -> char.isDigit() }) quantity = it
                            } else {
                                if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") quantity = it
                            }
                        },
                        label = { Text("Quantità *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (selectedUnit == MeasurementUnit.UNIT) KeyboardType.Number else KeyboardType.Decimal
                        ),
                        singleLine = true,
                        isError = !isQuantityValid && quantity.isNotEmpty(),
                        supportingText = {
                            if (!isQuantityValid && quantity.isNotEmpty()) {
                                Text("Inserisci un numero valido", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    UnitDropdown(
                        selectedUnit = selectedUnit,
                        onUnitSelected = { 
                            selectedUnit = it 
                            // If switching to UNITS, clear or floor quantity if it has decimals
                            if (it == MeasurementUnit.UNIT) {
                                quantity = quantity.toDoubleOrNull()?.toInt()?.toString() ?: ""
                            }
                        },
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

                val kcalLabel = when(selectedUnit) {
                    MeasurementUnit.KG -> "Kcal/100g"
                    MeasurementUnit.L -> "Kcal/100ml"
                    MeasurementUnit.UNIT -> "Kcal/unità"
                }

                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text(kcalLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
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
                                val product = Product(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    category = selectedCategoryName,
                                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                                    unit = selectedUnit,
                                    kcal = kcal.toIntOrNull(),
                                    ean = ean.ifBlank { null }
                                )
                                viewModel.addProduct(product)
                            }
                        },
                        enabled = isFormValid && !uiState.isAddingProduct,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = LightGreen.copy(alpha = 0.5f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Conferma")
                    }
                }
            }

            if (uiState.isFetchingProduct || uiState.isAddingProduct) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(
    selectedUnit: MeasurementUnit,
    onUnitSelected: (MeasurementUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val units = MeasurementUnit.entries

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit.symbol,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unità") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.symbol) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategoryName: String,
    allCategories: List<String>,
    onCategorySelected: (String) -> Unit,
    onAddNewCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategoryName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("(nuova)") },
                onClick = { 
                    onAddNewCategory()
                    expanded = false 
                }
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun NewProductScreenPreview() {
    XpensaTheme {
        val viewModel: PantryViewModel = viewModel()
        NewProductScreen(
            onNavigateBack = {},
            onNavigateToScanner = {},
            viewModel = viewModel
        )
    }
}

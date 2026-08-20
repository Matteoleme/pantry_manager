package com.mobileapp.xpensa

import com.mobileapp.xpensa.data.Category
import com.mobileapp.xpensa.data.MeasurementUnit
import com.mobileapp.xpensa.data.Product
import com.mobileapp.xpensa.ui.PantryViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class PantryViewModelTest {

    @Test
    fun addProduct_updatesState() {
        val viewModel = PantryViewModel()
        val initialCount = viewModel.uiState.value.products.size
        
        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = "Test Product",
            category = Category.ALTRO.toString(),
            quantity = 1.0,
            unit = MeasurementUnit.UNIT
        )
        
        viewModel.addProduct(newProduct)
        
        assertEquals(initialCount + 1, viewModel.uiState.value.products.size)
        assertEquals(newProduct, viewModel.uiState.value.products.last())
    }
}

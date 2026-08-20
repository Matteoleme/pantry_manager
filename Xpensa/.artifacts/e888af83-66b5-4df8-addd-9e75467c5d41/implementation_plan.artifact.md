# Implement New Product Form Screen

This plan covers the implementation of the "New Product" form screen for the "Dispensa di Casa X" app, including state management using a shared `PantryViewModel` and navigation using Navigation 3.

## Proposed Changes

### Data Layer

#### [MODIFY] [Models.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/data/Models.kt)
- Update `Product` data class to include `expiryDate` (String?), `kcal` (Int?), and `ean` (String?).
- Ensure `Category` enum includes all required categories.

### ViewModel Layer

#### [NEW] [PantryViewModel.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/PantryViewModel.kt)
- Rename and move `HomeViewModel` to `PantryViewModel`.
- Add `addProduct(product: Product)` method.
- Maintain `uiState` with the list of products.

### UI Layer

#### [NEW] [NewProductScreen.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/products/NewProductScreen.kt)
- Implement the form with `TextField` for Name, Quantity, Kcal/100g, and EAN.
- Implement `DatePicker` for the Expiry Date.
- Implement `ExposedDropdownMenu` for Unità di misura and Categoria.
- Add "Annulla" (Light Red) and "Conferma" (Light Green) buttons.
- Wire up the "Conferma" button to call `viewModel.addProduct()`.

#### [MODIFY] [PantryApp.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/PantryApp.kt)
- Update to use `PantryViewModel` scoped to the backstack (or provided via `viewModel()`).
- Replace the `NewProductScreen` placeholder with the actual implementation.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/home/HomeScreen.kt)
- Update imports to use `PantryViewModel` instead of `HomeViewModel`.

## Verification Plan

### Manual Verification
- Launch the app.
- Tap the FAB or use the drawer to navigate to the "New Product" screen.
- Fill in the form and tap "Conferma".
- Verify that the new product appears in the pantry list on the Home screen.
- Verify that "Annulla" returns to the previous screen without adding a product.

# Project Foundation for "Dispensa di Casa X"

Setup the core foundation for the pantry management app, including theme, data models, navigation, and adaptive UI scaffold.

## User Review Required
> [!IMPORTANT]
> The app uses Jetpack Navigation 3 and Material 3 Adaptive components.
> The color palette is centered around Teal as requested.

## Proposed Changes

### [Theme & Colors]
#### [MODIFY] [Color.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/theme/Color.kt)
Define Teal-based colors, plus Light Red (Cancel) and Light Green (Confirm).
#### [MODIFY] [Theme.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/theme/Theme.kt)
Implement Material 3 theme with dynamic color support and static Teal fallback.

### [Data Models]
#### [NEW] [Models.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/data/Models.kt)
Create `Product`, `Category`, and `Unit` data classes.

### [Navigation]
#### [NEW] [Destinations.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/navigation/Destinations.kt)
Define `@Serializable` destinations for Home, Trends, Favorites, and New Product.

### [UI Components]
#### [NEW] [PantryScaffold.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/components/PantryScaffold.kt)
Implement the main scaffold with `ModalNavigationDrawer`, `TopAppBar`, `NavigationBar`, and `FloatingActionButton`.
#### [NEW] [PantryApp.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/PantryApp.kt)
The main entry point for the UI, wiring up Navigation 3 with the Scaffold.

### [Main Activity]
#### [MODIFY] [MainActivity.kt](file:///C:/Users/biond/AndroidStudioProjects/Xpensa/app/src/main/java/com/mobileapp/xpensa/MainActivity.kt)
Set up edge-to-edge and host `PantryApp`.

## Verification Plan

### Automated Tests
- `gradlew assembleDebug` to verify compilation.

### Manual Verification
- Verify that the bottom navigation switches between Home, Trends, and Favorites.
- Verify that the FAB navigates to the "New Product" screen.
- Verify that the hamburger icon opens the navigation drawer.
- Verify the Material 3 Teal theme is applied.

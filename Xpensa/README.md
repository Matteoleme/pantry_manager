# Xpensa - Smart Pantry Manager

**Mobile Application and Cloud Computing Course (2025/2026)**  
*Sapienza University of Rome*

---

## 1. Overview

**Xpensa** is a comprehensive Android mobile application designed to simplify household pantry management. The system allows users to keep track of their food inventory, identify items that need replenishment versus what is already available at home, record daily calorie consumption, and coordinate shared pantries across multiple family members or housemates.

---

## 2. Architecture Overview

Xpensa follows modern Android development standards, leveraging Clean Architecture and MVVM principles with reactive data streams.

### 2.1 Android Client
- **UI & Navigation**: Declarative UI built with Jetpack Compose (Material 3) and Jetpack Navigation.
- **Architecture**: Modern MVVM pattern using Kotlin Coroutines and ViewModels.
- **Networking**: Retrofit & OkHttp with automatic JWT token management.
- **Data Persistence**: DataStore Preferences for user session credentials and preferences; Room DB for offline/local storage.
- **Hardware & Sensor Integration**:
  - **CameraX + ML Kit Barcode Scanning**: High-performance camera integration for scanning EAN barcodes.
  - **Android SensorManager (Accelerometer)**: Physical shake gesture detection (`ShakeDetector`) to trigger the camera scanner from anywhere in the app.
  - **Google Play Services Location**: `FusedLocationProviderClient` for calculating real-time distances to nearby grocery stores.

### 2.2 Cloud & External Services Infrastructure
- **Cloud Backend (AWS)**: Scalable cloud infrastructure hosted on Amazon Web Services (AWS) providing REST API endpoints for user authentication, product management, pantry sharing, consumption logs, and statistics.
- **Open Food Facts API**: External REST integration used during EAN barcode scanning to fetch nutritional info (calories/100g), product titles, and default categories.
- **OpenStreetMap (Nominatim API)**: Geocoding service to search for nearby supermarkets and food stores.
- **Firebase Cloud Messaging (FCM)**: Google Firebase infrastructure for real-time remote push notifications (e.g., pantry sharing requests).

---

## 3. Key Features

### 0. Authentication & User Management
- **Account Registration & Login**: User authentication supporting email/username and password.
- **JWT Session Token Management**: Automatic token refresh handling using custom HTTP interceptors to ensure seamless user authorization.

### 1. Product Inventory View (Home Screen)
- **Centralized Inventory**: Complete view of all products available in the pantry.
- **Category Division**: Items grouped and organized by custom or pre-defined categories.
- **Real-Time Search & Filtering**: Instant search by product name and filtering by category.
- **Quick Operations**: Convenient controls for deleting items or updating quantities directly.

### 2. Product Consumption & Calorie Tracking ("Consuma")
- **Quantity Logging**: Select and consume specific quantities of products present in the pantry.
- **Automatic Stock Decrement**: Consumption automatically updates the available inventory.
- **Nutritional Integration**: Consumed products add to the user's daily total caloric intake.

### 3. Category Management
- **Custom Categories**: Add new personalized categories to tailor pantry organization.
- **Category Deletion**: Remove unwanted categories with backend synchronization.

### 4. Product Creation & EAN Barcode Scanning
- **Manual Product Entry**: Detailed form supporting product name, category, quantity, unit weight/volume, expiration date, and calories per 100g.
- **EAN Barcode Scanner**: Instant product creation by scanning EAN barcodes using ML Kit and CameraX.
- **Open Food Facts Integration**: Automatically populates product fields when a valid barcode is scanned.

### 5. Favorite Stores & Location-Based Navigation
- **Store Search & Selection**: Search for grocery stores and supermarkets using the Nominatim OpenStreetMap API.
- **Live Distance Calculation**: Calculates distance from the user's current GPS location.
- **Navigation Launch**: One-tap launch into external navigation apps (e.g., Google Maps) for turn-by-turn directions.

### 6. User Profile & Pantry Settings
- **Password Management**: Change user account password securely.
- **Pantry Information**: View details about the currently active pantry and list of connected users.
- **Calorie Goal Setting**: Customize personal daily caloric threshold (kcal limit) for nutrition tracking.

### 7. Remote Pantry Access Requests
- **Pantry Sharing**: Request access to a remote pantry owned by another user by providing their username/pantry ID.
- **Collaborative Inventory**: Enables multiple users in a household to manage the same physical pantry.

### 8. Push Notifications & Sharing Management
- **Real-Time Push Notifications**: Receives FCM push notifications when another user requests access to your pantry.
- **Interactive Requests**: Dedicated UI allowing pantry owners to **Accept** or **Reject** incoming sharing requests in real time.

### 9. Calorie & Consumption Analytics (Stats & Trends)
- **Daily Analytics**: Visual charts breaking down daily consumed calories per category compared against the target threshold.
- **Monthly Analytics**: Historical charts and trends to analyze consumption patterns over time.

### 10. Accelerometer Shake Gesture Scanner
- **Hardware Shake Detection**: Accelerometer sensor listener monitoring physical device movement.
- **Instant Camera Launch**: Shaking the device anywhere within the app opens the barcode camera scanner immediately.

---

## 4. Tech Stack Summary

| Component | Technology |
|---|---|
| **Language** | Kotlin |
| **UI & Navigation** | Jetpack Compose (Material 3), Jetpack Navigation |
| **Architecture** | MVVM, Kotlin Coroutines, ViewModels |
| **Networking** | Retrofit, OkHttp (JWT Auth) |
| **Barcode Scanning** | Google ML Kit, CameraX |
| **Location & Sensors** | FusedLocationProvider, Accelerometer (SensorManager) |
| **Data Storage** | DataStore Preferences, Room |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **External APIs** | Open Food Facts, OpenStreetMap (Nominatim) |
| **Cloud Hosting** | AWS (Amazon Web Services) |

package com.mobileapp.xpensa.data

import android.util.Log
// import com.google.firebase.messaging.FirebaseMessaging
import com.mobileapp.xpensa.data.local.DataStoreManager
// import kotlinx.coroutines.tasks.await

class FcmTokenRepository(private val dataStoreManager: DataStoreManager) {

    /**
     * Recupera il token FCM corrente da Firebase e lo salva localmente.
     * Prepara anche la chiamata al backend (TODO).
     */
    suspend fun fetchAndSaveToken() {
        /*
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("FcmTokenRepository", "FCM Token recuperato: $token")
            saveToken(token)
        } catch (e: Exception) {
            Log.e("FcmTokenRepository", "Errore nel recupero del token FCM", e)
        }
        */
    }

    /**
     * Salva il token localmente e lo invia al backend se necessario.
     */
    suspend fun saveToken(token: String) {
        dataStoreManager.saveFcmToken(token)
        registerTokenWithBackend(token)
    }

    /**
     * TODO: Implementare la registrazione del token con il backend reale.
     * Al momento l'endpoint NON è disponibile.
     */
    private suspend fun registerTokenWithBackend(token: String) {
        Log.d("FcmTokenRepository", "Registrazione token con backend (TODO): $token")
        // Esempio futuro:
        // try {
        //     val response = apiService.updateDeviceToken(TokenRequest(token))
        //     if (response.isSuccessful) {
        //         Log.d("FcmTokenRepository", "Token registrato correttamente sul backend")
        //     }
        // } catch (e: Exception) {
        //     Log.e("FcmTokenRepository", "Errore durante la registrazione del token sul backend", e)
        // }
    }
}

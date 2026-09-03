package com.mobileapp.xpensa.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mobileapp.xpensa.data.api.AuthApi
import com.mobileapp.xpensa.data.api.DeviceTokenRequest
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FcmTokenRepository(
    private val dataStoreManager: DataStoreManager,
    private val authApi: AuthApi? = null
) {

    /**
     * Flow per osservare il token FCM salvato nel DataStore.
     */
    val fcmToken: Flow<String?> = dataStoreManager.fcmTokenFlow

    /**
     * Recupera il token FCM corrente da Firebase Messaging e lo salva localmente nel DataStore.
     *
     * @return Il token recuperato, oppure null in caso di errore.
     */
    suspend fun fetchAndSaveToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("FcmTokenRepository", "FCM Token recuperato: $token")
            saveToken(token)
            token
        } catch (e: Exception) {
            Log.e("FcmTokenRepository", "Errore nel recupero del token FCM", e)
            null
        }
    }

    /**
     * Salva il token FCM localmente nel DataStore dell'app.
     */
    suspend fun saveToken(token: String) {
        dataStoreManager.saveFcmToken(token)
    }

    /**
     * Recupera il Flow del token FCM salvato nel DataStore.
     */
    fun getSavedToken(): Flow<String?> {
        return dataStoreManager.fcmTokenFlow
    }

    /**
     * Invia il token FCM memorizzato nel DataStore al backend tramite PUT /users/me/device.
     * Se il token non è presente localmente, tenta prima di recuperarlo da Firebase.
     * Se il token rimane nullo o vuoto, la chiamata al backend viene saltata.
     */
    suspend fun registerTokenWithBackend(): Result<Unit> {
        val api = authApi ?: return Result.failure(IllegalStateException("AuthApi non configurata in FcmTokenRepository"))

        var currentToken = dataStoreManager.fcmTokenFlow.first()
        if (currentToken.isNullOrBlank()) {
            Log.d("FcmTokenRepository", "Nessun FCM token trovato nel DataStore, tentativo di recupero da Firebase...")
            currentToken = fetchAndSaveToken()
        }

        if (currentToken.isNullOrBlank()) {
            Log.d("FcmTokenRepository", "Nessun FCM token disponibile, registrazione sul backend saltata.")
            return Result.success(Unit)
        }

        return try {
            val response = api.updateDeviceToken(DeviceTokenRequest(fcmToken = currentToken))
            if (response.isSuccessful) {
                Log.d("FcmTokenRepository", "FCM token registrato con successo sul backend: $currentToken")
                Result.success(Unit)
            } else {
                val errorMsg = "Errore durante la registrazione del token FCM sul backend: HTTP ${response.code()}"
                Log.e("FcmTokenRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FcmTokenRepository", "Eccezione durante la registrazione del token FCM sul backend", e)
            Result.failure(e)
        }
    }
}



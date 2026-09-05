package com.mobileapp.xpensa.data.api

import com.mobileapp.xpensa.data.AuthRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : Authenticator {

    companion object {
        private val lock = Any()

        @Volatile
        private var latestAccessToken: String? = null

        fun setLatestAccessToken(token: String?) {
            synchronized(lock) {
                latestAccessToken = token
            }
        }

        fun resetLatestAccessToken() {
            setLatestAccessToken(null)
        }

        fun getLatestAccessToken(): String? {
            return latestAccessToken
        }
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.toString()
        
        // 1. Evitiamo loop infiniti durante login, logout o refresh
        if (url.contains("auth/login") || url.contains("auth/logout") || url.contains("auth/refresh")) {
            return null
        }

        // 2. Se questa specifica richiesta è già stata ritentata una volta (priorResponse != null)
        // e ha fallito nuovamente con 401, il nuovo token è stato rifiutato dal server -> logout forzato
        if (response.priorResponse != null) {
            android.util.Log.w("TokenAuthenticator", "La richiesta ritentata ha fallito ancora con 401. Disconnessione utente...")
            resetLatestAccessToken()
            runBlocking { dataStoreManager.clearTokens() }
            return null
        }

        // 3. Verifichiamo se l'errore 401 indica esplicitamente che l'access token è scaduto ("ACCESS_TOKEN_EXPIRED")
        val isAccessTokenExpired = isAccessTokenExpiredError(response)
        if (!isAccessTokenExpired) {
            // Se l'errore 401 non è per token scaduto (es: "Session has been revoked", "Invalid authentication token", "User not found"),
            // NON tentiamo il refresh ma disconnettiamo subito l'utente
            android.util.Log.w("TokenAuthenticator", "Errore 401 diverso da ACCESS_TOKEN_EXPIRED. Disconnessione utente...")
            resetLatestAccessToken()
            runBlocking { dataStoreManager.clearTokens() }
            return null
        }

        synchronized(lock) {
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // 4. Se un'altra richiesta in parallelo ha già completato il refresh, usiamo subito il nuovo token
            if (latestAccessToken != null && latestAccessToken != requestToken) {
                android.util.Log.d("TokenAuthenticator", "Token già aggiornato in parallelo, riuso il nuovo token...")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            // 5. Leggiamo dal DataStore i token attuali
            val refreshToken = runBlocking { dataStoreManager.refreshTokenFlow.first() }
            val currentToken = runBlocking { dataStoreManager.authTokenFlow.first() }

            // Se il token nel DataStore è già diverso da quello che ha fallito, lo usiamo
            if (requestToken != currentToken && !currentToken.isNullOrBlank()) {
                latestAccessToken = currentToken
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // 6. Eseguiamo il refresh tramite il token di refresh
            if (!refreshToken.isNullOrBlank()) {
                android.util.Log.d("TokenAuthenticator", "Invio richiesta di refresh token al backend...")
                val refreshResult = runBlocking { authRepository.refreshToken(refreshToken) }
                
                if (refreshResult.isSuccess) {
                    val newAccessToken = refreshResult.getOrNull()?.accessToken
                    if (!newAccessToken.isNullOrBlank()) {
                        latestAccessToken = newAccessToken
                        android.util.Log.d("TokenAuthenticator", "Refresh token completato con successo. Nuovo token ottenuto.")
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                } else {
                    val exception = refreshResult.exceptionOrNull()
                    android.util.Log.e("TokenAuthenticator", "Refresh token fallito: ${exception?.message}")
                    // Se il refresh fallisce (es. refresh token scaduto o rifiutato), puliamo i token e disconnettiamo
                    resetLatestAccessToken()
                    runBlocking { dataStoreManager.clearTokens() }
                }
            } else {
                android.util.Log.w("TokenAuthenticator", "Nessun refresh token disponibile. Disconnessione...")
                resetLatestAccessToken()
                runBlocking { dataStoreManager.clearTokens() }
            }
        }

        return null
    }

    /**
     * Controlla se la risposta HTTP 401 contiene il JSON:
     * { "detail": { "code": "ACCESS_TOKEN_EXPIRED", "message": "..." } }
     */
    private fun isAccessTokenExpiredError(response: Response): Boolean {
        return try {
            val bodyString = response.peekBody(1024 * 1024).string()
            if (bodyString.isBlank()) return false

            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(bodyString)
            val jsonObject = jsonElement as? JsonObject ?: return false

            val detail = jsonObject["detail"]
            if (detail is JsonObject) {
                val code = (detail["code"] as? JsonPrimitive)?.content
                code == "ACCESS_TOKEN_EXPIRED"
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("TokenAuthenticator", "Errore nell'analisi del corpo di errore 401", e)
            false
        }
    }
}

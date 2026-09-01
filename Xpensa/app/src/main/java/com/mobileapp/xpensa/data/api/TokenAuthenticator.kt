package com.mobileapp.xpensa.data.api

import com.mobileapp.xpensa.data.AuthRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.toString()
        
        // Evitiamo loop infiniti o refresh durante logout/login
        if (url.contains("auth/login") || url.contains("auth/logout") || url.contains("auth/refresh")) {
            return null
        }

        synchronized(this) {
            val refreshToken = runBlocking { dataStoreManager.refreshTokenFlow.first() }
            val currentToken = runBlocking { dataStoreManager.authTokenFlow.first() }

            // Se il token salvato è già diverso da quello della richiesta, 
            // qualcun altro ha già fatto il refresh
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (requestToken != currentToken && currentToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            if (refreshToken != null) {
                val refreshResult = runBlocking { authRepository.refreshToken(refreshToken) }
                
                if (refreshResult.isSuccess) {
                    val newAccessToken = refreshResult.getOrNull()?.accessToken
                    if (newAccessToken != null) {
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                } else {
                    // Se il refresh fallisce (es. token 30gg scaduto), puliamo tutto
                    runBlocking { dataStoreManager.clearTokens() }
                }
            }
        }

        return null
    }
}

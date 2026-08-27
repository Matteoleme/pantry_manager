package com.mobileapp.xpensa.data.api

import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val dataStoreManager: DataStoreManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        val isExternalService = url.contains("openfoodfacts.org") || url.contains("openstreetmap.org")

        if (!isExternalService) {
            val token = runBlocking { dataStoreManager.authTokenFlow.first() }
            if (token != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                android.util.Log.d("AuthInterceptor", "Header Authorization aggiunto per: $url")
                return chain.proceed(newRequest)
            }
        }

        return chain.proceed(originalRequest)
    }
}

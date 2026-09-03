package com.mobileapp.xpensa.data.api

import com.mobileapp.xpensa.data.AuthRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object AuthApiFactory {
    fun createAuthApi(dataStoreManager: DataStoreManager): AuthApi {
        val json = Json { ignoreUnknownKeys = true }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val baseClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val authApiForRefresh = Retrofit.Builder()
            .baseUrl(AuthApi.BASE_URL)
            .client(baseClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)

        val authRepositoryForRefresh = AuthRepository(authApiForRefresh, dataStoreManager)

        val sharedClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(dataStoreManager))
            .authenticator(TokenAuthenticator(authRepositoryForRefresh, dataStoreManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(AuthApi.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }
}

package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("me")
    suspend fun getUserInfo(): Response<UserResponse>

    @POST("/auth/change_password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

package com.mobileapp.xpensa.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("me")
    suspend fun getUserInfo(): Response<UserResponse>

    @POST("/auth/change_password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @PUT("users/me/device")
    suspend fun updateDeviceToken(@Body request: DeviceTokenRequest): Response<Unit>

    companion object {
        const val BASE_URL = NetworkConfig.BACKEND_BASE_URL
    }
}

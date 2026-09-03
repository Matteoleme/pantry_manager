package com.mobileapp.xpensa.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RegisterRequest(
    val name: String,
    val username: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val id: Int,
    val name: String,
    val username: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val name: String,
    val username: String,
    val local: Boolean
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class CategoryResponse(
    val name: String,
    val id: Int? = null,
    val username: String? = null
)

@Serializable
data class DeviceTokenRequest(
    @SerialName("fcm_token") val fcmToken: String
)



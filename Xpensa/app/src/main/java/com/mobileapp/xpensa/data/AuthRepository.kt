package com.mobileapp.xpensa.data

import com.mobileapp.xpensa.data.api.AuthApi
import com.mobileapp.xpensa.data.api.ChangePasswordRequest
import com.mobileapp.xpensa.data.api.LoginRequest
import com.mobileapp.xpensa.data.api.LoginResponse
import com.mobileapp.xpensa.data.api.RegisterRequest
import com.mobileapp.xpensa.data.api.UserResponse
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

import android.util.Log
import com.mobileapp.xpensa.data.api.RefreshTokenRequest

class AuthRepository(
    private val api: AuthApi,
    private val dataStoreManager: DataStoreManager,
    private val fcmTokenRepository: FcmTokenRepository? = null
) {
    suspend fun register(request: RegisterRequest): Result<Unit> {
        return try {
            val response = api.register(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<Unit> {
        return try {
            val response = api.login(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    dataStoreManager.saveAuthToken(body.accessToken, body.refreshToken, body.tokenType)
                    dataStoreManager.saveCurrentUsername(request.username)

                    // Registrazione del token FCM sul backend dopo un login riuscito (non bloccante)
                    try {
                        fcmTokenRepository?.registerTokenWithBackend()
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Errore non bloccante durante la registrazione del token FCM dopo il login", e)
                    }

                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserInfo(): Result<UserResponse> {
        return try {
            val response = api.getUserInfo()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    dataStoreManager.saveCurrentUsername(body.username)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return try {
            val response = api.changePassword(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> {
        return try {
            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    dataStoreManager.saveAuthToken(body.accessToken, body.refreshToken, body.tokenType)
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val response = api.logout()
            dataStoreManager.clearTokens()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                // Anche se la chiamata al server fallisce, puliamo i token locali
                Result.success(Unit) 
            }
        } catch (e: Exception) {
            dataStoreManager.clearTokens()
            Result.success(Unit)
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "Unknown error"
            val json = Json.parseToJsonElement(errorBody).jsonObject
            // Try to extract "detail" which is common in FastAPI/Pydantic errors
            val detail = json["detail"]
            if (detail != null) {
                if (detail is kotlinx.serialization.json.JsonArray) {
                    detail.joinToString { it.jsonObject["msg"]?.jsonPrimitive?.content ?: "Error" }
                } else {
                    detail.jsonPrimitive.content
                }
            } else {
                "Error: ${response.code()}"
            }
        } catch (e: Exception) {
            "Error: ${response.code()}"
        }
    }
}

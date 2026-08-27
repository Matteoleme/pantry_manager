package com.mobileapp.xpensa.data

import com.mobileapp.xpensa.data.api.AuthApi
import com.mobileapp.xpensa.data.api.LoginRequest
import com.mobileapp.xpensa.data.api.RegisterRequest
import com.mobileapp.xpensa.data.local.DataStoreManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

class AuthRepository(
    private val api: AuthApi,
    private val dataStoreManager: DataStoreManager
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
                    dataStoreManager.saveAuthToken(body.accessToken, body.tokenType)
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

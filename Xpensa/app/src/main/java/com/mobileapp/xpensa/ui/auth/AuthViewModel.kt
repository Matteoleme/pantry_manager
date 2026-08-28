package com.mobileapp.xpensa.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileapp.xpensa.data.AuthRepository
import com.mobileapp.xpensa.data.api.ChangePasswordRequest
import com.mobileapp.xpensa.data.api.LoginRequest
import com.mobileapp.xpensa.data.api.RegisterRequest
import com.mobileapp.xpensa.data.api.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistered: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: UserResponse? = null,
    val passwordChanged: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun register(name: String, username: String, password: String, passwordConfirm: String) {
        val validationError = validateRegister(name, username, password, passwordConfirm)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.register(RegisterRequest(name, username, password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Username and password are required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.login(LoginRequest(username, password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun validateRegister(name: String, username: String, password: String, passwordConfirm: String): String? {
        if (name.isBlank() || name.length > 100) return "Name must be between 1 and 100 characters"
        if (username.isBlank() || username.length > 100) return "Username must be between 1 and 100 characters"
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { !it.isLetterOrDigit() }) return "Password must contain at least one special character"
        if (password.contains(":")) return "Password must not contain ':'"
        if (password != passwordConfirm) return "Passwords do not match"
        return null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetRegistration() {
        _uiState.update { it.copy(isRegistered = false) }
    }

    fun getUserInfo() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getUserInfo()
                .onSuccess { user ->
                    _uiState.update { it.copy(isLoading = false, user = user) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun changePassword(old: String, new: String, confirm: String) {
        if (new != confirm) {
            _uiState.update { it.copy(error = "Le password non coincidono") }
            return
        }
        if (new.length < 8) {
            _uiState.update { it.copy(error = "La password deve essere di almeno 8 caratteri") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.changePassword(ChangePasswordRequest(old, new))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, passwordChanged = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _uiState.update { AuthUiState() }
            onSuccess()
        }
    }

    fun resetPasswordChanged() {
        _uiState.update { it.copy(passwordChanged = false) }
    }
}

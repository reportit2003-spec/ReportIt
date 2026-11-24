package com.example.reportit.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reportit.data.local.LocalCacheManager
import com.example.reportit.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        _uiState.value = if (authRepository.isUserLoggedIn) {
            AuthUiState.Authenticated
        } else {
            AuthUiState.NotAuthenticated
        }
    }

    fun toggleAuthMode() {
        _isLoginMode.value = !_isLoginMode.value
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState.Authenticated
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Sign in error", error)
                    _uiState.value = AuthUiState.Error(
                        parseAuthError(error)
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUp(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState.Authenticated
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Sign up error", error)
                    _uiState.value = AuthUiState.Error(
                        parseAuthError(error)
                    )
                }
        }
    }

    suspend fun signOut() {
        authRepository.signOut()
        LocalCacheManager.clearAllCache()
        _uiState.value = AuthUiState.NotAuthenticated
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.NotAuthenticated
        }
    }

    private fun parseAuthError(error: Throwable): String {
        val msg = error.message?.lowercase() ?: ""
        return when {
            "invalid_credentials" in msg || "invalid login" in msg ->
                "Invalid email or password. Please try again."
            "user already registered" in msg || "already been registered" in msg ->
                "An account with this email already exists. Try signing in."
            "email not confirmed" in msg ->
                "Please verify your email before signing in."
            "too many requests" in msg || "rate limit" in msg ->
                "Too many attempts. Please wait a moment and try again."
            "network" in msg || "timeout" in msg || "unable to resolve" in msg ->
                "Network error. Please check your internet connection."
            "weak password" in msg ->
                "Password is too weak. Please use a stronger password."
            else -> "Something went wrong. Please try again."
        }
    }

    sealed class AuthUiState {
        data object Idle : AuthUiState()
        data object Loading : AuthUiState()
        data object Authenticated : AuthUiState()
        data object NotAuthenticated : AuthUiState()
        data class Error(val message: String) : AuthUiState()
    }
}
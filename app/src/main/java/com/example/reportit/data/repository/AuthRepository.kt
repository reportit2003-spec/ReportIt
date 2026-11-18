package com.example.reportit.data.repository

import com.example.reportit.data.model.User
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {

    private val auth = SupabaseClientProvider.auth

    val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    val currentUserEmail: String?
        get() = auth.currentUserOrNull()?.email

    val isUserLoggedIn: Boolean
        get() = auth.currentUserOrNull() != null

    val authStateFlow: Flow<AuthState>
        get() = auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.Authenticated
                is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
                else -> AuthState.Loading
            }
        }

    suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = auth.currentUserOrNull()
        User(
            id = session?.id ?: "",
            email = session?.email ?: "",
            created_at = session?.createdAt?.toString() ?: ""
        )
    }

    suspend fun signUp(email: String, password: String): Result<User> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val session = auth.currentUserOrNull()
        User(
            id = session?.id ?: "",
            email = session?.email ?: "",
            created_at = session?.createdAt?.toString() ?: ""
        )
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    sealed class AuthState {
        data object Authenticated : AuthState()
        data object NotAuthenticated : AuthState()
        data object Loading : AuthState()
    }
}
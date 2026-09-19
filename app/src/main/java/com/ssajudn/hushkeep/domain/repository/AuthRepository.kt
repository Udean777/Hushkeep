package com.ssajudn.hushkeep.domain.repository

import com.ssajudn.hushkeep.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

data class AuthUser(
    val id: String,
    val email: String?,
    val username: String? = null,
    val isLocalPreview: Boolean,
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data object ConfigurationRequired : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
    data class Error(val message: String) : AuthState
}

interface AuthRepository {
    val state: StateFlow<AuthState>

    suspend fun restoreSession()

    suspend fun signIn(email: String, password: String): AppResult<AuthUser>

    suspend fun signUp(username: String, email: String, password: String): AppResult<AuthUser>

    suspend fun signOut(): AppResult<Unit>

    suspend fun deleteAccount(): AppResult<Unit>
}

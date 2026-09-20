package com.ssajudn.hushkeep.data.local

import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.OtpPolicy
import com.ssajudn.hushkeep.core.common.UsernamePolicy
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.domain.repository.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Explicit local preview mode used when Supabase credentials are not configured.
 * It is not a production authentication mechanism.
 */
class LocalPreviewAuthRepository(
    private val sessionStore: SessionStore,
) : AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state

    override suspend fun restoreSession() {
        val session = sessionStore.session.first()
        _state.value = session?.let {
            AuthState.SignedIn(
                AuthUser(
                    id = it.userId,
                    email = it.email,
                    username = it.username,
                    isLocalPreview = true,
                ),
            )
        } ?: AuthState.SignedOut
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> {
        if (email.isBlank() || password.isBlank()) {
            return AppResult.Failure(AppError.Validation("Email dan password wajib diisi."))
        }

        val user = AuthUser(
            id = "local-preview-user",
            email = email.trim(),
            isLocalPreview = true,
        )
        sessionStore.save(user)
        _state.value = AuthState.SignedIn(user)
        return AppResult.Success(user)
    }

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ): AppResult<AuthUser> {
        if (!UsernamePolicy.isValid(username)) {
            return AppResult.Failure(AppError.Validation(UsernamePolicy.errorMessage))
        }
        if (password.length < 8) {
            return AppResult.Failure(AppError.Validation("Password minimal 8 karakter."))
        }
        if (email.isBlank()) {
            return AppResult.Failure(AppError.Validation("Email wajib diisi."))
        }

        val user = AuthUser(
            id = "local-preview-user",
            email = email.trim(),
            username = UsernamePolicy.normalize(username),
            isLocalPreview = true,
        )
        sessionStore.save(user)
        _state.value = AuthState.SignedIn(user)
        return AppResult.Success(user)
    }

    override suspend fun signOut(): AppResult<Unit> {
        sessionStore.clear()
        _state.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        sessionStore.clear()
        _state.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }

    override suspend fun reauthenticate(password: String): AppResult<Unit> =
        if (password.isBlank()) {
            AppResult.Failure(AppError.Validation("Password wajib diisi."))
        } else {
            AppResult.Success(Unit)
        }

    override suspend fun verifySignupOtp(email: String, token: String): AppResult<AuthUser> {
        if (!OtpPolicy.isValid(token)) {
            return AppResult.Failure(AppError.Validation("Kode verifikasi harus 6 digit."))
        }
        return signIn(email, "local-preview-otp")
    }

    override suspend fun resendSignupOtp(email: String): AppResult<Unit> =
        if (email.isBlank()) {
            AppResult.Failure(AppError.Validation("Email wajib diisi."))
        } else {
            AppResult.Success(Unit)
        }
}

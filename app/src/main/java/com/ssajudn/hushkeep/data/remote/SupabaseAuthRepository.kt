package com.ssajudn.hushkeep.data.remote

import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.OtpPolicy
import com.ssajudn.hushkeep.core.common.UsernamePolicy
import com.ssajudn.hushkeep.core.common.UserFacingMessages
import com.ssajudn.hushkeep.core.config.AppConfig
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.domain.repository.AuthUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class ProfileInsert(
    val id: String,
    val email: String?,
    val username: String,
)

class SupabaseAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state

    override suspend fun restoreSession() {
        runCatching {
            client.auth.awaitInitialization()
            client.auth.currentUserOrNull()
        }.fold(
            onSuccess = { user ->
                _state.value = user?.let {
                    AuthState.SignedIn(AuthUser(it.id, it.email, isLocalPreview = false))
                } ?: AuthState.SignedOut
            },
            onFailure = { error ->
                _state.value = AuthState.Error(UserFacingMessages.SESSION_RESTORE_FAILED)
            },
        )
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> {
        if (email.isBlank() || password.isBlank()) {
            return AppResult.Failure(AppError.Validation("Email dan password wajib diisi."))
        }

        _state.value = AuthState.Loading
        return runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = client.auth.currentUserOrNull()
                ?: error("Sesi berhasil dibuat, tetapi user tidak tersedia.")
            AuthUser(user.id, user.email, isLocalPreview = false)
        }.fold(
            onSuccess = { user ->
                _state.value = AuthState.SignedIn(user)
                AppResult.Success(user)
            },
            onFailure = { error ->
                _state.value = AuthState.Error(UserFacingMessages.SIGN_IN_FAILED)
                AppResult.Failure(AppError.Remote("Tidak dapat masuk ke Hushkeep.", error))
            },
        )
    }

    override suspend fun signUp(username: String, email: String, password: String): AppResult<AuthUser> {
        val normalizedUsername = UsernamePolicy.normalize(username)
        if (!UsernamePolicy.isValid(username)) {
            return AppResult.Failure(
                AppError.Validation(UsernamePolicy.errorMessage),
            )
        }
        if (email.isBlank() || password.length < 8) {
            return AppResult.Failure(
                AppError.Validation("Gunakan email valid dan password minimal 8 karakter."),
            )
        }

        _state.value = AuthState.Loading
        return runCatching {
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = client.auth.currentUserOrNull()
                ?: error("Akun dibuat. Periksa email untuk konfirmasi sebelum masuk.")
            client.from("profiles").insert(
                ProfileInsert(
                    id = user.id,
                    email = user.email,
                    username = normalizedUsername,
                ),
            )
            AuthUser(user.id, user.email, isLocalPreview = false)
        }.fold(
            onSuccess = { user ->
                _state.value = AuthState.SignedIn(user)
                AppResult.Success(user)
            },
            onFailure = { error ->
                _state.value = AuthState.Error(UserFacingMessages.SIGN_UP_FAILED)
                AppResult.Failure(AppError.Remote("Tidak dapat membuat akun.", error))
            },
        )
    }

    override suspend fun signOut(): AppResult<Unit> {
        return runCatching {
            client.auth.signOut()
            _state.value = AuthState.SignedOut
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { error -> AppResult.Failure(AppError.Remote("Tidak dapat keluar.", error)) },
        )
    }

    override suspend fun deleteAccount(): AppResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val accessToken = client.auth.currentAccessTokenOrNull()
                ?: error("Sesi tidak tersedia.")
            val connection = (URL(
                "${AppConfig.supabaseUrl.trimEnd('/')}/functions/v1/delete-account",
            ).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
            }
            connection.connect()
            val status = connection.responseCode
            connection.disconnect()
            if (status !in 200..299) error("Penghapusan akun gagal dengan status $status.")
            client.auth.signOut()
            _state.value = AuthState.SignedOut
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { error ->
                AppResult.Failure(AppError.Remote("Akun belum dapat dihapus.", error))
            },
        )
    }

    override suspend fun reauthenticate(password: String): AppResult<Unit> {
        if (password.isBlank()) return AppResult.Failure(AppError.Validation("Password wajib diisi."))
        val email = client.auth.currentUserOrNull()?.email
            ?: return AppResult.Failure(AppError.AuthenticationRequired)
        return runCatching {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Failure(AppError.Remote("Password tidak cocok.", it)) },
        )
    }

    override suspend fun verifySignupOtp(email: String, token: String): AppResult<AuthUser> {
        if (!OtpPolicy.isValid(token)) {
            return AppResult.Failure(AppError.Validation("Kode verifikasi harus 6 digit."))
        }
        return runCatching {
            client.auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = email.trim(),
                token = token,
            )
            val user = client.auth.currentUserOrNull()
                ?: error("Sesi verifikasi belum tersedia.")
            AuthUser(user.id, user.email, isLocalPreview = false)
        }.fold(
            onSuccess = { user ->
                _state.value = AuthState.SignedIn(user)
                AppResult.Success(user)
            },
            onFailure = { error ->
                AppResult.Failure(AppError.Remote(UserFacingMessages.OTP_VERIFY_FAILED, error))
            },
        )
    }

    override suspend fun resendSignupOtp(email: String): AppResult<Unit> {
        if (email.isBlank()) return AppResult.Failure(AppError.Validation("Email wajib diisi."))
        return runCatching {
            client.auth.resendEmail(OtpType.Email.SIGNUP, email.trim())
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { error ->
                AppResult.Failure(AppError.Remote(UserFacingMessages.OTP_RESEND_FAILED, error))
            },
        )
    }

}

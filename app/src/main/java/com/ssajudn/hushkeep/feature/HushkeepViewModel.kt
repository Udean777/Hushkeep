package com.ssajudn.hushkeep.feature

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.UiState
import com.ssajudn.hushkeep.core.config.AppConfig
import com.ssajudn.hushkeep.core.media.ExportManager
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface AppMessage {
    data class Text(val value: String) : AppMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
class HushkeepViewModel(
    private val authRepository: AuthRepository,
    private val memoryRepository: MemoryRepository,
    private val exportManager: ExportManager,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.state

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }

    private val signedInUserId: Flow<String> = authState
        .filterIsInstance<AuthState.SignedIn>()
        .map { it.user.id }
        .distinctUntilChanged()

    val timeline: StateFlow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.observeTimeline(ownerId)
    }

    val favorites: StateFlow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.observeFavorites(ownerId)
    }

    val albums = signedInUserId
        .flatMapLatest { ownerId ->
            memoryRepository.observeAlbums(ownerId)
                .map { values -> if (values.isEmpty()) UiState.Empty else UiState.Content(values) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(AppError.Unknown)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val trash = signedInUserId
        .flatMapLatest { ownerId ->
            memoryRepository.observeTrash(ownerId)
                .map { values -> if (values.isEmpty()) UiState.Empty else UiState.Content(values) }
                .onStart { emit(UiState.Loading) }
                .catch { emit(UiState.Error(AppError.Unknown)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val storageUsage: StateFlow<Long> = signedInUserId
        .flatMapLatest(memoryRepository::observeStorageUsage)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val activeUploadCount: StateFlow<Int> = memoryRepository.observeActiveUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _messages = MutableSharedFlow<AppMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val localPreviewEnabled: Boolean
        get() = !AppConfig.isSupabaseConfigured

    fun observeAlbum(albumId: String): Flow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.observeByAlbum(ownerId, albumId)
    }

    fun search(query: String): Flow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.search(ownerId, query)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password).onFailure { report(it) }
        }
    }

    fun signUp(username: String, email: String, password: String, confirmPassword: String) {
        if (username.isBlank()) {
            _messages.tryEmit(AppMessage.Text("Username wajib diisi."))
            return
        }
        if (password != confirmPassword) {
            _messages.tryEmit(AppMessage.Text("Confirm password belum sama."))
            return
        }
        viewModelScope.launch {
            authRepository.signUp(username, email, password).onFailure { report(it) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().onFailure { report(it) }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess { _messages.tryEmit(AppMessage.Text("Akun telah dihapus.")) }
                .onFailure { report(it) }
        }
    }

    fun importPhotos(uris: List<Uri>, albumId: String? = null) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            var imported = 0
            uris.forEach { uri ->
                when (memoryRepository.importPhoto(ownerId, uri, albumId)) {
                    is AppResult.Success -> imported++
                    is AppResult.Failure -> Unit
                }
            }
            if (imported > 0) {
                _messages.emit(AppMessage.Text("$imported foto ditambahkan ke vault."))
            } else {
                _messages.emit(AppMessage.Text("Tidak ada foto yang berhasil ditambahkan."))
            }
        }
    }

    fun createAlbum(name: String) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.createAlbum(ownerId, name)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Album dibuat.")) }
                .onFailure { report(it) }
        }
    }

    fun toggleFavorite(memory: Memory) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.toggleFavorite(ownerId, memory.id, !memory.isFavorite)
                .onFailure { report(it) }
        }
    }

    fun updateCaption(memory: Memory, caption: String) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.updateCaption(ownerId, memory.id, caption)
                .onFailure { report(it) }
        }
    }

    fun deleteMemory(memory: Memory) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.softDeleteMemory(ownerId, memory.id)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Foto dipindahkan ke trash.")) }
                .onFailure { report(it) }
        }
    }

    fun restoreTrash(item: TrashItem) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.restoreTrash(ownerId, item).onFailure { report(it) }
        }
    }

    fun permanentlyDelete(item: TrashItem) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.permanentlyDelete(ownerId, item).onFailure { report(it) }
        }
    }

    fun exportData(destination: Uri) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            exportManager.exportAll(ownerId, destination)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Export selesai.")) }
                .onFailure { report(it) }
        }
    }

    private fun <T> observeMemories(
        source: (String) -> Flow<List<T>>,
    ): StateFlow<UiState<List<T>>> {
        return signedInUserId
            .flatMapLatest { ownerId ->
                source(ownerId)
                    .map { values -> if (values.isEmpty()) UiState.Empty else UiState.Content(values) }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error(AppError.Unknown)) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
    }

    private fun report(error: AppError) {
        val message = when (error) {
            AppError.NotFound -> "Data tidak ditemukan."
            AppError.NetworkUnavailable -> "Koneksi belum tersedia."
            is AppError.Validation -> error.message
            is AppError.Remote -> error.message
            is AppError.Storage -> error.message
            AppError.AuthenticationRequired -> "Sesi diperlukan."
            AppError.Unknown -> "Terjadi kesalahan."
        }
        _messages.tryEmit(AppMessage.Text(message))
    }
}

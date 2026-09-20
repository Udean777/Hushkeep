package com.ssajudn.hushkeep.feature

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.UiState
import com.ssajudn.hushkeep.core.config.AppConfig
import com.ssajudn.hushkeep.core.media.ExportManager
import com.ssajudn.hushkeep.core.common.UserFacingMessages
import com.ssajudn.hushkeep.core.sync.RealtimeSyncCoordinator
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.MemorySearchFilters
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.StorageUsage
import com.ssajudn.hushkeep.domain.model.UploadJob
import com.ssajudn.hushkeep.domain.repository.AuthRepository
import com.ssajudn.hushkeep.domain.repository.AuthState
import com.ssajudn.hushkeep.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.awaitCancellation
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
    private val realtimeSyncCoordinator: RealtimeSyncCoordinator,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.state

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
        viewModelScope.launch {
            authState
                .collectLatest { signedIn ->
                    if (signedIn is AuthState.SignedIn) {
                        realtimeSyncCoordinator.start(signedIn.user.id)
                        try {
                            memoryRepository.refreshFromCloud(signedIn.user.id)
                            awaitCancellation()
                        } finally {
                            realtimeSyncCoordinator.stop()
                        }
                    } else {
                        realtimeSyncCoordinator.stop()
                    }
                }
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

    val storageUsage: StateFlow<StorageUsage> = signedInUserId
        .flatMapLatest(memoryRepository::observeStorageUsage)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageUsage(0L, 0L, 0L))

    val uploadJobs: StateFlow<List<UploadJob>> = signedInUserId
        .flatMapLatest(memoryRepository::observeUploadJobs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<AppMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val localPreviewEnabled: Boolean
        get() = !AppConfig.isSupabaseConfigured

    fun observeAlbum(albumId: String): Flow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.observeByAlbum(ownerId, albumId)
    }

    fun search(filters: MemorySearchFilters): Flow<UiState<List<Memory>>> = observeMemories { ownerId ->
        memoryRepository.search(ownerId, filters)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password).onFailure { report(it) }
        }
    }

    fun refreshCloud() {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch { memoryRepository.refreshFromCloud(ownerId) }
    }

    fun ensureRealtime() {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch { realtimeSyncCoordinator.start(ownerId) }
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

    fun verifySignupOtp(email: String, token: String) {
        viewModelScope.launch {
            authRepository.verifySignupOtp(email, token).onFailure { report(it) }
        }
    }

    fun resendSignupOtp(email: String) {
        viewModelScope.launch {
            authRepository.resendSignupOtp(email).onFailure { report(it) }
        }
    }

    fun deleteAccount(password: String) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            val reauth = authRepository.reauthenticate(password)
            if (reauth is AppResult.Failure) {
                report(reauth.error)
                return@launch
            }
            val deleted = authRepository.deleteAccount()
            if (deleted is AppResult.Failure) {
                report(deleted.error)
                return@launch
            }
            memoryRepository.clearLocalData(ownerId)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Akun dan data lokal telah dihapus.")) }
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

    fun renameAlbum(albumId: String, name: String) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.renameAlbum(ownerId, albumId, name)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Album diubah.")) }
                .onFailure { report(it) }
        }
    }

    fun deleteAlbum(albumId: String) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.deleteAlbum(ownerId, albumId)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Album dipindahkan ke trash.")) }
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

    fun retryUpload(job: UploadJob) {
        viewModelScope.launch {
            memoryRepository.retryUpload(job.id).onFailure { report(it) }
        }
    }

    fun retryFailedUploads() {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            memoryRepository.retryFailedUploads(ownerId).onFailure { report(it) }
        }
    }

    fun cancelUpload(job: UploadJob) {
        viewModelScope.launch {
            memoryRepository.cancelUpload(job.id).onFailure { report(it) }
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

    fun exportMemory(memoryId: String, destination: Uri) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            exportManager.exportMemory(ownerId, memoryId, destination)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Foto berhasil diekspor.")) }
                .onFailure { report(it) }
        }
    }

    fun downloadPhoto(memoryId: String, destination: Uri) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            exportManager.downloadPhoto(ownerId, memoryId, destination)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Foto berhasil diunduh.")) }
                .onFailure { report(it) }
        }
    }

    fun exportAlbum(albumId: String, destination: Uri) {
        val ownerId = (authState.value as? AuthState.SignedIn)?.user?.id ?: return
        viewModelScope.launch {
            exportManager.exportAlbum(ownerId, albumId, destination)
                .onSuccess { _messages.tryEmit(AppMessage.Text("Album berhasil diekspor.")) }
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
        _messages.tryEmit(AppMessage.Text(UserFacingMessages.forError(error)))
    }

    override fun onCleared() {
        realtimeSyncCoordinator.close()
        super.onCleared()
    }
}

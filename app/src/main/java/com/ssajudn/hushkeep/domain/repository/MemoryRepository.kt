package com.ssajudn.hushkeep.domain.repository

import android.net.Uri
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.domain.model.Album
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.MemorySearchFilters
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.StorageUsage
import com.ssajudn.hushkeep.domain.model.UploadJob
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeTimeline(ownerId: String): Flow<List<Memory>>

    fun observeFavorites(ownerId: String): Flow<List<Memory>>

    fun observeByAlbum(ownerId: String, albumId: String): Flow<List<Memory>>

    fun search(ownerId: String, filters: MemorySearchFilters): Flow<List<Memory>>

    fun observeAlbums(ownerId: String): Flow<List<Album>>

    fun observeTrash(ownerId: String): Flow<List<TrashItem>>

    fun observeStorageUsage(ownerId: String): Flow<StorageUsage>

    fun observeUploadJobs(ownerId: String): Flow<List<UploadJob>>

    suspend fun refreshFromCloud(ownerId: String): AppResult<Unit>

    suspend fun importPhoto(ownerId: String, uri: Uri, albumId: String? = null): AppResult<Memory>

    suspend fun createAlbum(ownerId: String, name: String): AppResult<Album>

    suspend fun renameAlbum(ownerId: String, albumId: String, name: String): AppResult<Unit>

    suspend fun deleteAlbum(ownerId: String, albumId: String): AppResult<Unit>

    suspend fun toggleFavorite(ownerId: String, memoryId: String, isFavorite: Boolean): AppResult<Unit>

    suspend fun updateCaption(ownerId: String, memoryId: String, caption: String?): AppResult<Unit>

    suspend fun softDeleteMemory(ownerId: String, memoryId: String): AppResult<Unit>

    suspend fun restoreTrash(ownerId: String, item: TrashItem): AppResult<Unit>

    suspend fun permanentlyDelete(ownerId: String, item: TrashItem): AppResult<Unit>

    suspend fun retryUpload(jobId: String): AppResult<Unit>

    suspend fun retryFailedUploads(ownerId: String): AppResult<Unit>

    suspend fun cancelUpload(jobId: String): AppResult<Unit>

    suspend fun clearLocalData(ownerId: String): AppResult<Unit>
}

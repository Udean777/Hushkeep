package com.ssajudn.hushkeep.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.ssajudn.hushkeep.core.common.AppError
import com.ssajudn.hushkeep.core.common.AppResult
import com.ssajudn.hushkeep.core.common.Clock
import com.ssajudn.hushkeep.core.common.MimeTypeResolver
import com.ssajudn.hushkeep.core.common.UriResolver
import com.ssajudn.hushkeep.core.config.MediaConstraints
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.data.local.dao.AlbumDao
import com.ssajudn.hushkeep.data.local.dao.MemoryDao
import com.ssajudn.hushkeep.data.local.mapper.toDomain
import com.ssajudn.hushkeep.data.local.mapper.toEntity
import com.ssajudn.hushkeep.domain.model.Album
import com.ssajudn.hushkeep.domain.model.MediaObject
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.MemorySearchFilters
import com.ssajudn.hushkeep.domain.model.SyncState
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.TrashResourceType
import com.ssajudn.hushkeep.domain.model.StorageUsage
import com.ssajudn.hushkeep.domain.model.UploadJob
import com.ssajudn.hushkeep.domain.repository.MemoryRepository
import com.ssajudn.hushkeep.core.sync.UploadQueue
import com.ssajudn.hushkeep.core.sync.CloudSyncQueue
import com.ssajudn.hushkeep.core.sync.SyncConflictResolver
import com.ssajudn.hushkeep.data.remote.RemoteAlbum
import com.ssajudn.hushkeep.data.remote.RemoteMediaObject
import com.ssajudn.hushkeep.data.remote.RemoteMemory
import com.ssajudn.hushkeep.data.remote.SupabaseMemoryDataSource
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class LocalMemoryRepository(
    private val database: HushkeepDatabase,
    private val contentResolver: ContentResolver,
    private val mediaDirectory: File,
    workManager: WorkManager,
    private val supabaseClient: SupabaseClient? = null,
    private val clock: Clock = Clock.System,
) : MemoryRepository {
    private val uriResolver = UriResolver(contentResolver)
    private val albumDao: AlbumDao = database.albumDao()
    private val memoryDao: MemoryDao = database.memoryDao()
    private val uploadQueue = UploadQueue(database.uploadJobDao(), workManager, clock)
    private val cloudSyncQueue = CloudSyncQueue(workManager)
    private val cloudSource = supabaseClient?.let(::SupabaseMemoryDataSource)

    override fun observeTimeline(ownerId: String): Flow<List<Memory>> =
        memoryDao.observeTimeline(ownerId).map { memories -> memories.map { it.toDomain() } }

    override fun observeFavorites(ownerId: String): Flow<List<Memory>> =
        memoryDao.observeFavorites(ownerId).map { memories -> memories.map { it.toDomain() } }

    override fun observeByAlbum(ownerId: String, albumId: String): Flow<List<Memory>> =
        memoryDao.observeByAlbum(ownerId, albumId)
            .map { memories -> memories.map { it.toDomain() } }

    override fun search(ownerId: String, filters: MemorySearchFilters): Flow<List<Memory>> =
        memoryDao.search(
            ownerId = ownerId,
            query = filters.query.trim(),
            albumId = filters.albumId,
            fromEpochMs = filters.fromEpochMs,
            toEpochMsExclusive = filters.toEpochMsExclusive,
        ).map { memories -> memories.map { it.toDomain() } }

    override fun observeAlbums(ownerId: String): Flow<List<Album>> =
        albumDao.observeActive(ownerId).map { albums -> albums.map { it.toDomain() } }

    override fun observeTrash(ownerId: String): Flow<List<TrashItem>> = combine(
        memoryDao.observeTrash(ownerId),
        albumDao.observeTrash(ownerId),
    ) { memories, albums ->
        buildList {
            memories.forEach { memory ->
                memory.deletedAtEpochMs?.let { deletedAt ->
                    add(
                        TrashItem(
                            id = memory.id,
                            ownerId = memory.ownerId,
                            resourceType = TrashResourceType.MEMORY,
                            deletedAt = Instant.ofEpochMilli(deletedAt),
                            expiresAt = null,
                        ),
                    )
                }
            }
            albums.forEach { album ->
                album.deletedAtEpochMs?.let { deletedAt ->
                    add(
                        TrashItem(
                            id = album.id,
                            ownerId = album.ownerId,
                            resourceType = TrashResourceType.ALBUM,
                            deletedAt = Instant.ofEpochMilli(deletedAt),
                            expiresAt = null,
                        ),
                    )
                }
            }
        }.sortedByDescending(TrashItem::deletedAt)
    }

    override fun observeStorageUsage(ownerId: String): Flow<StorageUsage> = combine(
        database.mediaObjectDao().observeLocalStorageUsage(ownerId),
        database.mediaObjectDao().observeCloudStorageUsage(ownerId),
        database.mediaObjectDao().observePendingStorageUsage(ownerId),
    ) { local, cloud, pending ->
        StorageUsage(localBytes = local, cloudBytes = cloud, pendingBytes = pending)
    }

    override fun observeUploadJobs(ownerId: String): Flow<List<UploadJob>> =
        database.uploadJobDao().observeForOwner(ownerId).map { jobs -> jobs.map { it.toDomain() } }

    override suspend fun refreshFromCloud(ownerId: String): AppResult<Unit> {
        val source = cloudSource ?: return AppResult.Success(Unit)
        return runCatching {
            val remoteAlbums = source.fetchAlbums(ownerId)
            val remoteMemories = source.fetchMemories(ownerId)
            val remoteMedia = source.fetchMedia(ownerId)
            val existingMemories = memoryDao.findAllForOwner(ownerId).associateBy { it.id }
            val existingMedia = database.mediaObjectDao().findAll(ownerId).associateBy { it.id }
            val signedUrls = source.signedUrls(remoteMedia.map { it.storagePath })

            database.withTransaction {
                albumDao.upsertAll(
                    remoteAlbums.map { remote ->
                        val incoming = remote.toEntity(albumDao.findById(remote.id))
                        val existing = albumDao.findById(remote.id)
                        if (existing != null && SyncConflictResolver.shouldKeepLocal(
                                existing.updatedAtEpochMs,
                                incoming.updatedAtEpochMs,
                            )
                        ) existing else incoming
                    },
                )
                database.mediaObjectDao().upsertAll(
                    remoteMedia.map { remote ->
                        val incoming = remote.toEntity(existingMedia[remote.id])
                        val existing = existingMedia[remote.id]
                        if (existing != null && SyncConflictResolver.shouldKeepLocal(
                                existing.updatedAtEpochMs,
                                incoming.updatedAtEpochMs,
                                existing.syncState,
                            )
                        ) existing else incoming
                    },
                )
                memoryDao.upsertAll(
                    remoteMemories.map { remote ->
                        val existing = existingMemories[remote.id]
                        val incoming = remote.toEntity(
                            existing = existing,
                            remoteUrl = remoteMedia
                                .firstOrNull { it.memoryId == remote.id }
                                ?.storagePath
                                ?.let(signedUrls::get),
                        )
                        if (existing != null && SyncConflictResolver.shouldKeepLocal(
                                existing.updatedAtEpochMs,
                                incoming.updatedAtEpochMs,
                                existing.syncState,
                            )
                        ) {
                            existing
                        } else {
                            incoming
                        }
                    },
                )
            }
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Failure(AppError.Remote("Cloud belum dapat dimuat.", it)) },
        )
    }

    override suspend fun importPhoto(
        ownerId: String,
        uri: Uri,
        albumId: String?,
    ): AppResult<Memory> {
        val metadata = uriResolver.inspect(uri)
        val mimeType = metadata.mimeType
            ?: return AppResult.Failure(AppError.Validation("Format foto tidak dikenali."))

        if (mimeType !in MediaConstraints.supportedImageMimeTypes) {
            return AppResult.Failure(AppError.Validation("Format $mimeType belum didukung."))
        }

        val sizeBytes = metadata.sizeBytes ?: 0L
        if (sizeBytes > MediaConstraints.MAX_UPLOAD_BYTES) {
            return AppResult.Failure(AppError.Validation("Ukuran foto melebihi batas MVP."))
        }

        val now = clock.now()
        val memoryId = UUID.randomUUID().toString()
        val mediaId = UUID.randomUUID().toString()
        val localFile = File(
            mediaDirectory,
            "$mediaId.${MimeTypeResolver.extensionForMimeType(mimeType) ?: "img"}",
        )
        val copyError = try {
            withContext(Dispatchers.IO) {
                check(mediaDirectory.mkdirs() || mediaDirectory.isDirectory) {
                    "Tidak dapat menyiapkan penyimpanan media."
                }
                uriResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Foto tidak dapat dibaca.")
                check(localFile.isFile && localFile.length() > 0L) {
                    "Foto yang dipilih kosong."
                }
            }
            null
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            error
        }
        if (copyError != null) {
            localFile.delete()
            return AppResult.Failure(
                AppError.Storage(
                    "Foto belum dapat disimpan ke ruang pribadi.",
                    copyError,
                ),
            )
        }
        val storedSizeBytes = localFile.length()
        if (storedSizeBytes > MediaConstraints.MAX_UPLOAD_BYTES) {
            localFile.delete()
            return AppResult.Failure(AppError.Validation("Ukuran foto melebihi batas MVP."))
        }
        val localUri = Uri.fromFile(localFile).toString()
        val memory = Memory(
            id = memoryId,
            ownerId = ownerId,
            albumId = albumId,
            caption = null,
            capturedAt = now,
            createdAt = now,
            updatedAt = now,
            isFavorite = false,
            syncState = SyncState.PENDING,
            deletedAt = null,
            localUri = localUri,
        )
        val mediaObject = MediaObject(
            id = mediaId,
            memoryId = memoryId,
            ownerId = ownerId,
            fileName = metadata.displayName ?: "memory-$mediaId",
            mimeType = mimeType,
            sizeBytes = storedSizeBytes,
            width = null,
            height = null,
            localUri = localUri,
            storagePath = null,
            syncState = SyncState.PENDING,
            createdAt = now,
            updatedAt = now,
        )

        database.withTransaction {
            memoryDao.upsert(memory.toEntity())
            database.mediaObjectDao().upsert(mediaObject.toEntity())
        }
        uploadQueue.enqueue(
            mediaObjectId = mediaId,
            localUri = localUri,
            fileName = metadata.displayName,
            totalBytes = storedSizeBytes,
        )
        cloudSyncQueue.enqueue(ownerId)
        return AppResult.Success(memory)
    }

    override suspend fun createAlbum(ownerId: String, name: String): AppResult<Album> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return AppResult.Failure(AppError.Validation("Nama album wajib diisi."))
        }

        val now = clock.now()
        val album = Album(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = cleanName,
            coverMemoryId = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        albumDao.upsert(album.toEntity())
        cloudSyncQueue.enqueue(ownerId)
        return AppResult.Success(album)
    }

    override suspend fun renameAlbum(ownerId: String, albumId: String, name: String): AppResult<Unit> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return AppResult.Failure(AppError.Validation("Nama album wajib diisi."))
        val updated = albumDao.rename(albumId, ownerId, cleanName, clock.now().toEpochMilli())
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun deleteAlbum(ownerId: String, albumId: String): AppResult<Unit> {
        val now = clock.now().toEpochMilli()
        val updated = database.withTransaction {
            val deleted = albumDao.softDelete(albumId, ownerId, now, now)
            if (deleted > 0) memoryDao.detachFromAlbum(ownerId, albumId, now)
            deleted
        }
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun toggleFavorite(
        ownerId: String,
        memoryId: String,
        isFavorite: Boolean,
    ): AppResult<Unit> {
        val updated =
            memoryDao.setFavorite(memoryId, ownerId, isFavorite, clock.now().toEpochMilli())
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun updateCaption(
        ownerId: String,
        memoryId: String,
        caption: String?,
    ): AppResult<Unit> {
        val cleanCaption = caption?.trim()?.takeIf(String::isNotBlank)
        if (cleanCaption != null && cleanCaption.length > 280) {
            return AppResult.Failure(AppError.Validation("Caption maksimal 280 karakter."))
        }
        val updated = memoryDao.updateCaption(
            memoryId = memoryId,
            ownerId = ownerId,
            caption = cleanCaption,
            updatedAtEpochMs = clock.now().toEpochMilli(),
        )
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun softDeleteMemory(ownerId: String, memoryId: String): AppResult<Unit> {
        val now = clock.now().toEpochMilli()
        val updated = memoryDao.softDelete(memoryId, ownerId, now, now)
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun restoreTrash(ownerId: String, item: TrashItem): AppResult<Unit> {
        val updated = when (item.resourceType) {
            TrashResourceType.MEMORY -> memoryDao.restore(
                item.id,
                ownerId,
                clock.now().toEpochMilli()
            )

            TrashResourceType.ALBUM -> albumDao.restore(
                item.id,
                ownerId,
                clock.now().toEpochMilli()
            )
        }
        if (updated > 0) cloudSyncQueue.enqueue(ownerId)
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun permanentlyDelete(ownerId: String, item: TrashItem): AppResult<Unit> {
        if (item.resourceType == TrashResourceType.MEMORY) {
            val mediaObjects = database.mediaObjectDao().findForMemory(item.id)
            try {
                cloudSource?.deleteObjects(mediaObjects.mapNotNull { it.storagePath })
                cloudSource?.deleteMemory(ownerId, item.id)
            } catch (error: Throwable) {
                return AppResult.Failure(AppError.Remote("Media cloud belum dapat dihapus.", error))
            }
        } else if (item.resourceType == TrashResourceType.ALBUM) {
            try {
                cloudSource?.deleteAlbum(ownerId, item.id)
            } catch (error: Throwable) {
                return AppResult.Failure(AppError.Remote("Album cloud belum dapat dihapus.", error))
            }
        }
        val deleted = when (item.resourceType) {
            TrashResourceType.MEMORY -> database.withTransaction {
                val mediaObjects = database.mediaObjectDao().findForMemory(item.id)
                mediaObjects.forEach { media ->
                    database.uploadJobDao().deleteForMediaObject(media.id)
                    media.localUri?.let(::deleteLocalFile)
                }
                database.mediaObjectDao().deleteForMemory(item.id)
                memoryDao.permanentlyDelete(item.id, ownerId)
            }
            TrashResourceType.ALBUM -> albumDao.permanentlyDelete(item.id, ownerId)
        }
        if (deleted > 0) cloudSyncQueue.enqueue(ownerId)
        return if (deleted > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun retryUpload(jobId: String): AppResult<Unit> =
        if (uploadQueue.retry(jobId)) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)

    override suspend fun retryFailedUploads(ownerId: String): AppResult<Unit> {
        uploadQueue.retryAll(ownerId)
        return AppResult.Success(Unit)
    }

    override suspend fun cancelUpload(jobId: String): AppResult<Unit> =
        if (uploadQueue.cancel(jobId)) AppResult.Success(Unit) else AppResult.Failure(AppError.NotFound)

    override suspend fun clearLocalData(ownerId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                database.uploadJobDao().deleteAllForOwner(ownerId)
                database.mediaObjectDao().deleteAllForOwner(ownerId)
                memoryDao.deleteAllForOwner(ownerId)
                albumDao.deleteAllForOwner(ownerId)
            }
            mediaDirectory.listFiles()?.forEach { file -> file.delete() }
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Failure(AppError.Storage("Data lokal belum dapat dibersihkan.", it)) },
        )
    }

    private fun deleteLocalFile(localUri: String) {
        val uri = Uri.parse(localUri)
        if (uri.scheme == "file") {
            uri.path?.let(::File)?.delete()
        }
    }

    private fun RemoteAlbum.toEntity(
        existing: com.ssajudn.hushkeep.data.local.entity.AlbumEntity?,
    ) = com.ssajudn.hushkeep.data.local.entity.AlbumEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        coverMemoryId = existing?.coverMemoryId,
        createdAtEpochMs = parseInstant(createdAt).toEpochMilli(),
        updatedAtEpochMs = parseInstant(updatedAt).toEpochMilli(),
        deletedAtEpochMs = deletedAt?.let(::parseInstant)?.toEpochMilli(),
    )

    private fun RemoteMemory.toEntity(
        existing: com.ssajudn.hushkeep.data.local.entity.MemoryEntity?,
        remoteUrl: String?,
    ) = com.ssajudn.hushkeep.data.local.entity.MemoryEntity(
        id = id,
        ownerId = ownerId,
        albumId = albumId,
        caption = caption,
        capturedAtEpochMs = parseInstant(capturedAt).toEpochMilli(),
        createdAtEpochMs = parseInstant(createdAt).toEpochMilli(),
        updatedAtEpochMs = parseInstant(updatedAt).toEpochMilli(),
        isFavorite = isFavorite,
        syncState = SyncState.SYNCED.name,
        deletedAtEpochMs = deletedAt?.let(::parseInstant)?.toEpochMilli(),
        localUri = existing?.localUri,
        remoteUrl = remoteUrl,
    )

    private fun RemoteMediaObject.toEntity(
        existing: com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity?,
    ) = com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity(
        id = id,
        memoryId = memoryId,
        ownerId = ownerId,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        localUri = existing?.localUri,
        storagePath = storagePath,
        syncState = SyncState.SYNCED.name,
        createdAtEpochMs = parseInstant(createdAt).toEpochMilli(),
        updatedAtEpochMs = parseInstant(updatedAt).toEpochMilli(),
    )

    private fun parseInstant(value: String): Instant = runCatching {
        Instant.parse(value)
    }.getOrElse {
        OffsetDateTime.parse(value).toInstant()
    }
}

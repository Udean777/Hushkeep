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
import com.ssajudn.hushkeep.domain.model.SyncState
import com.ssajudn.hushkeep.domain.model.TrashItem
import com.ssajudn.hushkeep.domain.model.TrashResourceType
import com.ssajudn.hushkeep.domain.repository.MemoryRepository
import com.ssajudn.hushkeep.core.sync.UploadQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID

class LocalMemoryRepository(
    private val database: HushkeepDatabase,
    private val contentResolver: ContentResolver,
    private val mediaDirectory: File,
    workManager: WorkManager,
    private val clock: Clock = Clock.System,
) : MemoryRepository {
    private val uriResolver = UriResolver(contentResolver)
    private val albumDao: AlbumDao = database.albumDao()
    private val memoryDao: MemoryDao = database.memoryDao()
    private val uploadQueue = UploadQueue(database.uploadJobDao(), workManager, clock)

    override fun observeTimeline(ownerId: String): Flow<List<Memory>> =
        memoryDao.observeTimeline(ownerId).map { memories -> memories.map { it.toDomain() } }

    override fun observeFavorites(ownerId: String): Flow<List<Memory>> =
        memoryDao.observeFavorites(ownerId).map { memories -> memories.map { it.toDomain() } }

    override fun observeByAlbum(ownerId: String, albumId: String): Flow<List<Memory>> =
        memoryDao.observeByAlbum(ownerId, albumId)
            .map { memories -> memories.map { it.toDomain() } }

    override fun search(ownerId: String, query: String): Flow<List<Memory>> =
        memoryDao.search(ownerId, query.trim()).map { memories -> memories.map { it.toDomain() } }

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

    override fun observeStorageUsage(ownerId: String): Flow<Long> =
        database.mediaObjectDao().observeStorageUsage(ownerId)

    override fun observeActiveUploadCount(): Flow<Int> =
        database.uploadJobDao().observeActiveCount()

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
        uploadQueue.enqueue(mediaId, localUri)
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
        return AppResult.Success(album)
    }

    override suspend fun toggleFavorite(
        ownerId: String,
        memoryId: String,
        isFavorite: Boolean,
    ): AppResult<Unit> {
        val updated =
            memoryDao.setFavorite(memoryId, ownerId, isFavorite, clock.now().toEpochMilli())
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun updateCaption(
        ownerId: String,
        memoryId: String,
        caption: String?,
    ): AppResult<Unit> {
        val updated = memoryDao.updateCaption(
            memoryId = memoryId,
            ownerId = ownerId,
            caption = caption?.trim()?.takeIf(String::isNotBlank),
            updatedAtEpochMs = clock.now().toEpochMilli(),
        )
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun softDeleteMemory(ownerId: String, memoryId: String): AppResult<Unit> {
        val now = clock.now().toEpochMilli()
        val updated = memoryDao.softDelete(memoryId, ownerId, now, now)
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
        return if (updated > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    override suspend fun permanentlyDelete(ownerId: String, item: TrashItem): AppResult<Unit> {
        val deleted = when (item.resourceType) {
            TrashResourceType.MEMORY -> {
                database.withTransaction {
                    val mediaObjects = database.mediaObjectDao().findForMemory(item.id)
                    mediaObjects.forEach { media ->
                        database.uploadJobDao().deleteForMediaObject(media.id)
                        media.localUri?.let(::deleteLocalFile)
                    }
                    database.mediaObjectDao().deleteForMemory(item.id)
                    memoryDao.permanentlyDelete(item.id, ownerId)
                }
            }

            TrashResourceType.ALBUM -> albumDao.permanentlyDelete(item.id, ownerId)
        }
        return if (deleted > 0) AppResult.Success(Unit)
        else AppResult.Failure(AppError.NotFound)
    }

    private fun deleteLocalFile(localUri: String) {
        val uri = Uri.parse(localUri)
        if (uri.scheme == "file") {
            uri.path?.let(::File)?.delete()
        }
    }
}

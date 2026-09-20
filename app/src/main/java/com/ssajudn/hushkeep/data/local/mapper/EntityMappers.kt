package com.ssajudn.hushkeep.data.local.mapper

import com.ssajudn.hushkeep.data.local.entity.AlbumEntity
import com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity
import com.ssajudn.hushkeep.data.local.entity.MemoryEntity
import com.ssajudn.hushkeep.data.local.entity.UploadJobEntity
import com.ssajudn.hushkeep.domain.model.Album
import com.ssajudn.hushkeep.domain.model.MediaObject
import com.ssajudn.hushkeep.domain.model.Memory
import com.ssajudn.hushkeep.domain.model.SyncState
import com.ssajudn.hushkeep.domain.model.UploadJob
import com.ssajudn.hushkeep.domain.model.UploadStatus
import java.time.Instant

fun AlbumEntity.toDomain(): Album = Album(
    id = id,
    ownerId = ownerId,
    name = name,
    coverMemoryId = coverMemoryId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
    deletedAt = deletedAtEpochMs?.let(Instant::ofEpochMilli),
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    coverMemoryId = coverMemoryId,
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    deletedAtEpochMs = deletedAt?.toEpochMilli(),
)

fun MemoryEntity.toDomain(): Memory = Memory(
    id = id,
    ownerId = ownerId,
    albumId = albumId,
    caption = caption,
    capturedAt = Instant.ofEpochMilli(capturedAtEpochMs),
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
    isFavorite = isFavorite,
    syncState = SyncState.fromStorage(syncState),
    deletedAt = deletedAtEpochMs?.let(Instant::ofEpochMilli),
    localUri = localUri,
    remoteUrl = remoteUrl,
)

fun Memory.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    ownerId = ownerId,
    albumId = albumId,
    caption = caption,
    capturedAtEpochMs = capturedAt.toEpochMilli(),
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    isFavorite = isFavorite,
    syncState = syncState.name,
    deletedAtEpochMs = deletedAt?.toEpochMilli(),
    localUri = localUri,
    remoteUrl = remoteUrl,
)

fun MediaObjectEntity.toDomain(): MediaObject = MediaObject(
    id = id,
    memoryId = memoryId,
    ownerId = ownerId,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    localUri = localUri,
    storagePath = storagePath,
    syncState = SyncState.fromStorage(syncState),
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
)

fun MediaObject.toEntity(): MediaObjectEntity = MediaObjectEntity(
    id = id,
    memoryId = memoryId,
    ownerId = ownerId,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    localUri = localUri,
    storagePath = storagePath,
    syncState = syncState.name,
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
)

fun UploadJobEntity.toDomain(): UploadJob = UploadJob(
    id = id,
    mediaObjectId = mediaObjectId,
    localUri = localUri,
    fileName = fileName,
    totalBytes = totalBytes,
    bytesTransferred = bytesTransferred,
    progressPercent = progressPercent,
    status = UploadStatus.fromStorage(status),
    attemptCount = attemptCount,
    lastError = lastError,
    nextAttemptAt = nextAttemptAtEpochMs?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
)

fun UploadJob.toEntity(): UploadJobEntity = UploadJobEntity(
    id = id,
    mediaObjectId = mediaObjectId,
    localUri = localUri,
    fileName = fileName,
    totalBytes = totalBytes,
    bytesTransferred = bytesTransferred,
    progressPercent = progressPercent,
    status = status.name,
    attemptCount = attemptCount,
    lastError = lastError,
    nextAttemptAtEpochMs = nextAttemptAt?.toEpochMilli(),
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
)

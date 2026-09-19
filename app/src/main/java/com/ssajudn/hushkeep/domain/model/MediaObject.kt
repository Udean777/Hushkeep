package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class MediaObject(
    val id: String,
    val memoryId: String,
    val ownerId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val localUri: String?,
    val storagePath: String?,
    val syncState: SyncState,
    val createdAt: Instant,
    val updatedAt: Instant,
)

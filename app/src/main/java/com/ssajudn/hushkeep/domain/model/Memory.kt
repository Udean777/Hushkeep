package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class Memory(
    val id: String,
    val ownerId: String,
    val albumId: String?,
    val caption: String?,
    val capturedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isFavorite: Boolean,
    val syncState: SyncState,
    val deletedAt: Instant?,
    val localUri: String? = null,
    val remoteUrl: String? = null,
)

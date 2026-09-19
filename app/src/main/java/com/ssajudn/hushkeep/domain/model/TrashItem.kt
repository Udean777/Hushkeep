package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class TrashItem(
    val id: String,
    val ownerId: String,
    val resourceType: TrashResourceType,
    val deletedAt: Instant,
    val expiresAt: Instant?,
)

enum class TrashResourceType {
    MEMORY,
    ALBUM,
}

package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class Album(
    val id: String,
    val ownerId: String,
    val name: String,
    val coverMemoryId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class UserProfile(
    val id: String,
    val email: String?,
    val displayName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

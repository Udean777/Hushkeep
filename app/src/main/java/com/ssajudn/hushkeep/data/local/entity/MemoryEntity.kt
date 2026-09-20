package com.ssajudn.hushkeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["albumId"]),
        Index(value = ["capturedAtEpochMs"]),
        Index(value = ["deletedAtEpochMs"]),
    ],
)
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val albumId: String?,
    val caption: String?,
    val capturedAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isFavorite: Boolean,
    val syncState: String,
    val deletedAtEpochMs: Long?,
    val localUri: String?,
    val remoteUrl: String?,
)

package com.ssajudn.hushkeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["deletedAtEpochMs"]),
    ],
)
data class AlbumEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val ownerId: String,
    val name: String,
    val coverMemoryId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val deletedAtEpochMs: Long?,
)

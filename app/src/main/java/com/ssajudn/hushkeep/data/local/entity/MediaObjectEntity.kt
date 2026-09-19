package com.ssajudn.hushkeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_objects",
    indices = [
        Index(value = ["memoryId"]),
        Index(value = ["ownerId"]),
        Index(value = ["syncState"]),
    ],
)
data class MediaObjectEntity(
    @PrimaryKey
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
    val syncState: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

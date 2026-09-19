package com.ssajudn.hushkeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "upload_jobs",
    indices = [
        Index(value = ["mediaObjectId"]),
        Index(value = ["status"]),
        Index(value = ["nextAttemptAtEpochMs"]),
    ],
)
data class UploadJobEntity(
    @PrimaryKey
    val id: String,
    val mediaObjectId: String,
    val localUri: String,
    val status: String,
    val attemptCount: Int,
    val lastError: String?,
    val nextAttemptAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

package com.ssajudn.hushkeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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
    val fileName: String?,
    @ColumnInfo(defaultValue = "0")
    val totalBytes: Long,
    @ColumnInfo(defaultValue = "0")
    val bytesTransferred: Long,
    @ColumnInfo(defaultValue = "0")
    val progressPercent: Int,
    val status: String,
    val attemptCount: Int,
    val lastError: String?,
    val nextAttemptAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

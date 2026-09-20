package com.ssajudn.hushkeep.domain.model

import java.time.Instant

data class UploadJob(
    val id: String,
    val mediaObjectId: String,
    val localUri: String,
    val fileName: String?,
    val totalBytes: Long,
    val bytesTransferred: Long,
    val progressPercent: Int,
    val status: UploadStatus,
    val attemptCount: Int,
    val lastError: String?,
    val nextAttemptAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class UploadStatus {
    QUEUED,
    PREPARING,
    READY_FOR_UPLOAD,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromStorage(value: String): UploadStatus {
            return entries.firstOrNull { it.name == value } ?: QUEUED
        }
    }
}

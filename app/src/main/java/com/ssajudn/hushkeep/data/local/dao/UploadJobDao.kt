package com.ssajudn.hushkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssajudn.hushkeep.data.local.entity.UploadJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadJobDao {
    @Query("SELECT * FROM upload_jobs WHERE id = :jobId LIMIT 1")
    suspend fun findById(jobId: String): UploadJobEntity?

    @Query(
        """
        SELECT * FROM upload_jobs
        WHERE status IN ('QUEUED', 'FAILED')
          AND (nextAttemptAtEpochMs IS NULL OR nextAttemptAtEpochMs <= :nowEpochMs)
        ORDER BY createdAtEpochMs ASC
        """,
    )
    fun observeReady(nowEpochMs: Long): Flow<List<UploadJobEntity>>

    @Query(
        """
        UPDATE upload_jobs
        SET status = :status, attemptCount = :attemptCount, lastError = :lastError,
            nextAttemptAtEpochMs = :nextAttemptAtEpochMs, updatedAtEpochMs = :updatedAtEpochMs
        WHERE id = :jobId
        """,
    )
    suspend fun updateStatus(
        jobId: String,
        status: String,
        attemptCount: Int,
        lastError: String?,
        nextAttemptAtEpochMs: Long?,
        updatedAtEpochMs: Long,
    ): Int

    @Upsert
    suspend fun upsert(job: UploadJobEntity)

    @Query("DELETE FROM upload_jobs WHERE id = :jobId")
    suspend fun delete(jobId: String): Int

    @Query("DELETE FROM upload_jobs WHERE mediaObjectId = :mediaObjectId")
    suspend fun deleteForMediaObject(mediaObjectId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM upload_jobs
        WHERE status IN ('QUEUED', 'PREPARING', 'READY_FOR_UPLOAD', 'UPLOADING', 'FAILED')
        """,
    )
    fun observeActiveCount(): Flow<Int>
}

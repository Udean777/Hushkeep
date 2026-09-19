package com.ssajudn.hushkeep.core.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.ssajudn.hushkeep.core.common.Clock
import com.ssajudn.hushkeep.core.config.SyncPolicy
import com.ssajudn.hushkeep.data.local.dao.UploadJobDao
import com.ssajudn.hushkeep.data.local.mapper.toDomain
import com.ssajudn.hushkeep.data.local.mapper.toEntity
import com.ssajudn.hushkeep.domain.model.UploadJob
import com.ssajudn.hushkeep.domain.model.UploadStatus
import java.util.UUID

class UploadQueue(
    private val uploadJobDao: UploadJobDao,
    private val workManager: WorkManager,
    private val clock: Clock = Clock.System,
) {
    suspend fun enqueue(
        mediaObjectId: String,
        localUri: String,
        jobId: String = UUID.randomUUID().toString(),
    ): UploadJob {
        val now = clock.now()
        val job = UploadJob(
            id = jobId,
            mediaObjectId = mediaObjectId,
            localUri = localUri,
            status = UploadStatus.QUEUED,
            attemptCount = 0,
            lastError = null,
            nextAttemptAt = null,
            createdAt = now,
            updatedAt = now,
        )

        uploadJobDao.upsert(job.toEntity())
        workManager.enqueueUniqueWork(
            uniqueWorkName(job.id),
            ExistingWorkPolicy.KEEP,
            UploadWorkRequestFactory.create(job.id),
        )
        return job
    }

    suspend fun cancel(jobId: String): Boolean {
        val existing = uploadJobDao.findById(jobId) ?: return false
        val updated = uploadJobDao.updateStatus(
            jobId = existing.id,
            status = UploadStatus.CANCELLED.name,
            attemptCount = existing.attemptCount,
            lastError = existing.lastError,
            nextAttemptAtEpochMs = null,
            updatedAtEpochMs = clock.now().toEpochMilli(),
        )
        workManager.cancelUniqueWork(uniqueWorkName(jobId))
        return updated > 0
    }

    fun uniqueWorkName(jobId: String): String =
        "${SyncPolicy.UPLOAD_WORK_NAME}:$jobId"

    suspend fun find(jobId: String): UploadJob? =
        uploadJobDao.findById(jobId)?.toDomain()
}

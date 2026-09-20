package com.ssajudn.hushkeep.core.sync

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssajudn.hushkeep.core.common.Clock
import com.ssajudn.hushkeep.core.common.MimeTypeResolver
import com.ssajudn.hushkeep.core.common.RetryPolicy
import com.ssajudn.hushkeep.core.common.UserFacingMessages
import com.ssajudn.hushkeep.core.common.UriResolver
import com.ssajudn.hushkeep.core.config.StoragePaths
import com.ssajudn.hushkeep.core.network.SupabaseClientProvider
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.domain.model.UploadStatus
import com.ssajudn.hushkeep.domain.model.SyncState
import io.github.jan.supabase.storage.storage
import java.time.Instant
import java.io.ByteArrayOutputStream

/**
 * Runs one idempotent media upload. The media id is part of the object path,
 * so a retry overwrites the same private object instead of creating a copy.
 */
class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val database = HushkeepDatabase.getInstance(appContext)
    private val uriResolver = UriResolver(appContext.contentResolver)
    private val clock: Clock = Clock.System

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(UploadWorkRequestFactory.JOB_ID_KEY)
            ?: return Result.failure()
        val job = database.uploadJobDao().findById(jobId)
            ?: return Result.failure()
        val now = clock.now()

        database.uploadJobDao().updateStatus(
            jobId = job.id,
            status = UploadStatus.PREPARING.name,
            attemptCount = job.attemptCount,
            lastError = null,
            nextAttemptAtEpochMs = null,
            updatedAtEpochMs = now.toEpochMilli(),
        )
        database.uploadJobDao().updateProgress(job.id, 0L, 0, now.toEpochMilli())
        database.mediaObjectDao().findById(job.mediaObjectId)?.let { media ->
            database.memoryDao().updateSyncState(
                memoryId = media.memoryId,
                ownerId = media.ownerId,
                syncState = SyncState.SYNCING.name,
                updatedAtEpochMs = now.toEpochMilli(),
            )
        }

        val localUri = Uri.parse(job.localUri)
        if (!uriResolver.canRead(localUri)) {
            return handleFailure(
                job.id,
                job.mediaObjectId,
                job.attemptCount,
                now,
                UserFacingMessages.LOCAL_MEDIA_UNAVAILABLE,
            )
        }

        val supabase = SupabaseClientProvider.create()
        if (supabase == null) {
            database.mediaObjectDao().findById(job.mediaObjectId)?.let { media ->
                database.mediaObjectDao().updateSyncState(
                    mediaObjectId = media.id,
                    syncState = SyncState.PENDING.name,
                    storagePath = media.storagePath,
                    updatedAtEpochMs = now.toEpochMilli(),
                )
                database.memoryDao().updateSyncState(
                    memoryId = media.memoryId,
                    ownerId = media.ownerId,
                    syncState = SyncState.PENDING.name,
                    updatedAtEpochMs = now.toEpochMilli(),
                )
            }
            database.uploadJobDao().updateProgress(job.id, job.totalBytes, 100, now.toEpochMilli())
            database.uploadJobDao().updateStatus(
                jobId = job.id,
                status = UploadStatus.COMPLETED.name,
                attemptCount = job.attemptCount,
                lastError = null,
                nextAttemptAtEpochMs = null,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            return Result.success()
        }

        val media = database.mediaObjectDao().findById(job.mediaObjectId)
            ?: return handleFailure(
                job.id,
                job.mediaObjectId,
                job.attemptCount,
                now,
                UserFacingMessages.UPLOAD_FAILED,
            )
        val bytes = readBytesWithProgress(job)
            ?: return handleFailure(
                job.id,
                job.mediaObjectId,
                job.attemptCount,
                now,
                UserFacingMessages.LOCAL_MEDIA_UNAVAILABLE,
            )
        if (bytes.isEmpty()) {
            return handleFailure(
                job.id,
                job.mediaObjectId,
                job.attemptCount,
                now,
                UserFacingMessages.UPLOAD_FAILED,
            )
        }

        database.uploadJobDao().updateStatus(
            jobId = job.id,
            status = UploadStatus.UPLOADING.name,
            attemptCount = job.attemptCount,
            lastError = null,
            nextAttemptAtEpochMs = null,
            updatedAtEpochMs = clock.now().toEpochMilli(),
        )
        setProgress(androidx.work.workDataOf("phase" to UploadStatus.UPLOADING.name))

        return try {
            val extension = MimeTypeResolver.extensionForMimeType(media.mimeType)
            val storagePath = StoragePaths.originalObjectPath(media.ownerId, media.id, extension)
            supabase.storage.from(StoragePaths.PRIVATE_BUCKET).upload(storagePath, bytes) {
                upsert = true
            }
            database.mediaObjectDao().updateSyncState(
                mediaObjectId = media.id,
                syncState = SyncState.SYNCED.name,
                storagePath = storagePath,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            database.memoryDao().updateSyncState(
                memoryId = media.memoryId,
                ownerId = media.ownerId,
                syncState = SyncState.SYNCING.name,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            database.uploadJobDao().updateProgress(job.id, bytes.size.toLong(), 100, now.toEpochMilli())
            setProgress(androidx.work.workDataOf("progress_percent" to 100))
            database.uploadJobDao().updateStatus(
                jobId = job.id,
                status = UploadStatus.COMPLETED.name,
                attemptCount = job.attemptCount,
                lastError = null,
                nextAttemptAtEpochMs = null,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            CloudSyncQueue(androidx.work.WorkManager.getInstance(applicationContext))
                .enqueue(media.ownerId)
            Result.success()
        } catch (error: Throwable) {
            handleFailure(
                job.id,
                job.mediaObjectId,
                job.attemptCount,
                now,
                UserFacingMessages.UPLOAD_FAILED,
            )
        }
    }

    private suspend fun handleFailure(
        jobId: String,
        mediaObjectId: String,
        currentAttemptCount: Int,
        now: Instant,
        message: String,
    ): Result {
        val attemptCount = currentAttemptCount + 1
        val shouldRetry = RetryPolicy.shouldRetry(attemptCount)
        val nextAttemptAt = now.plusSeconds(RetryPolicy.backoffSeconds(attemptCount))

        database.uploadJobDao().updateStatus(
            jobId = jobId,
            status = UploadStatus.FAILED.name,
            attemptCount = attemptCount,
            lastError = message,
            nextAttemptAtEpochMs = nextAttemptAt.toEpochMilli().takeIf { shouldRetry },
            updatedAtEpochMs = now.toEpochMilli(),
        )
        database.mediaObjectDao().findById(mediaObjectId)?.let { media ->
            database.memoryDao().updateSyncState(
                memoryId = media.memoryId,
                ownerId = media.ownerId,
                syncState = SyncState.FAILED.name,
                updatedAtEpochMs = now.toEpochMilli(),
            )
        }

        return if (shouldRetry) Result.retry() else Result.failure()
    }

    private suspend fun readBytesWithProgress(job: com.ssajudn.hushkeep.data.local.entity.UploadJobEntity): ByteArray? {
        val input = uriResolver.openInputStream(Uri.parse(job.localUri)) ?: return null
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            var lastPercent = -1
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                total += count
                val percent = if (job.totalBytes > 0) {
                    ((total * 100L) / job.totalBytes).toInt().coerceIn(0, 99)
                } else 0
                if (percent != lastPercent && percent % 5 == 0) {
                    lastPercent = percent
                    database.uploadJobDao().updateProgress(job.id, total, percent, clock.now().toEpochMilli())
                    setProgress(androidx.work.workDataOf("progress_percent" to percent))
                }
            }
            output.toByteArray()
        }
    }
}

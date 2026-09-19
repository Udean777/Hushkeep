package com.ssajudn.hushkeep.core.sync

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssajudn.hushkeep.core.common.Clock
import com.ssajudn.hushkeep.core.common.MimeTypeResolver
import com.ssajudn.hushkeep.core.common.RetryPolicy
import com.ssajudn.hushkeep.core.common.UriResolver
import com.ssajudn.hushkeep.core.config.StoragePaths
import com.ssajudn.hushkeep.core.network.SupabaseClientProvider
import com.ssajudn.hushkeep.data.local.HushkeepDatabase
import com.ssajudn.hushkeep.domain.model.UploadStatus
import com.ssajudn.hushkeep.domain.model.SyncState
import io.github.jan.supabase.storage.storage
import java.time.Instant

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

        val localUri = Uri.parse(job.localUri)
        if (!uriResolver.canRead(localUri)) {
            return handleFailure(job.id, job.attemptCount, now, "Local media could not be read")
        }

        val supabase = SupabaseClientProvider.create()
        if (supabase == null) {
            database.uploadJobDao().updateStatus(
                jobId = job.id,
                status = UploadStatus.READY_FOR_UPLOAD.name,
                attemptCount = job.attemptCount,
                lastError = null,
                nextAttemptAtEpochMs = null,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            return Result.success()
        }

        val media = database.mediaObjectDao().findById(job.mediaObjectId)
            ?: return handleFailure(job.id, job.attemptCount, now, "Media object tidak ditemukan")
        val bytes = uriResolver.openInputStream(localUri)
            ?.use { it.readBytes() }
            ?: return handleFailure(job.id, job.attemptCount, now, "File lokal tidak dapat dibuka")
        if (bytes.isEmpty()) {
            return handleFailure(job.id, job.attemptCount, now, "File lokal kosong")
        }

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
            database.uploadJobDao().updateStatus(
                jobId = job.id,
                status = UploadStatus.COMPLETED.name,
                attemptCount = job.attemptCount,
                lastError = null,
                nextAttemptAtEpochMs = null,
                updatedAtEpochMs = now.toEpochMilli(),
            )
            Result.success()
        } catch (error: Throwable) {
            handleFailure(job.id, job.attemptCount, now, error.message ?: "Upload gagal")
        }
    }

    private suspend fun handleFailure(
        jobId: String,
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

        return if (shouldRetry) Result.retry() else Result.failure()
    }
}

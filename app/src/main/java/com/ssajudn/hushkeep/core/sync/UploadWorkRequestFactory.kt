package com.ssajudn.hushkeep.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.ssajudn.hushkeep.core.config.SyncPolicy
import java.util.concurrent.TimeUnit

object UploadWorkRequestFactory {
    const val JOB_ID_KEY: String = "upload_job_id"

    fun create(jobId: String): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(JOB_ID_KEY to jobId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                SyncPolicy.INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(SyncPolicy.UPLOAD_WORK_NAME)
            .build()
    }
}

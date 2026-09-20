package com.ssajudn.hushkeep.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.ssajudn.hushkeep.core.config.SyncPolicy
import java.util.concurrent.TimeUnit

object CloudSyncWorkRequestFactory {
    const val OWNER_ID_KEY: String = "cloud_sync_owner_id"

    fun create(ownerId: String): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInputData(workDataOf(OWNER_ID_KEY to ownerId))
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
            .addTag(SyncPolicy.CLOUD_SYNC_WORK_NAME)
            .build()
}

package com.ssajudn.hushkeep.core.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.ssajudn.hushkeep.core.config.SyncPolicy

class CloudSyncQueue(
    private val workManager: WorkManager,
) {
    fun enqueue(ownerId: String) {
        workManager.enqueueUniqueWork(
            "${SyncPolicy.CLOUD_SYNC_WORK_NAME}:$ownerId",
            ExistingWorkPolicy.REPLACE,
            CloudSyncWorkRequestFactory.create(ownerId),
        )
    }
}

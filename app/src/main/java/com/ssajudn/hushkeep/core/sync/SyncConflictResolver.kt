package com.ssajudn.hushkeep.core.sync

import com.ssajudn.hushkeep.domain.model.SyncState

object SyncConflictResolver {
    /**
     * MVP rule: the newer timestamp wins. A pending local change wins a tie so
     * the user's explicit edit is not discarded before its first push.
     */
    fun shouldKeepLocal(
        localUpdatedAtEpochMs: Long,
        remoteUpdatedAtEpochMs: Long,
        localSyncState: String? = null,
    ): Boolean = localUpdatedAtEpochMs > remoteUpdatedAtEpochMs ||
        (localUpdatedAtEpochMs == remoteUpdatedAtEpochMs && isLocalChangePending(localSyncState))

    fun shouldUploadLocal(
        localUpdatedAtEpochMs: Long,
        remoteUpdatedAtEpochMs: Long,
        localSyncState: String? = null,
    ): Boolean = localUpdatedAtEpochMs > remoteUpdatedAtEpochMs ||
        (localUpdatedAtEpochMs == remoteUpdatedAtEpochMs && isLocalChangePending(localSyncState))

    private fun isLocalChangePending(syncState: String?): Boolean = when (SyncState.fromStorage(syncState.orEmpty())) {
        SyncState.PENDING,
        SyncState.SYNCING,
        SyncState.PARTIALLY_SYNCED,
        SyncState.FAILED,
        -> true

        SyncState.SYNCED,
        SyncState.DELETED,
        -> false
    }
}

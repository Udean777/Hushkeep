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
        (localUpdatedAtEpochMs == remoteUpdatedAtEpochMs && localSyncState == SyncState.PENDING.name)

    fun shouldUploadLocal(localUpdatedAtEpochMs: Long, remoteUpdatedAtEpochMs: Long): Boolean =
        localUpdatedAtEpochMs >= remoteUpdatedAtEpochMs
}

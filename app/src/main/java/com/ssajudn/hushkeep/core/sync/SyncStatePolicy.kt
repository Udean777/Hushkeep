package com.ssajudn.hushkeep.core.sync

import com.ssajudn.hushkeep.domain.model.SyncState

object SyncStatePolicy {
    fun afterMediaUpload(): SyncState = SyncState.PARTIALLY_SYNCED

    fun afterMetadataSyncFailure(hasUploadedMedia: Boolean): SyncState =
        if (hasUploadedMedia) SyncState.PARTIALLY_SYNCED else SyncState.FAILED

    fun isRetryable(value: String): Boolean = when (SyncState.fromStorage(value)) {
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

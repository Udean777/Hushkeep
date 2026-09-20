package com.ssajudn.hushkeep.core.sync

import com.ssajudn.hushkeep.domain.model.SyncState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {
    @Test
    fun `newer local data wins refresh conflict`() {
        assertTrue(SyncConflictResolver.shouldKeepLocal(200L, 100L))
        assertFalse(SyncConflictResolver.shouldKeepLocal(100L, 200L))
    }

    @Test
    fun `pending local data wins equal timestamp`() {
        assertTrue(
            SyncConflictResolver.shouldKeepLocal(
                localUpdatedAtEpochMs = 100L,
                remoteUpdatedAtEpochMs = 100L,
                localSyncState = SyncState.PENDING.name,
            ),
        )
        assertFalse(
            SyncConflictResolver.shouldKeepLocal(
                localUpdatedAtEpochMs = 100L,
                remoteUpdatedAtEpochMs = 100L,
                localSyncState = SyncState.SYNCED.name,
            ),
        )
    }

    @Test
    fun `local data is uploaded when it is newer or tied`() {
        assertTrue(SyncConflictResolver.shouldUploadLocal(200L, 100L))
        assertTrue(
            SyncConflictResolver.shouldUploadLocal(
                localUpdatedAtEpochMs = 100L,
                remoteUpdatedAtEpochMs = 100L,
                localSyncState = SyncState.PARTIALLY_SYNCED.name,
            ),
        )
        assertFalse(SyncConflictResolver.shouldUploadLocal(100L, 200L))
    }

    @Test
    fun `synced local data does not get re-uploaded on an equal timestamp`() {
        assertFalse(
            SyncConflictResolver.shouldUploadLocal(
                localUpdatedAtEpochMs = 100L,
                remoteUpdatedAtEpochMs = 100L,
                localSyncState = SyncState.SYNCED.name,
            ),
        )
    }

    @Test
    fun `partial local data wins an equal timestamp during refresh`() {
        assertTrue(
            SyncConflictResolver.shouldKeepLocal(
                localUpdatedAtEpochMs = 100L,
                remoteUpdatedAtEpochMs = 100L,
                localSyncState = SyncState.PARTIALLY_SYNCED.name,
            ),
        )
    }
}

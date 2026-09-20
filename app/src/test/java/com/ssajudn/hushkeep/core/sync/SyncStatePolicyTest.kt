package com.ssajudn.hushkeep.core.sync

import com.ssajudn.hushkeep.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStatePolicyTest {
    @Test
    fun `successful media upload creates partial sync state`() {
        assertEquals(SyncState.PARTIALLY_SYNCED, SyncStatePolicy.afterMediaUpload())
    }

    @Test
    fun `metadata failure stays partial when media exists`() {
        assertEquals(
            SyncState.PARTIALLY_SYNCED,
            SyncStatePolicy.afterMetadataSyncFailure(hasUploadedMedia = true),
        )
    }

    @Test
    fun `metadata failure is failed when no media exists`() {
        assertEquals(
            SyncState.FAILED,
            SyncStatePolicy.afterMetadataSyncFailure(hasUploadedMedia = false),
        )
    }

    @Test
    fun `synced and deleted states are not retryable`() {
        assertFalse(SyncStatePolicy.isRetryable(SyncState.SYNCED.name))
        assertFalse(SyncStatePolicy.isRetryable(SyncState.DELETED.name))
    }

    @Test
    fun `pending partial and failed states are retryable`() {
        assertTrue(SyncStatePolicy.isRetryable(SyncState.PENDING.name))
        assertTrue(SyncStatePolicy.isRetryable(SyncState.PARTIALLY_SYNCED.name))
        assertTrue(SyncStatePolicy.isRetryable(SyncState.FAILED.name))
    }
}

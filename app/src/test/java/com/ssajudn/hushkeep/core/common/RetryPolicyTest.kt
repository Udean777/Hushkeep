package com.ssajudn.hushkeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {
    @Test
    fun `allows attempts below the configured maximum`() {
        assertTrue(RetryPolicy.shouldRetry(0))
        assertTrue(RetryPolicy.shouldRetry(4))
        assertFalse(RetryPolicy.shouldRetry(5))
    }

    @Test
    fun `uses capped exponential backoff`() {
        assertEquals(10L, RetryPolicy.backoffSeconds(0))
        assertEquals(20L, RetryPolicy.backoffSeconds(1))
        assertEquals(40L, RetryPolicy.backoffSeconds(2))
    }
}

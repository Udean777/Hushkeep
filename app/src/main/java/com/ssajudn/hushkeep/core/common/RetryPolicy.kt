package com.ssajudn.hushkeep.core.common

import com.ssajudn.hushkeep.core.config.SyncPolicy
import kotlin.math.min

object RetryPolicy {
    fun shouldRetry(attemptCount: Int): Boolean =
        attemptCount < SyncPolicy.MAX_RETRY_ATTEMPTS

    fun backoffSeconds(attemptCount: Int): Long {
        val exponent = attemptCount.coerceIn(0, 20)
        val multiplier = 1L shl exponent
        return min(
            SyncPolicy.MAX_BACKOFF_SECONDS,
            SyncPolicy.INITIAL_BACKOFF_SECONDS * multiplier,
        )
    }
}

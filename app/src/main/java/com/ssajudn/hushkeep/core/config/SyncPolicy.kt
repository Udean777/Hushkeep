package com.ssajudn.hushkeep.core.config

object SyncPolicy {
    const val MAX_RETRY_ATTEMPTS: Int = 5
    const val INITIAL_BACKOFF_SECONDS: Long = 10L
    const val MAX_BACKOFF_SECONDS: Long = 6L * 60L * 60L
    const val UPLOAD_WORK_NAME: String = "hushkeep-upload-queue"
}

package com.ssajudn.hushkeep.domain.model

data class StorageUsage(
    val localBytes: Long,
    val cloudBytes: Long,
    val pendingBytes: Long,
)

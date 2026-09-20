package com.ssajudn.hushkeep.domain.model

data class MemorySearchFilters(
    val query: String = "",
    val albumId: String? = null,
    val fromEpochMs: Long? = null,
    val toEpochMsExclusive: Long? = null,
)

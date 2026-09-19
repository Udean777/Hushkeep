package com.ssajudn.hushkeep.domain.model

enum class SyncState {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    DELETED;

    companion object {
        fun fromStorage(value: String): SyncState {
            return entries.firstOrNull { it.name == value } ?: PENDING
        }
    }
}

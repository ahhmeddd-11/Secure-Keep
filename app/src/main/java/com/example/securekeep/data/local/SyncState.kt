package com.example.securekeep.data.local

enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    DIRTY,
    SYNCING,
    CONFLICT,
    FAILED
}
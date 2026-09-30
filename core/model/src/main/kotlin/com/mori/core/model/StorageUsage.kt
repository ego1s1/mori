package com.mori.core.model

/** On-disk footprint of the app-private library. */
data class StorageUsage(
    val comicCount: Int,
    val libraryBytes: Long,
    val coversBytes: Long,
    /**
     * Transient materialized read cache (full-comic working copies). Bounded
     * with LRU eviction — never confused with covers, which is why it gets
     * its own line instead of inflating [coversBytes].
     */
    val cacheBytes: Long = 0L,
)

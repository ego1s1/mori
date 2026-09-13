package com.mori.core.model

/** On-disk footprint of the app-private library. */
data class StorageUsage(
    val comicCount: Int,
    val libraryBytes: Long,
    val coversBytes: Long,
)

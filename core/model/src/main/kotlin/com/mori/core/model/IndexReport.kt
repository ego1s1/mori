package com.mori.core.model

/** Result of scanning the app-private library directory into the index. */
data class IndexReport(
    val indexed: Int,
    val failed: Int,
    val removed: Int,
)

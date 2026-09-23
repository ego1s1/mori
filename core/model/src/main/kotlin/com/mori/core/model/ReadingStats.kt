package com.mori.core.model

/**
 * Reading aggregates across all sessions.
 *
 * - [totalSessions]: reader visits recorded.
 * - [totalDurationMs]: wall time inside the reader.
 * - [totalPagesTurned]: settled page turns.
 * - [booksFinished]: books read to the last page (single-page books excluded).
 */
data class ReadingStats(
    val totalSessions: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalPagesTurned: Int = 0,
    val booksFinished: Int = 0,
)

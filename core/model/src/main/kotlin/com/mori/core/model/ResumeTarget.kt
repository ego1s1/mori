package com.mori.core.model

/**
 * A resumable reading position surfaced to the main navigator. Carries only
 * identifiers — artwork and progress stay with the owning feature.
 */
data class ResumeTarget(
    val comicId: String,
    val pageIndex: Int,
    val title: String,
)

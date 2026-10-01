package com.mori.core.model

/** Aggregate progress of an import job shown in onboarding/library. */
data class ImportReport(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
)

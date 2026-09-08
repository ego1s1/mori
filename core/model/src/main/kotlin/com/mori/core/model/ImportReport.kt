package com.mori.core.model

/** Status of a background import job. */
enum class ImportStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

/** A single item inside an import job. */
data class ImportItem(
    val displayName: String,
    val status: ImportStatus,
    val error: String?,
)

/** Aggregate progress of an import job shown in onboarding/library. */
data class ImportReport(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
    val items: List<ImportItem>,
) {
    val isComplete: Boolean get() = succeeded + failed >= total
}

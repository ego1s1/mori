package com.mori.comic.model

/**
 * Metadata describing a comic, populated from a `ComicInfo.xml` file (the ComicRack
 * standard) when present.
 *
 * Any unrecognized elements are preserved in [raw] so consumers can access fields this
 * library does not model explicitly.
 */
data class ComicMetadata(
    val title: String? = null,
    val series: String? = null,
    val number: String? = null,
    val volume: Int? = null,
    val publisher: String? = null,
    val writer: String? = null,
    val year: Int? = null,
    val pageCount: Int? = null,
    val language: String? = null,
    val summary: String? = null,
    val manga: Boolean? = null,
    val raw: Map<String, String> = emptyMap(),
)

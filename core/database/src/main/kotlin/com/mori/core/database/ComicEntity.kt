package com.mori.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Indexed comic row. The source of truth for the library UI; file bytes live in
 * app-private storage, covers as generated thumbnails.
 */
@Entity(tableName = "comics")
data class ComicEntity(
    /** Stable id derived from the archive file name inside the library directory. */
    @PrimaryKey val id: String,
    val title: String,
    val series: String?,
    val number: String?,
    /** [com.mori.core.model.ComicFormat] name. */
    val format: String,
    val pageCount: Int,
    val sourcePath: String,
    val coverPath: String?,
    val lastPageIndex: Int,
    val sourceDisplayName: String,
    val sourceModified: Long,
    /** [com.mori.core.model.ComicError] name, or null when healthy. */
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

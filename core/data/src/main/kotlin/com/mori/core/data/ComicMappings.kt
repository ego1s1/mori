package com.mori.core.data

import com.mori.core.database.ComicEntity
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat

internal fun ComicEntity.toModel(): Comic = Comic(
    id = id,
    title = title,
    series = series,
    number = number,
    format = runCatching { ComicFormat.valueOf(format) }.getOrDefault(ComicFormat.CBZ),
    pageCount = pageCount,
    sourcePath = sourcePath,
    coverPath = coverPath,
    lastPageIndex = lastPageIndex,
    sourceDisplayName = sourceDisplayName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    error = error?.let { runCatching { ComicError.valueOf(it) }.getOrNull() },
)

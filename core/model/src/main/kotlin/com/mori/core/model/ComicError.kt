package com.mori.core.model

/** Why a comic cannot be opened or has no pages. Null means healthy. */
enum class ComicError {
    CORRUPT,
    PASSWORD_REQUIRED,
    EMPTY,
    UNSUPPORTED,
    /** The file or folder vanished (moved, renamed, or access revoked). */
    NOT_FOUND,
}

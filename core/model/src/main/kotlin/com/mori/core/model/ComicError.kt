package com.mori.core.model

/** Why a comic cannot be opened or has no pages. Null means healthy. */
enum class ComicError {
    CORRUPT,
    PASSWORD_REQUIRED,
    EMPTY,
    UNSUPPORTED,
}

/**
 * Single user-facing message per error, shared by every screen so copy never
 * drifts. Names the file state and the next step in one voice.
 */
fun ComicError.userMessage(): String = when (this) {
    ComicError.CORRUPT -> "Can't open this file. It may be damaged — re-import it or remove it."
    ComicError.PASSWORD_REQUIRED -> "This archive needs a password, which Mori can't enter yet."
    ComicError.EMPTY -> "This archive has no readable pages."
    ComicError.UNSUPPORTED -> "This format isn't supported yet."
}

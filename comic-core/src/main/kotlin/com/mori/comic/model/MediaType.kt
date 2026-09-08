package com.mori.comic.model

/**
 * Image container formats recognized by the library.
 *
 * [UNKNOWN] represents any non-image entry and is used to filter page candidates.
 */
enum class MediaType {
    JPEG,
    PNG,
    WEBP,
    GIF,
    BMP,
    UNKNOWN,
    ;

    companion object {
        /** Resolves a [MediaType] from an entry or file name, or [UNKNOWN] if it is not a known image. */
        fun fromName(name: String): MediaType {
            val extension = name.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "jpg", "jpeg", "jpe", "jfif" -> JPEG
                "png" -> PNG
                "webp" -> WEBP
                "gif" -> GIF
                "bmp" -> BMP
                else -> UNKNOWN
            }
        }

        /** Whether [name] looks like an image entry based on its extension. */
        fun isImageName(name: String): Boolean = fromName(name) != UNKNOWN
    }
}

package com.mori.comic

/**
 * Base type for all errors thrown by the comic library.
 *
 * Consumers can catch [ComicException] to handle any library-specific failure, or a
 * concrete subclass for granular handling.
 */
sealed class ComicException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The source is not a supported comic archive or image container. */
class UnsupportedFormatException(message: String) : ComicException(message)

/** The archive exists but cannot be read as its detected format (truncated, wrong magic, etc.). */
class CorruptArchiveException(message: String, cause: Throwable? = null) : ComicException(message, cause)

/** The archive contains no readable pages. */
class EmptyArchiveException(message: String) : ComicException(message)

/** The archive is encrypted and requires a password to open. */
class PasswordRequiredException(message: String, cause: Throwable? = null) : ComicException(message, cause)

/** The requested page does not exist in the archive. */
class PageNotFoundException(message: String) : ComicException(message)

/** The archive has already been closed. */
class ArchiveClosedException(message: String) : ComicException(message)

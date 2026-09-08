package com.mori.comic

import java.io.File
import java.io.IOException

/** Container formats the library can open. */
enum class ArchiveFormat {
    CBZ,
    CBR,
    ;

    companion object {
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\x03\x04"
        private val ZIP_EMPTY_MAGIC = byteArrayOf(0x50, 0x4B, 0x05, 0x06) // "PK\x05\x06"
        private val ZIP_SPANNED_MAGIC = byteArrayOf(0x50, 0x4B, 0x07, 0x08) // "PK\x07\x08"
        private val RAR_MAGIC = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07) // "Rar!\x1A\x07"

        /**
         * Determines the archive format of [file] from its extension and, as a fallback,
         * its magic bytes.
         *
         * @throws UnsupportedFormatException when neither the extension nor the contents
         *   match a supported format.
         */
        fun detect(file: File): ArchiveFormat {
            val extension = file.extension.lowercase()
            val byExtension = when (extension) {
                "cbz", "zip" -> CBZ
                "cbr", "rar" -> CBR
                else -> null
            }
            if (byExtension != null) return byExtension

            val magic = readMagic(file)
            return when {
                startsWith(magic, ZIP_MAGIC) ||
                    startsWith(magic, ZIP_EMPTY_MAGIC) ||
                    startsWith(magic, ZIP_SPANNED_MAGIC) -> CBZ
                startsWith(magic, RAR_MAGIC) -> CBR
                else -> throw UnsupportedFormatException(
                    "Unsupported comic format: '${file.name}'",
                )
            }
        }

        private fun readMagic(file: File): ByteArray {
            try {
                file.inputStream().use { input ->
                    val buffer = ByteArray(MAX_MAGIC_BYTES)
                    val read = input.read(buffer)
                    return buffer.copyOf(read.coerceAtLeast(0))
                }
            } catch (e: IOException) {
                throw CorruptArchiveException("Unable to read '${file.name}'", e)
            }
        }

        private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
            if (bytes.size < prefix.size) return false
            return prefix.indices.all { bytes[it] == prefix[it] }
        }

        private const val MAX_MAGIC_BYTES = 8
    }
}

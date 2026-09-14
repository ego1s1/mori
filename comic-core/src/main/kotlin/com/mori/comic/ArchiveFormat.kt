package com.mori.comic

import java.io.File
import java.io.IOException

/** Container formats the library can open. */
enum class ArchiveFormat {
    CBZ,
    CBR,
    CB7,
    CBT,
    ;

    companion object {
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\x03\x04"
        private val ZIP_EMPTY_MAGIC = byteArrayOf(0x50, 0x4B, 0x05, 0x06) // "PK\x05\x06"
        private val ZIP_SPANNED_MAGIC = byteArrayOf(0x50, 0x4B, 0x07, 0x08) // "PK\x07\x08"
        private val RAR_MAGIC = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07) // "Rar!\x1A\x07"
        // "7z\xBC\xAF\x27\x1C"
        private val SEVEN_ZIP_MAGIC =
            byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)
        private const val USTAR_OFFSET = 257
        private const val USTAR_MAGIC = "ustar"

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
                "cb7", "7z" -> CB7
                "cbt", "tar" -> CBT
                else -> null
            }
            if (byExtension != null) return byExtension

            val magic = readMagic(file)
            return when {
                startsWith(magic, ZIP_MAGIC) ||
                    startsWith(magic, ZIP_EMPTY_MAGIC) ||
                    startsWith(magic, ZIP_SPANNED_MAGIC) -> CBZ
                startsWith(magic, RAR_MAGIC) -> CBR
                startsWith(magic, SEVEN_ZIP_MAGIC) -> CB7
                isUstar(magic) -> CBT
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

        /** POSIX ustar magic sits at offset 257, past the name prefix. */
        internal fun looksLikeTar(header: ByteArray): Boolean {
            if (header.size < USTAR_OFFSET + USTAR_MAGIC.length) return false
            return USTAR_MAGIC.indices.all { header[USTAR_OFFSET + it] == USTAR_MAGIC[it].code.toByte() }
        }

        private fun isUstar(bytes: ByteArray): Boolean = looksLikeTar(bytes)

        private const val MAX_MAGIC_BYTES = 264
    }
}

package com.mori.comic.util

import com.mori.comic.CorruptArchiveException
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Decompression budgets: archive entries are attacker-controlled byte
 * streams, so no page is ever inflated into an unbounded [ByteArray].
 * The pixel guard in `PageDecoder` runs after decompression and cannot stop
 * a zip-bomb page on its own — this cap is the first line.
 *
 * 128 MiB per page is far above real content (even a 50MP photo ships as a
 * few megabytes of JPEG) while keeping a crafted entry from OOMing the
 * reader before the pixel budget is consulted.
 */
const val MAX_PAGE_BYTES = 128L * 1024 * 1024

/**
 * Reads this stream up to [capBytes], throwing [CorruptArchiveException]
 * naming [what] when the entry runs past the budget. Callers let it
 * propagate: it already maps to the corrupt error row downstream.
 */
fun InputStream.readCapped(capBytes: Long = MAX_PAGE_BYTES, what: String): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > capBytes) {
            throw CorruptArchiveException("$what exceeds per-page size budget")
        }
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

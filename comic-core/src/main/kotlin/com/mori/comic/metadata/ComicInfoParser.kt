package com.mori.comic.metadata

import com.mori.comic.model.ComicMetadata
import org.w3c.dom.Node
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses a ComicRack-style `ComicInfo.xml` document into [ComicMetadata].
 *
 * The parser is defensive: malformed or unsupported documents never throw. Unrecognized
 * elements are preserved in [ComicMetadata.raw].
 */
object ComicInfoParser {

    /**
     * Parses [input] as `ComicInfo.xml`. The stream is consumed but not
     * closed. At most [MAX_XML_BYTES] are read first: real ComicInfo files
     * are kilobytes, and the parser hardening below is best-effort on some
     * Android runtimes — the byte cap holds even where entity budgets are
     * unsupported, so a hostile entry cannot buffer unbounded XML.
     */
    fun parse(input: InputStream): ComicMetadata {
        val bytes = input.readUpTo(MAX_XML_BYTES) ?: return ComicMetadata()
        val document = runCatching { newDocumentBuilder().parse(bytes.inputStream()) }.getOrNull()
        val root = document?.documentElement ?: return ComicMetadata()

        val raw = collectElements(root)
        return if (raw.isEmpty()) ComicMetadata() else fromRaw(raw)
    }

    private fun collectElements(root: Node): LinkedHashMap<String, String> {
        val raw = linkedMapOf<String, String>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val value = node.textContent?.trim().orEmpty()
                if (value.isNotEmpty()) {
                    raw[node.nodeName] = value
                }
            }
        }
        return raw
    }

    private fun fromRaw(raw: Map<String, String>): ComicMetadata = ComicMetadata(
            title = raw["Title"],
            series = raw["Series"],
            number = raw["Number"],
            volume = raw["Volume"]?.toIntOrNull(),
            publisher = raw["Publisher"],
            writer = raw["Writer"],
            year = raw["Year"]?.toIntOrNull(),
            pageCount = raw["PageCount"]?.toIntOrNull(),
            language = raw["LanguageISO"] ?: raw["Language"],
            summary = raw["Summary"],
            manga = raw["Manga"]?.parseBoolean(),
            raw = raw,
        )

    private fun newDocumentBuilder(): javax.xml.parsers.DocumentBuilder =
        DocumentBuilderFactory.newInstance()
        .apply {
            // Best-effort hardening against XXE. Unsupported features are ignored so the
            // same code path works on both the JVM and Android's bundled parser.
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            // Second layer for parsers that ignore the doctype ban above:
            // secure processing plus tight expansion budgets. ComicInfo files
            // are kilobytes; anything bigger is hostile.
            runCatching {
                setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true)
            }
            runCatching {
                setAttribute("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", 1_000)
            }
            runCatching {
                setAttribute("http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit", 100_000)
            }
        }
        .newDocumentBuilder()

    private fun String.parseBoolean(): Boolean? = when (lowercase()) {
        "yes", "true", "1", "y" -> true
        "no", "false", "0", "n" -> false
        else -> null
    }

    /** Real ComicInfo files are kilobytes; anything bigger is hostile. */
    const val MAX_XML_BYTES = 512 * 1024

    /**
     * Reads up to [cap] bytes, or null when the stream is longer (the
     * caller then refuses the entry instead of buffering it).
     */
    private fun InputStream.readUpTo(cap: Int): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > cap) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}

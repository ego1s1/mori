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

    /** Parses [input] as `ComicInfo.xml`. The stream is consumed but not closed. */
    fun parse(input: InputStream): ComicMetadata {
        val document = runCatching { newDocumentBuilder().parse(input) }.getOrNull()
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
        }
        .newDocumentBuilder()

    private fun String.parseBoolean(): Boolean? = when (lowercase()) {
        "yes", "true", "1", "y" -> true
        "no", "false", "0", "n" -> false
        else -> null
    }
}

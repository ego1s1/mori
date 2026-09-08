package com.mori.comic.metadata

import com.mori.comic.model.ComicMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ComicInfoParserTest {

    private fun parse(xml: String): ComicMetadata =
        ComicInfoParser.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    @Test
    fun parsesFullDocument() {
        val xml = """
            <ComicInfo>
              <Title>My Comic</Title>
              <Series>Series A</Series>
              <Number>7</Number>
              <Volume>3</Volume>
              <Publisher>Pub</Publisher>
              <Writer>W</Writer>
              <Year>2020</Year>
              <PageCount>24</PageCount>
              <LanguageISO>en</LanguageISO>
              <Summary>Summary text</Summary>
              <Manga>Yes</Manga>
            </ComicInfo>
        """.trimIndent()
        val meta = parse(xml)
        assertEquals("My Comic", meta.title)
        assertEquals("Series A", meta.series)
        assertEquals("7", meta.number)
        assertEquals(3, meta.volume)
        assertEquals("Pub", meta.publisher)
        assertEquals("W", meta.writer)
        assertEquals(2020, meta.year)
        assertEquals(24, meta.pageCount)
        assertEquals("en", meta.language)
        assertEquals("Summary text", meta.summary)
        assertEquals(true, meta.manga)
    }

    @Test
    fun emptyDocumentYieldsEmptyMetadata() {
        assertTrue(parse("<ComicInfo></ComicInfo>").isEmpty)
        assertTrue(parse("<ComicInfo/>").isEmpty)
    }

    @Test
    fun malformedXmlFallsBackToEmpty() {
        assertTrue(parse("this is not xml at all").isEmpty)
        assertTrue(parse("<ComicInfo><Title>unclosed</ComicInfo>").isEmpty)
    }

    @Test
    fun invalidNumericValuesAreNull() {
        val meta = parse("<ComicInfo><Volume>abc</Volume><Year>12x</Year><PageCount></PageCount></ComicInfo>")
        assertNull(meta.volume)
        assertNull(meta.year)
        assertNull(meta.pageCount)
    }

    @Test
    fun mangaBooleanVariants() {
        assertEquals(true, parse("<ComicInfo><Manga>Yes</Manga></ComicInfo>").manga)
        assertEquals(true, parse("<ComicInfo><Manga>true</Manga></ComicInfo>").manga)
        assertEquals(false, parse("<ComicInfo><Manga>No</Manga></ComicInfo>").manga)
        assertEquals(false, parse("<ComicInfo><Manga>0</Manga></ComicInfo>").manga)
        assertNull(parse("<ComicInfo><Manga>maybe</Manga></ComicInfo>").manga)
    }

    @Test
    fun languageFallsBackToLegacyField() {
        assertEquals("jp", parse("<ComicInfo><Language>jp</Language></ComicInfo>").language)
        assertEquals("en", parse("<ComicInfo><LanguageISO>en</LanguageISO></ComicInfo>").language)
        assertEquals(
            "iso",
            parse("<ComicInfo><Language>legacy</Language><LanguageISO>iso</LanguageISO></ComicInfo>").language,
        )
    }

    @Test
    fun unknownElementsAreCapturedRaw() {
        val meta = parse("<ComicInfo><Custom>value</Custom><Title>T</Title></ComicInfo>")
        assertEquals("value", meta.raw["Custom"])
        assertEquals("T", meta.title)
        assertFalse(meta.isEmpty)
    }

    @Test
    fun entityExpansionIsNotExploded() {
        // If XXE were possible this would attempt a file read; it must fall back gracefully.
        val xml = """
            <!DOCTYPE ComicInfo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <ComicInfo><Title>&xxe;</Title></ComicInfo>
        """.trimIndent()
        val meta = parse(xml)
        assertTrue(meta.title == null || !meta.title!!.contains("root:"))
    }

    @Test
    fun whitespaceOnlyValuesAreIgnored() {
        val meta = parse("<ComicInfo><Title>   </Title><Series>S</Series></ComicInfo>")
        assertNull(meta.title)
        assertEquals("S", meta.series)
    }
}

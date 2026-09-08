package com.mori.comic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalSortTest {

    private fun assertOrdered(vararg expected: String) {
        val input = expected.toList()
        val actual = input.sortedWith(NaturalSort.comparator)
        assertEquals("Expected natural order", input, actual)
    }

    private fun assertSign(expected: Int, a: String, b: String) {
        val actual = NaturalSort.compare(a, b).coerceIn(-1, 1)
        assertEquals(
            "compare(\"$a\", \"$b\") should be ${expected.coerceIn(-1, 1)} but was $actual",
            expected.coerceIn(-1, 1),
            actual,
        )
    }

    @Test
    fun numericOrderingIsNotLexicographic() {
        assertOrdered("1.jpg", "2.jpg", "10.jpg")
        assertOrdered("1", "2", "10", "20", "100")
    }

    @Test
    fun leadingZerosCompareEqualNumerically() {
        assertSign(0, "001.jpg", "01.jpg")
        assertSign(0, "001.jpg", "1.jpg")
        assertSign(0, "1.jpg", "01.jpg")
    }

    @Test
    fun longerNumbersAreLarger() {
        assertSign(-1, "2.jpg", "10.jpg")
        assertSign(1, "100.jpg", "99.jpg")
    }

    @Test
    fun mixedAlphaNumeric() {
        assertOrdered("page1.jpg", "page2.jpg", "page10.jpg")
        assertOrdered("ch1", "ch2", "ch10", "ch20")
    }

    @Test
    fun digitsSortBeforeLetters() {
        assertSign(-1, "001.jpg", "cover.jpg")
    }

    @Test
    fun comparisonIsCaseInsensitive() {
        assertSign(0, "Page1.jpg", "page1.jpg")
        assertSign(0, "PAGE.JPG", "page.jpg")
    }

    @Test
    fun emptyStringsSortFirst() {
        assertSign(0, "", "")
        assertSign(-1, "", "1")
        assertSign(1, "1", "")
        assertOrdered("", "1", "a")
    }

    @Test
    fun dotsAndDecimalsAreHandled() {
        assertOrdered("1.5.jpg", "1.10.jpg")
    }

    @Test
    fun minusSignIsTreatedAsACharacter() {
        assertSign(-1, "-2", "-10")
    }

    @Test
    fun largeNumbersDoNotOverflow() {
        assertSign(1, "18446744073709551616", "18446744073709551615")
        assertSign(-1, "99999999999999999999", "100000000000000000000")
    }

    @Test
    fun trailingSuffixAfterSharedPrefix() {
        assertSign(-1, "file2", "file2a")
        assertOrdered("file2", "file2a", "file10")
    }

    @Test
    fun zeroRunsCompareEqual() {
        assertSign(0, "000", "0")
        assertSign(0, "page000", "page0")
    }

    @Test
    fun fullMixedListSortsStably() {
        val input = listOf("b10", "a", "2", "cover", "10", "b1", "3", "b2", "1")
        val expected = listOf("1", "2", "3", "10", "a", "b1", "b2", "b10", "cover")
        assertEquals(expected, input.sortedWith(NaturalSort.comparator))
    }

    @Test
    fun singleCharacterAndWhitespace() {
        assertSign(-1, "a", "b")
        assertSign(-1, " ", "a")
    }
}

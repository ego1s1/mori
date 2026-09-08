package com.mori.comic.util

/**
 * Natural (alphanumeric) ordering for file names and page labels.
 *
 * Unlike plain lexicographic sorting, natural ordering compares runs of digits by
 * their numeric value so that `"page2"` sorts before `"page10"` and `"001"` compares
 * equal to `"01"` and `"1"`.
 *
 * Comparison rules:
 *  - Strings are compared chunk by chunk, alternating between digit and non-digit runs.
 *  - Two digit runs are compared numerically: leading zeros are ignored and a longer
 *    run is larger. This is overflow-safe for arbitrarily long numbers.
 *  - Two non-digit runs are compared case-insensitively, character by character.
 *  - If the strings are equal up to the length of the shorter one, the shorter one
 *    sorts first.
 *
 * This comparator never throws and is suitable as a stable sort key for page ordering.
 */
object NaturalSort {
    /** Comparator that orders [String]s naturally. */
    val comparator: Comparator<String> = Comparator { a, b -> compare(a, b) }

    fun compare(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        while (ia < a.length && ib < b.length) {
            val ca = a[ia]
            val cb = b[ib]
            val result = if (ca.isDigit() && cb.isDigit()) {
                val aRun = readNumberRun(a, ia)
                val bRun = readNumberRun(b, ib)
                ia += aRun.length
                ib += bRun.length
                compareNumberRuns(aRun, bRun)
            } else {
                ia += 1
                ib += 1
                ca.lowercaseChar().compareTo(cb.lowercaseChar())
            }
            if (result != 0) return result
        }
        return when {
            ia == a.length && ib == b.length -> 0
            ia == a.length -> -1
            else -> 1
        }
    }

    private fun readNumberRun(value: String, start: Int): String {
        var end = start
        while (end < value.length && value[end].isDigit()) end += 1
        return value.substring(start, end)
    }

    private fun compareNumberRuns(a: String, b: String): Int {
        val strippedA = a.trimStart('0')
        val strippedB = b.trimStart('0')
        return when {
            strippedA.length != strippedB.length -> strippedA.length.compareTo(strippedB.length)
            strippedA < strippedB -> -1
            strippedA > strippedB -> 1
            else -> 0
        }
    }
}

package org.futo.inputmethod.latin.uix.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TextSelectionBoundsTest {
    private fun selectedWord(text: String, cursor: Int): String? =
        findWordSelectionBounds(text, cursor, Locale.ENGLISH)?.let {
            text.substring(it.start, it.end)
        }

    private fun selectedLine(text: String, cursor: Int): String? =
        findLineSelectionBounds(text, cursor)?.let {
            text.substring(it.start, it.end)
        }

    @Test
    fun selectWordAtStartMiddleAndEnd() {
        assertEquals("hello", selectedWord("hello world", 0))
        assertEquals("hello", selectedWord("hello world", 2))
        assertEquals("hello", selectedWord("hello world", 5))
        assertEquals("world", selectedWord("hello world", 6))
        assertEquals("world", selectedWord("hello world", 11))
    }

    @Test
    fun selectWordIgnoresWhitespaceAndPunctuation() {
        assertNull(selectedWord("hello  world", 6))
        assertNull(selectedWord("hello, world", 6))
        assertEquals("hello", selectedWord("hello, world", 5))
    }

    @Test
    fun selectWordHandlesUnicodeAndInvalidCursor() {
        assertEquals("don't", selectedWord("don't stop", 3))
        assertEquals("café", selectedWord("café", 4))
        assertEquals("hello", selectedWord("🙂hello", 2))
        assertNull(selectedWord("hello", -1))
        assertNull(selectedWord("hello", 6))
    }

    @Test
    fun selectLineWithoutIncludingNewline() {
        val text = "first\nsecond\nthird"
        assertEquals("first", selectedLine(text, 0))
        assertEquals("first", selectedLine(text, 5))
        assertEquals("second", selectedLine(text, 6))
        assertEquals("second", selectedLine(text, 12))
        assertEquals("third", selectedLine(text, text.length))
    }

    @Test
    fun selectLineHandlesCrLfAndEmptyLines() {
        assertEquals("second", selectedLine("first\r\nsecond", 7))
        assertEquals("", selectedLine("first\n\nthird", 6))
        assertEquals("", selectedLine("", 0))
        assertNull(selectedLine("text", -1))
        assertNull(selectedLine("text", 5))
    }
}

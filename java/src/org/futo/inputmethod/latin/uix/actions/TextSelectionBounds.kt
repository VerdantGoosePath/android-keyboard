package org.futo.inputmethod.latin.uix.actions

import java.text.BreakIterator
import java.util.Locale

/** A selection in UTF-16 offsets, with an exclusive end. */
internal data class TextSelectionBounds(val start: Int, val end: Int)

private fun isWordCodePoint(codePoint: Int): Boolean =
    Character.isLetterOrDigit(codePoint) || when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        Character.CONNECTOR_PUNCTUATION.toInt() -> true
        else -> false
    }

/** Select the word containing the caret, or the preceding word when the caret is at its end. */
internal fun findWordSelectionBounds(text: String, cursor: Int, locale: Locale): TextSelectionBounds? {
    if (cursor !in 0..text.length || text.isEmpty()) return null

    val index = when {
        cursor < text.length && isWordCodePoint(Character.codePointAt(text, cursor)) -> cursor
        cursor > 0 && isWordCodePoint(Character.codePointBefore(text, cursor)) ->
            cursor - Character.charCount(Character.codePointBefore(text, cursor))
        else -> return null
    }

    val iterator = BreakIterator.getWordInstance(locale)
    iterator.setText(text)
    val end = iterator.following(index)
    if (end == BreakIterator.DONE) return null
    val start = iterator.preceding(end)
    if (start == BreakIterator.DONE || start >= end) return null
    return TextSelectionBounds(start, end)
}

/** Select the logical line (excluding CR/LF), not a soft-wrapped visual line. */
internal fun findLineSelectionBounds(text: String, cursor: Int): TextSelectionBounds? {
    if (cursor !in 0..text.length) return null

    var start = cursor
    while (start > 0 && text[start - 1] != '\n' && text[start - 1] != '\r') start--
    var end = cursor
    while (end < text.length && text[end] != '\n' && text[end] != '\r') end++
    return TextSelectionBounds(start, end)
}

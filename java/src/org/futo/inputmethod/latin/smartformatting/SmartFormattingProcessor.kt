package org.futo.inputmethod.latin.smartformatting

import android.content.Context
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.RichInputConnection
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.getSetting

/**
 * Runs after the ordinary IME has processed a key, preserving editor actions and composing text.
 */
object SmartFormattingProcessor {
    private fun keypress(event: Event): Boolean =
        event.eventType == Event.EVENT_TYPE_INPUT_KEYPRESS ||
            event.eventType == Event.EVENT_TYPE_INPUT_KEYPRESS_RESUMED

    private fun pairFor(code: Int, context: Context): String? = with(SmartFormattingSettings) {
        when (code) {
            '('.code -> if (context.getSetting(closeRound)) ")" else null
            '['.code -> if (context.getSetting(closeSquare)) "]" else null
            '{'.code -> if (context.getSetting(closeCurly)) "}" else null
            '<'.code -> if (context.getSetting(closeAngle)) ">" else null
            '\''.code -> if (context.getSetting(closeSingleQuote)) "'" else null
            '"'.code -> if (context.getSetting(closeDoubleQuote)) "\"" else null
            '*'.code -> if (context.getSetting(closeAsterisk)) "*" else null
            '\u0060'.code -> if (context.getSetting(closeBacktick)) "\u0060" else null
            else -> null
        }
    }

    private fun canSkip(code: Int, context: Context): Boolean = with(SmartFormattingSettings) {
        when (code) {
            ')'.code -> context.getSetting(closeRound)
            ']'.code -> context.getSetting(closeSquare)
            '}'.code -> context.getSetting(closeCurly)
            '>'.code -> context.getSetting(closeAngle)
            '\''.code -> context.getSetting(closeSingleQuote)
            '"'.code -> context.getSetting(closeDoubleQuote)
            '*'.code -> context.getSetting(closeAsterisk)
            '\u0060'.code -> context.getSetting(closeBacktick)
            else -> false
        }
    }

    /**
     * Handle indentation shortcuts before InputLogic can turn two spaces into a period.
     * Only run at the end of a logical line with a collapsed selection.
     */
    private fun shortcutBefore(event: Event, connection: RichInputConnection, context: Context): Boolean {
        val code = event.mCodePoint
        if (code != ' '.code && code != '-'.code) return false
        val cursor = connection.mExpectedSelStart
        if (cursor < 0 || cursor != connection.mExpectedSelEnd) return false
        val after = connection.getTextAfterCursor(1, 0)?.firstOrNull()
        if (after != null && after != '\n' && after != '\r') return false
        val before = connection.getTextBeforeCursor(4096, 0)?.toString() ?: return false
        val line = before.substringAfterLast('\n').substringAfterLast('\r')
        val rewrite = listShortcutRewrite(
            line + code.toChar(), context.getSetting(SmartFormattingSettings.spacesPerTab),
            context.getSetting(SmartFormattingSettings.indentShortcut),
            context.getSetting(SmartFormattingSettings.deindentShortcut)
        ) ?: return false
        val start = cursor - line.length
        if (!connection.setSelection(start, cursor)) return false
        connection.commitText(rewrite.text, 1)
        connection.send()
        connection.setSelection(start + rewrite.text.length, start + rewrite.text.length)
        return true
    }

    /** Skip an existing matching closing delimiter instead of typing a duplicate. */
    fun before(event: Event, connection: RichInputConnection, context: Context): Boolean {
        if (!SmartFormattingSession.enabled || !keypress(event)) return false
        if (shortcutBefore(event, connection, context)) return true
        if (!canSkip(event.mCodePoint, context)) return false
        val cursor = connection.mExpectedSelStart
        if (cursor < 0 || cursor != connection.mExpectedSelEnd) return false
        val next = connection.getTextAfterCursor(1, 0)?.firstOrNull() ?: return false
        if (next.code != event.mCodePoint || !connection.setSelection(cursor + 1, cursor + 1)) return false

        if (next == '>' || next == ']') appendTagIfNeeded(connection, context)
        return true
    }

    fun after(event: Event, beforeCursor: Int, connection: RichInputConnection, context: Context) {
        if (!SmartFormattingSession.enabled || !keypress(event)) return
        val cursor = connection.mExpectedSelStart
        if (cursor < 0 || cursor != connection.mExpectedSelEnd || cursor <= beforeCursor) return
        val code = event.mCodePoint
        // Read editor context only when the key can trigger a Smart Formatting rule.
        when (code) {
            Constants.CODE_ENTER, ' '.code, '-'.code, '>'.code, ']'.code,
            '('.code, '['.code, '{'.code, '<'.code, '\''.code,
            '"'.code, '*'.code, '\u0060'.code -> Unit
            else -> return
        }
        val beforeText = connection.getTextBeforeCursor(4096, 0)?.toString() ?: return
        if (beforeText.isEmpty()) return

        if (code == Constants.CODE_ENTER && beforeText.endsWith("\n")) {
            val previous = beforeText.dropLast(1).substringAfterLast('\n').substringAfterLast('\r')
            val emptyMarkerLength = emptyListItemPrefixLength(previous)
            if (emptyMarkerLength != null &&
                context.getSetting(SmartFormattingSettings.clearEmptyListItem)) {
                // The line must be empty on both sides of the original caret. If Enter
                // split a populated line (e.g. "- |text"), preserve the list marker.
                val after = connection.getTextAfterCursor(1, 0)
                if (after != null && (after.isEmpty() || after[0] == '\n' || after[0] == '\r')) {
                    // Delete [indent + marker + space + newline], then insert one newline.
                    // The original list line becomes blank, with the caret on the next
                    // blank line. Use deleteSurroundingText via RichInputConnection:
                    // commitText("") is dropped by the keyboard's buffering wrapper.
                    connection.deleteTextBeforeCursor(emptyMarkerLength + 1)
                    connection.commitText("\n", 1)
                    return
                }
            }
            if (!context.getSetting(SmartFormattingSettings.listContinuation)) return
            val prefix = listContinuation(previous) ?: return
            if (prefix.isNotEmpty()) connection.commitText(prefix, 1)
            return
        }

        if (code == ' '.code || code == '-'.code) {
            val line = beforeText.substringAfterLast('\n').substringAfterLast('\r')
            val afterChar = connection.getTextAfterCursor(1, 0)?.firstOrNull()
            if (afterChar == null || afterChar == '\n' || afterChar == '\r') {
                val rewrite = listShortcutRewrite(
                    line, context.getSetting(SmartFormattingSettings.spacesPerTab),
                    context.getSetting(SmartFormattingSettings.indentShortcut),
                    context.getSetting(SmartFormattingSettings.deindentShortcut)
                )
                if (rewrite != null) {
                    val start = cursor - line.length
                    if (connection.setSelection(start, cursor)) {
                        connection.commitText(rewrite.text, 1)
                        connection.send()
                        connection.setSelection(start + rewrite.text.length, start + rewrite.text.length)
                    }
                    return
                }
            }
        }

        if (code == '>'.code || code == ']'.code) appendTagIfNeeded(connection, context)

        val closing = pairFor(code, context) ?: return
        if (beforeText.last().code != code) return
        if ((code == '\''.code || code == '"'.code) &&
            beforeText.length > 1 && beforeText[beforeText.lastIndex - 1].isLetterOrDigit()) return
        if (connection.getTextAfterCursor(1, 0)?.toString()?.startsWith(closing) == true) return
        connection.commitText(closing, 1)
        connection.send()
        connection.setSelection(cursor, cursor)
    }

    private fun appendTagIfNeeded(connection: RichInputConnection, context: Context) {
        val html = context.getSetting(SmartFormattingSettings.closeHtmlTag)
        val bbcode = context.getSetting(SmartFormattingSettings.closeBbcodeTag)
        if (!html && !bbcode) return
        val before = connection.getTextBeforeCursor(2048, 0)?.toString() ?: return
        val after = connection.getTextAfterCursor(2048, 0)?.toString() ?: return
        val tag = closingTag(before, after, html, bbcode) ?: return
        val position = connection.mExpectedSelStart
        connection.commitText(tag, 1)
        connection.send()
        connection.setSelection(position, position)
    }
}

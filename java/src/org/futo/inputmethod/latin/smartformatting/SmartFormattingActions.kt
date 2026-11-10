package org.futo.inputmethod.latin.smartformatting

import android.os.Build
import android.widget.Toast
import android.view.inputmethod.ExtractedTextRequest
import org.futo.inputmethod.latin.LatinIME
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.getSetting

private data class EditorSnapshot(val text: String, val offset: Int, val start: Int, val end: Int)

private fun snapshot(ime: LatinIME): EditorSnapshot? {
    val connection = ime.currentInputConnection ?: return null
    val extracted = runCatching { connection.getExtractedText(ExtractedTextRequest(), 0) }.getOrNull()
    if (extracted?.text != null && extracted.startOffset >= 0 &&
        extracted.selectionStart in 0..extracted.text.length &&
        extracted.selectionEnd in 0..extracted.text.length) {
        return EditorSnapshot(extracted.text.toString(), extracted.startOffset,
            extracted.selectionStart, extracted.selectionEnd)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val surrounding = runCatching { connection.getSurroundingText(8192, 8192, 0) }.getOrNull()
        if (surrounding != null && surrounding.offset >= 0 &&
            surrounding.selectionStart in 0..surrounding.text.length &&
            surrounding.selectionEnd in 0..surrounding.text.length) {
            return EditorSnapshot(surrounding.text.toString(), surrounding.offset,
                surrounding.selectionStart, surrounding.selectionEnd)
        }
    }
    return null
}

/** Manual commands affect only the current logical line. Multiline selections are deferred. */
private fun changeCurrentLineIndent(ime: LatinIME, increase: Boolean) {
    val connection = ime.currentInputConnection ?: return
    val snap = snapshot(ime) ?: return
    val first = minOf(snap.start, snap.end)
    val last = maxOf(snap.start, snap.end)
    if (snap.text.substring(first, last).any { it == '\n' || it == '\r' }) return
    val preceding = snap.text.substring(0, first)
    val lineStart = maxOf(preceding.lastIndexOf('\n'), preceding.lastIndexOf('\r')) + 1
    val spaces = ime.getSetting(SmartFormattingSettings.spacesPerTab).coerceIn(1, 16)
    val amount = if (increase) spaces else
        snap.text.substring(lineStart).takeWhile { it == ' ' }.length.coerceAtMost(spaces)
    if (amount == 0) return
    val absolute = snap.offset + lineStart

    connection.beginBatchEdit()
    try {
        connection.finishComposingText()
        if (increase) {
            if (!connection.setSelection(absolute, absolute)) return
            connection.commitText(" ".repeat(amount), 1)
            connection.setSelection(snap.offset + snap.start + amount, snap.offset + snap.end + amount)
        } else {
            if (!connection.setSelection(absolute, absolute + amount)) return
            connection.commitText("", 1)
            fun adjusted(index: Int) = snap.offset + maxOf(lineStart, index - amount)
            connection.setSelection(adjusted(snap.start), adjusted(snap.end))
        }
    } finally {
        connection.endBatchEdit()
    }
}

val SmartFormattingAction = Action(
    icon = R.drawable.type,
    name = R.string.smart_formatting_action_title,
    simplePressImpl = { manager, _ ->
        (manager.getContext() as? LatinIME)?.let {
            SmartFormattingSession.toggle(it)
            Toast.makeText(
                it,
                if (SmartFormattingSession.enabled) R.string.smart_formatting_now_enabled
                else R.string.smart_formatting_now_disabled,
                Toast.LENGTH_SHORT
            ).show()
        }
    },
    windowImpl = null,
)

val IndentAction = Action(
    icon = R.drawable.arrow_right,
    name = R.string.smart_formatting_indent_action,
    simplePressImpl = { manager, _ ->
        (manager.getContext() as? LatinIME)?.let { changeCurrentLineIndent(it, true) }
    },
    windowImpl = null,
)

val DeindentAction = Action(
    icon = R.drawable.arrow_left,
    name = R.string.smart_formatting_deindent_action,
    simplePressImpl = { manager, _ ->
        (manager.getContext() as? LatinIME)?.let { changeCurrentLineIndent(it, false) }
    },
    windowImpl = null,
)

package org.futo.inputmethod.latin.smartformatting

data class LineRewrite(val text: String)

/** The indentation unit used by both manual actions and list shortcuts. */
fun indentLine(line: String, spaces: Int): String = " ".repeat(spaces.coerceIn(1, 16)) + line

fun deindentLine(line: String, spaces: Int): String {
    val count = line.takeWhile { it == ' ' }.length.coerceAtMost(spaces.coerceIn(1, 16))
    return line.drop(count)
}

private val listLine = Regex("""^([ \t]*)([-+*]|\d+[.)]) (.*)$""")
private val htmlTag = Regex("""<([A-Za-z][A-Za-z0-9:_-]*)(?:\s+[^<>]*)?>$""")
private val bbcodeTag = Regex("""\[([A-Za-z][A-Za-z0-9_-]*)(?:=[^\[\]]+)?]$""")
private val htmlVoid = setOf("area", "base", "br", "col", "embed", "hr", "img", "input",
    "link", "meta", "param", "source", "track", "wbr")

/** Returns the list prefix to insert immediately after a newly typed newline. */
fun listContinuation(line: String): String? {
    val match = listLine.matchEntire(line) ?: return null
    val (indent, marker, contents) = match.destructured
    // Pressing Enter on an empty list item leaves the list rather than looping forever.
    if (contents.isEmpty()) return ""
    val next = if (marker.first().isDigit()) {
        val digits = marker.dropLast(1)
        val number = digits.toLongOrNull() ?: return null
        if (number == Long.MAX_VALUE) return null
        "${number + 1}${marker.last()}"
    } else marker
    return "$indent$next "
}

/**
 * Length of an exactly empty Markdown list item, including its indentation and marker.
 * Extra spaces or any other content after the first space make it nonempty.
 */
fun emptyListItemPrefixLength(line: String): Int? {
    val match = listLine.matchEntire(line) ?: return null
    return if (match.groupValues[3].isEmpty()) line.length else null
}

/** The optional space/dash shortcuts only operate on a line containing an empty list item. */
fun listShortcutRewrite(line: String, spaces: Int, indentShortcut: Boolean, deindentShortcut: Boolean): LineRewrite? {
    val match = listLine.matchEntire(line) ?: return null
    val (leading, marker, suffix) = match.destructured
    if (indentShortcut && suffix == "  ") return LineRewrite(indentLine("$leading$marker ", spaces))
    if (deindentShortcut && suffix == "--" && leading.startsWith(" ")) {
        return LineRewrite(deindentLine("$leading$marker ", spaces))
    }
    return null
}

/** Returns a closing tag for a newly completed opening tag, or null. */
fun closingTag(beforeCursor: String, afterCursor: String, html: Boolean, bbcode: Boolean): String? {
    if (html && beforeCursor.endsWith(">")) {
        val match = htmlTag.find(beforeCursor)
        if (match != null) {
            val name = match.groupValues[1]
            val raw = match.value
            if (!raw.endsWith("/>") && name.lowercase() !in htmlVoid) {
                val close = "</$name>"
                if (!afterCursor.startsWith(close)) return close
            }
        }
    }
    if (bbcode && beforeCursor.endsWith("]")) {
        val match = bbcodeTag.find(beforeCursor)
        if (match != null) {
            val close = "[/${match.groupValues[1]}]"
            if (!afterCursor.startsWith(close)) return close
        }
    }
    return null
}

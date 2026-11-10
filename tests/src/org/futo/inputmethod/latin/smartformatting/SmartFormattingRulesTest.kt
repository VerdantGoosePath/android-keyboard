package org.futo.inputmethod.latin.smartformatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartFormattingRulesTest {
    @Test fun continuesBulletsAndNumberedLists() {
        assertEquals("    - ", listContinuation("    - item"))
        assertEquals("  * ", listContinuation("  * item"))
        assertEquals("+ ", listContinuation("+ hello"))
        assertEquals("10. ", listContinuation("9. item"))
        assertEquals("101) ", listContinuation("100) text"))
        assertNull(listContinuation("plain text"))
    }

    @Test fun emptyItemsStopContinuation() {
        assertEquals("", listContinuation("- "))
        assertEquals("", listContinuation("  1. "))
    }

    @Test fun clearsOnlyTrulyEmptyListItems() {
        assertEquals(2, emptyListItemPrefixLength("- "))
        assertEquals(6, emptyListItemPrefixLength("    - "))
        assertEquals(3, emptyListItemPrefixLength("1. "))
        assertEquals(5, emptyListItemPrefixLength("  1) "))
        assertNull(emptyListItemPrefixLength("+  "))
        assertNull(emptyListItemPrefixLength("-  "))
        assertNull(emptyListItemPrefixLength("- text"))
        assertNull(emptyListItemPrefixLength("1. x"))
        assertNull(emptyListItemPrefixLength("  1.  "))
        assertNull(emptyListItemPrefixLength("just text"))
        assertEquals("- ", listContinuation("-  "))
        assertEquals("2. ", listContinuation("1.  "))
    }

    @Test fun indentAndDeindentDoNotTouchContent() {
        assertEquals("    plain", indentLine("plain", 4))
        assertEquals("  - item", deindentLine("    - item", 2))
        assertEquals("content", deindentLine("content", 4))
    }

    @Test fun listWhitespaceShortcutsPreserveMarker() {
        assertEquals("    - ", listShortcutRewrite("-   ", 4, true, true)?.text)
        assertEquals("  - ", listShortcutRewrite("    - --", 2, true, true)?.text)
        assertEquals("    4. ", listShortcutRewrite("4.   ", 4, true, true)?.text)
        assertNull(listShortcutRewrite("body   ", 4, true, true))
        assertNull(listShortcutRewrite("-   ", 4, false, true))
        assertNull(listShortcutRewrite(" - --", 4, true, false))
    }

    @Test fun closesHtmlAndBbcodeTags() {
        assertEquals("</section>", closingTag("<section>", "", true, false))
        assertEquals("</p>", closingTag("<p class=\"x\">", "", true, false))
        assertNull(closingTag("<br>", "", true, false))
        assertNull(closingTag("</div>", "", true, false))
        assertNull(closingTag("<div/>", "", true, false))
        assertNull(closingTag("<div>", "</div>", true, false))
        assertEquals("[/color]", closingTag("[color=red]", "", false, true))
        assertNull(closingTag("[/color]", "", false, true))
        assertNull(closingTag("[tag]", "", false, false))
    }
}

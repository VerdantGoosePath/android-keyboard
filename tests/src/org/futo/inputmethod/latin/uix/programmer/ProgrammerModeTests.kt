package org.futo.inputmethod.latin.uix.programmer

import android.test.AndroidTestCase
import android.view.KeyEvent

class ProgrammerModeTests : AndroidTestCase() {
    fun testLayoutRoundTripPreservesCustomKeys() {
        val layout = ProgrammerLayout(
            keys = listOf(
                ProgrammerKey("a", ProgrammerKeyType.Text, label = "Snippet", icon = "{ }", value = "λ (x) {\n  return x;\n}", width = 2.25f),
                ProgrammerKey("b", ProgrammerKeyType.Tab, tabSpaces = 4),
                ProgrammerKey("c", ProgrammerKeyType.Action, value = "undo")
            )
        )
        assertEquals(layout, decodeProgrammerLayout(encodeProgrammerLayout(layout)))
    }

    fun testMalformedLayoutFallsBackToDefaults() {
        assertEquals(defaultProgrammerKeys(), decodeProgrammerLayout("{invalid").keys)
    }

    fun testResizeRightPreservesTotalAndRelativeProportions() {
        val keys = listOf(
            ProgrammerKey("a", width = 1f),
            ProgrammerKey("b", width = 2f),
            ProgrammerKey("c", width = 3f),
            ProgrammerKey("d", width = 4f)
        )
        val resized = resizeProgrammerKeys(keys, index = 1, rightEdge = true, deltaFraction = 0.1f)
        assertEquals(keys.sumOf { it.width.toDouble() }, resized.sumOf { it.width.toDouble() }, 0.0001)
        assertEquals(3f / 4f, resized[2].width / resized[3].width, 0.0001f)
        assertEquals(keys[0].width, resized[0].width, 0.0001f)
        assertTrue(resized[1].width > keys[1].width)
    }

    fun testResizeLeftPreservesTotalAndRightSide() {
        val keys = (0..3).map { ProgrammerKey(it.toString(), width = 1f) }
        val resized = resizeProgrammerKeys(keys, index = 2, rightEdge = false, deltaFraction = -0.1f)
        assertEquals(4.0, resized.sumOf { it.width.toDouble() }, 0.0001)
        assertEquals(1f, resized[3].width, 0.0001f)
        assertTrue(resized[2].width > 1f)
    }

    fun testNoResizeAtBoundary() {
        val keys = listOf(ProgrammerKey("a"), ProgrammerKey("b"))
        assertEquals(keys, resizeProgrammerKeys(keys, 1, rightEdge = true, deltaFraction = 0.5f))
        assertEquals(keys, resizeProgrammerKeys(keys, 0, rightEdge = false, deltaFraction = 0.5f))
        assertEquals(keys, resizeProgrammerKeys(keys, -1, rightEdge = true, deltaFraction = 0.5f))
    }

    fun testCombinedModifiersAndAutoRelease() {
        val state = ProgrammerModifiers()
        state.respond(ProgrammerKeyType.Ctrl, ModifierResponse.Toggle, StubManager)
        state.respond(ProgrammerKeyType.Shift, ModifierResponse.Toggle, StubManager)
        assertTrue(state.active(ProgrammerKeyType.Ctrl))
        assertTrue(state.active(ProgrammerKeyType.Shift))
        assertTrue(state.metaState() and KeyEvent.META_CTRL_ON != 0)
        assertTrue(state.metaState() and KeyEvent.META_SHIFT_ON != 0)
        state.afterNonModifier(autoRelease = false)
        assertTrue(state.anyActive())
        state.afterNonModifier(autoRelease = true)
        assertFalse(state.anyActive())
    }

    fun testHeldModifierSurvivesOneShotRelease() {
        val state = ProgrammerModifiers()
        state.onDown(ProgrammerKeyType.Alt, ModifierResponse.Hold)
        assertTrue(state.active(ProgrammerKeyType.Alt))
        state.afterNonModifier(autoRelease = true)
        assertTrue(state.active(ProgrammerKeyType.Alt))
        state.onRelease(ProgrammerKeyType.Alt, ModifierResponse.Hold)
        assertFalse(state.anyActive())
    }

    fun testLetterAndNumberKeycodes() {
        assertEquals(KeyEvent.KEYCODE_F, keyCodeForCodePoint('f'.code)?.first)
        assertEquals(KeyEvent.KEYCODE_Z, keyCodeForCodePoint('Z'.code)?.first)
        assertEquals(KeyEvent.KEYCODE_7, keyCodeForCodePoint('7'.code)?.first)
        assertEquals(KeyEvent.KEYCODE_TAB, keyCodeForCodePoint('\t'.code)?.first)
    }
}

/** For toggle-only tests no keyboard operations should be sent. */
private object StubManager : org.futo.inputmethod.latin.uix.KeyboardManagerForAction {
    override fun getContext() = error("unused")
    override fun getLifecycleScope(): androidx.lifecycle.LifecycleCoroutineScope = error("unused")
    override fun createInputTransaction(): org.futo.inputmethod.latin.uix.ActionInputTransaction = error("unused")
    override fun typeText(v: String) = error("unused")
    override fun typeTextSurroundedByWhitespace(v: String) = error("unused")
    override fun typeUri(uri: android.net.Uri, mimeTypes: List<String>, ignoreConnectionOverride: Boolean) = error("unused")
    override fun appSupportsImageInsertion(schema: String, ignoreConnectionOverride: Boolean) = error("unused")
    override fun backspace(amount: Int) = error("unused")
    override fun closeActionWindow() = error("unused")
    override fun forceActionWindowAboveKeyboard(to: Boolean) = error("unused")
    override fun triggerSystemVoiceInput() = error("unused")
    override fun getThemeProvider(): org.futo.inputmethod.latin.uix.DynamicThemeProvider = error("unused")
    override fun sendCodePointEvent(codePoint: Int) = error("unused")
    override fun sendKeyEvent(keyCode: Int, metaState: Int) = error("unused")
    override fun isShifted() = false
    override fun cursorLeft(steps: Int, stepOverWords: Boolean, select: Boolean) = error("unused")
    override fun cursorRight(steps: Int, stepOverWords: Boolean, select: Boolean) = error("unused")
    override fun performHapticAndAudioFeedback(code: Int, view: android.view.View) = error("unused")
    override fun announce(s: String) = error("unused")
    override fun getActiveLocales(): List<java.util.Locale> = emptyList()
    override fun overrideInputConnection(inputConnection: android.view.inputmethod.InputConnection, editorInfo: android.view.inputmethod.EditorInfo) = error("unused")
    override fun unsetInputConnection(): Boolean = error("unused")
    override fun requestDialog(text: String, options: List<org.futo.inputmethod.latin.uix.DialogRequestItem>, onCancel: () -> Unit) = error("unused")
    override fun openInputMethodPicker() = error("unused")
    override fun activateAction(action: org.futo.inputmethod.latin.uix.Action) = error("unused")
    override fun showActionEditor() = error("unused")
    override fun getLatinIMEForDebug(): org.futo.inputmethod.latin.LatinIME = error("unused")
    override fun isDeviceLocked() = false
    override fun getSizingCalculator(): org.futo.inputmethod.v2keyboard.KeyboardSizingCalculator = error("unused")
    override fun showResizer() = error("unused")
    override fun getTutorialMode() = org.futo.inputmethod.latin.uix.TutorialMode.None
    override fun setTutorialArrowPosition(coordinates: androidx.compose.ui.layout.LayoutCoordinates) = error("unused")
    override fun markTutorialCompleted() = error("unused")
    override fun overrideKeyboardTypeface(typeface: android.graphics.Typeface?) = error("unused")
    override fun copyToClipboard(cut: Boolean) = error("unused")
    override fun pasteFromClipboard() = error("unused")
    override fun getSelectedText(): String? = null
    override fun getCurrentIME(): org.futo.inputmethod.engine.IMEInterface = error("unused")
}

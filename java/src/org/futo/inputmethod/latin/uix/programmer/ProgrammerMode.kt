package org.futo.inputmethod.latin.uix.programmer

import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateMapOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.SettingsKey
import org.futo.inputmethod.latin.uix.actions.AllActionsMap

@Serializable
enum class ProgrammerKeyType {
    Unconfigured, Ctrl, Alt, Shift, Tab, Home, End, Delete, Backspace,
    Left, Right, Up, Down, Action, Text
}

@Serializable
data class ProgrammerKey(
    val id: String,
    val type: ProgrammerKeyType = ProgrammerKeyType.Unconfigured,
    val label: String = "",
    val icon: String = "",
    val value: String = "",
    val width: Float = 1f,
    val tabSpaces: Int = 0
) {
    fun displayLabel(): String = label.ifEmpty {
        when(type) {
            ProgrammerKeyType.Unconfigured -> "+"
            ProgrammerKeyType.Ctrl -> "Ctrl"
            ProgrammerKeyType.Alt -> "Alt"
            ProgrammerKeyType.Shift -> "Shift"
            ProgrammerKeyType.Tab -> "Tab"
            ProgrammerKeyType.Home -> "Home"
            ProgrammerKeyType.End -> "End"
            ProgrammerKeyType.Delete -> "Del"
            ProgrammerKeyType.Backspace -> "⌫"
            ProgrammerKeyType.Left -> "←"
            ProgrammerKeyType.Right -> "→"
            ProgrammerKeyType.Up -> "↑"
            ProgrammerKeyType.Down -> "↓"
            ProgrammerKeyType.Action -> value
            ProgrammerKeyType.Text -> value.ifEmpty { "Text" }
        }
    }
}

@Serializable
data class ProgrammerLayout(
    val keys: List<ProgrammerKey> = defaultProgrammerKeys()
)

fun defaultProgrammerKeys(): List<ProgrammerKey> = listOf(
    ProgrammerKey("ctrl", ProgrammerKeyType.Ctrl),
    ProgrammerKey("alt", ProgrammerKeyType.Alt),
    ProgrammerKey("tab", ProgrammerKeyType.Tab),
    ProgrammerKey("left", ProgrammerKeyType.Left),
    ProgrammerKey("down", ProgrammerKeyType.Down),
    ProgrammerKey("up", ProgrammerKeyType.Up),
    ProgrammerKey("right", ProgrammerKeyType.Right),
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun decodeProgrammerLayout(raw: String): ProgrammerLayout = try {
    json.decodeFromString<ProgrammerLayout>(raw).let { layout ->
        layout.copy(keys = layout.keys.take(24).map { it.copy(width = it.width.coerceIn(0.25f, 12f)) })
    }
} catch (_: Exception) {
    ProgrammerLayout()
}

fun encodeProgrammerLayout(layout: ProgrammerLayout): String = json.encodeToString(layout)

val ProgrammerModeEnabled = SettingsKey(booleanPreferencesKey("programmer_mode_enabled"), false)
val ProgrammerLayoutSetting = SettingsKey(
    stringPreferencesKey("programmer_mode_layout"),
    encodeProgrammerLayout(ProgrammerLayout())
)

// Keycaps are normal alphabet-style keys. Flat matches the unframed arrow/number row.
const val KEYCAPS = 0
const val FLAT = 1
val ProgrammerKeyStyle = SettingsKey(intPreferencesKey("programmer_mode_style"), KEYCAPS)

enum class ModifierGesture { Tap, Hold }

enum class ModifierResponse { Toggle, Hold, Tap }

val CtrlTap = SettingsKey(intPreferencesKey("programmer_ctrl_tap"), ModifierResponse.Toggle.ordinal)
val CtrlHold = SettingsKey(intPreferencesKey("programmer_ctrl_hold"), ModifierResponse.Tap.ordinal)
val AltTap = SettingsKey(intPreferencesKey("programmer_alt_tap"), ModifierResponse.Toggle.ordinal)
val AltHold = SettingsKey(intPreferencesKey("programmer_alt_hold"), ModifierResponse.Tap.ordinal)
val ShiftTap = SettingsKey(intPreferencesKey("programmer_shift_tap"), ModifierResponse.Toggle.ordinal)
val ShiftHold = SettingsKey(intPreferencesKey("programmer_shift_hold"), ModifierResponse.Tap.ordinal)
val ProgrammerAutoRelease = SettingsKey(booleanPreferencesKey("programmer_auto_release"), true)

fun tapSetting(type: ProgrammerKeyType) = when(type) {
    ProgrammerKeyType.Ctrl -> CtrlTap
    ProgrammerKeyType.Alt -> AltTap
    ProgrammerKeyType.Shift -> ShiftTap
    else -> error("Not a modifier: $type")
}

fun holdSetting(type: ProgrammerKeyType) = when(type) {
    ProgrammerKeyType.Ctrl -> CtrlHold
    ProgrammerKeyType.Alt -> AltHold
    ProgrammerKeyType.Shift -> ShiftHold
    else -> error("Not a modifier: $type")
}

fun isModifier(type: ProgrammerKeyType) =
    type == ProgrammerKeyType.Ctrl || type == ProgrammerKeyType.Alt || type == ProgrammerKeyType.Shift

fun modifierKeyCode(type: ProgrammerKeyType) = when(type) {
    ProgrammerKeyType.Ctrl -> KeyEvent.KEYCODE_CTRL_LEFT
    ProgrammerKeyType.Alt -> KeyEvent.KEYCODE_ALT_LEFT
    ProgrammerKeyType.Shift -> KeyEvent.KEYCODE_SHIFT_LEFT
    else -> error("Not a modifier: $type")
}

private fun metaFor(type: ProgrammerKeyType) = when(type) {
    ProgrammerKeyType.Ctrl -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    ProgrammerKeyType.Alt -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    ProgrammerKeyType.Shift -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    else -> 0
}

/**
 * Sticky and physically held modifier states are separate: lifting a held modifier never clears
 * a previously latched one. This state belongs to the active keyboard, not the settings preview.
 */
class ProgrammerModifiers {
    private val latched = mutableStateMapOf<ProgrammerKeyType, Boolean>()
    private val held = mutableStateMapOf<ProgrammerKeyType, Boolean>()

    fun active(type: ProgrammerKeyType): Boolean = latched[type] == true || held[type] == true
    fun anyActive(): Boolean = listOf(ProgrammerKeyType.Ctrl, ProgrammerKeyType.Alt, ProgrammerKeyType.Shift).any(::active)
    fun metaState(): Int = listOf(ProgrammerKeyType.Ctrl, ProgrammerKeyType.Alt, ProgrammerKeyType.Shift)
        .filter(::active).fold(0) { result, modifier -> result or metaFor(modifier) }

    fun onDown(type: ProgrammerKeyType, holdResponse: ModifierResponse) {
        if(holdResponse == ModifierResponse.Hold) held[type] = true
    }

    fun onRelease(type: ProgrammerKeyType, holdResponse: ModifierResponse) {
        if(holdResponse == ModifierResponse.Hold) held.remove(type)
    }

    fun respond(type: ProgrammerKeyType, response: ModifierResponse, manager: KeyboardManagerForAction) {
        when(response) {
            ModifierResponse.Toggle -> latched[type] = !active(type)
            ModifierResponse.Hold -> { /* handled while finger is down */ }
            ModifierResponse.Tap -> manager.sendKeyEvent(modifierKeyCode(type), metaFor(type))
        }
    }

    fun afterNonModifier(autoRelease: Boolean) {
        if(autoRelease) latched.clear()
    }

    fun clear() { latched.clear(); held.clear() }
}

fun responseFromOrdinal(value: Int): ModifierResponse =
    ModifierResponse.entries.getOrElse(value) { ModifierResponse.Toggle }

/**
 * Maps ordinary software character events to platform keycodes instead of committing text.
 * Non-ASCII characters that Android cannot represent as a key event are left to the IME.
 */
fun keyCodeForCodePoint(codePoint: Int): Pair<Int, Int>? {
    if(codePoint in 'a'.code..'z'.code) return (KeyEvent.KEYCODE_A + codePoint - 'a'.code) to 0
    if(codePoint in 'A'.code..'Z'.code) return (KeyEvent.KEYCODE_A + codePoint - 'A'.code) to KeyEvent.META_SHIFT_ON
    if(codePoint in '0'.code..'9'.code) return (KeyEvent.KEYCODE_0 + codePoint - '0'.code) to 0
    if(codePoint == Constants.CODE_TAB) return KeyEvent.KEYCODE_TAB to 0
    if(codePoint == Constants.CODE_ENTER) return KeyEvent.KEYCODE_ENTER to 0
    if(codePoint == Constants.CODE_SPACE) return KeyEvent.KEYCODE_SPACE to 0
    if(!Character.isValidCodePoint(codePoint)) return null
    return try {
        val chars = Character.toChars(codePoint)
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(chars)
        val key = events?.firstOrNull { it.action == KeyEvent.ACTION_DOWN && !KeyEvent.isModifierKey(it.keyCode) }
        key?.let { it.keyCode to it.metaState }
    } catch (_: Exception) { null }
}

fun dispatchProgrammerEvent(
    event: Event,
    modifiers: ProgrammerModifiers,
    manager: KeyboardManagerForAction,
    autoRelease: Boolean
): Boolean {
    if(!modifiers.anyActive()) return false
    // The ordinary Shift key must continue to use the keyboard's own shift/caps state.
    if(event.mKeyCode == Constants.CODE_SHIFT || event.mKeyCode == Constants.CODE_SYMBOL_SHIFT ||
        event.mKeyCode == Constants.CODE_CAPSLOCK) return false

    val mapped = when {
        event.mKeyCode == Constants.CODE_DELETE -> KeyEvent.KEYCODE_DEL to 0
        event.mKeyCode == Constants.CODE_SHIFT_ENTER -> KeyEvent.KEYCODE_ENTER to KeyEvent.META_SHIFT_ON
        event.mCodePoint > 0 -> keyCodeForCodePoint(event.mCodePoint)
        else -> null
    }

    if(mapped != null) {
        manager.sendKeyEvent(mapped.first, modifiers.metaState() or mapped.second)
        modifiers.afterNonModifier(autoRelease)
        return true
    }

    // Do not swallow control keys or non-key events that the active IME must process.
    if(event.mCodePoint > 0 || event.mKeyCode == Constants.CODE_DELETE) {
        modifiers.afterNonModifier(autoRelease)
    }
    return false
}

fun sendProgrammerKey(
    key: ProgrammerKey,
    modifiers: ProgrammerModifiers,
    manager: KeyboardManagerForAction,
    autoRelease: Boolean
) {
    val code = when(key.type) {
        ProgrammerKeyType.Home -> KeyEvent.KEYCODE_MOVE_HOME
        ProgrammerKeyType.End -> KeyEvent.KEYCODE_MOVE_END
        ProgrammerKeyType.Delete -> KeyEvent.KEYCODE_FORWARD_DEL
        ProgrammerKeyType.Backspace -> KeyEvent.KEYCODE_DEL
        ProgrammerKeyType.Left -> KeyEvent.KEYCODE_DPAD_LEFT
        ProgrammerKeyType.Right -> KeyEvent.KEYCODE_DPAD_RIGHT
        ProgrammerKeyType.Up -> KeyEvent.KEYCODE_DPAD_UP
        ProgrammerKeyType.Down -> KeyEvent.KEYCODE_DPAD_DOWN
        ProgrammerKeyType.Tab -> if(key.tabSpaces <= 0) KeyEvent.KEYCODE_TAB else null
        else -> null
    }
    if(isModifier(key.type) || key.type == ProgrammerKeyType.Unconfigured) return

    val shift = if(manager.isShifted()) KeyEvent.META_SHIFT_ON else 0
    if(code != null) {
        manager.sendKeyEvent(code, modifiers.metaState() or shift)
    } else when(key.type) {
        ProgrammerKeyType.Tab -> {
            val text = " ".repeat(key.tabSpaces.coerceIn(1, 16))
            if(modifiers.anyActive()) {
                text.forEach { manager.sendKeyEvent(KeyEvent.KEYCODE_SPACE, modifiers.metaState() or shift) }
            } else manager.typeText(text)
        }
        ProgrammerKeyType.Text -> {
            if(modifiers.anyActive()) {
                key.value.codePoints().toArray().forEach { cp ->
                    val mapped = keyCodeForCodePoint(cp)
                    if(mapped != null) manager.sendKeyEvent(mapped.first, modifiers.metaState() or mapped.second or shift)
                    else manager.typeText(String(Character.toChars(cp)))
                }
            } else manager.typeText(key.value)
        }
        ProgrammerKeyType.Action -> {
            AllActionsMap[key.value]?.let { action ->
                manager.activateAction(action)
            }
        }
        else -> Unit
    }
    modifiers.afterNonModifier(autoRelease)
}

/** Pure width adjustment, keeping the sum and proportions of the opposite-side keys. */
fun resizeProgrammerKeys(
    keys: List<ProgrammerKey>, index: Int, rightEdge: Boolean, deltaFraction: Float
): List<ProgrammerKey> {
    if(index !in keys.indices || !deltaFraction.isFinite()) return keys
    val adjustable = if(rightEdge) (index + 1 until keys.size).toList() else (0 until index).toList()
    if(adjustable.isEmpty()) return keys
    val widths = keys.map { it.width.coerceAtLeast(0.25f) }.toMutableList()
    val total = widths.sum()
    val others = adjustable.sumOf { widths[it].toDouble() }.toFloat()
    val minWidth = 0.25f
    val change = (if(rightEdge) deltaFraction else -deltaFraction) * total
    // The neighbours shrink proportionally, but none may fall below the stored minimum.
    val maxGrowth = adjustable.minOf { i ->
        (widths[i] - minWidth).coerceAtLeast(0f) * others / widths[i]
    }
    val clamped = change.coerceIn(minWidth - widths[index], maxGrowth)
    if(clamped == 0f) return keys
    widths[index] += clamped
    for(i in adjustable) widths[i] -= clamped * widths[i] / others
    return keys.mapIndexed { i, key -> key.copy(width = widths[i].coerceAtLeast(minWidth)) }
}

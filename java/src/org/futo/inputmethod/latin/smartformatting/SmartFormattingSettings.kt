package org.futo.inputmethod.latin.smartformatting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.futo.inputmethod.latin.LatinIME
import org.futo.inputmethod.latin.uix.SettingsKey
import org.futo.inputmethod.latin.uix.deferSetSetting
import org.futo.inputmethod.latin.uix.getSetting

object SmartFormattingSettings {
    val startup = SettingsKey(stringPreferencesKey("smart_formatting_startup"), "disabled")
    val lastEnabled = SettingsKey(booleanPreferencesKey("smart_formatting_last_enabled"), false)
    val spacesPerTab = SettingsKey(intPreferencesKey("smart_formatting_spaces_per_tab"), 4)
    val listContinuation = SettingsKey(booleanPreferencesKey("smart_formatting_list_continuation"), true)
    val clearEmptyListItem = SettingsKey(booleanPreferencesKey("smart_formatting_clear_empty_list_item"), true)
    val indentShortcut = SettingsKey(booleanPreferencesKey("smart_formatting_indent_shortcut"), true)
    val deindentShortcut = SettingsKey(booleanPreferencesKey("smart_formatting_deindent_shortcut"), true)
    val closeRound = SettingsKey(booleanPreferencesKey("smart_formatting_close_round"), true)
    val closeSquare = SettingsKey(booleanPreferencesKey("smart_formatting_close_square"), true)
    val closeCurly = SettingsKey(booleanPreferencesKey("smart_formatting_close_curly"), true)
    val closeAngle = SettingsKey(booleanPreferencesKey("smart_formatting_close_angle"), false)
    val closeSingleQuote = SettingsKey(booleanPreferencesKey("smart_formatting_close_single_quote"), false)
    val closeDoubleQuote = SettingsKey(booleanPreferencesKey("smart_formatting_close_double_quote"), false)
    val closeAsterisk = SettingsKey(booleanPreferencesKey("smart_formatting_close_asterisk"), false)
    val closeBacktick = SettingsKey(booleanPreferencesKey("smart_formatting_close_backtick"), false)
    val closeHtmlTag = SettingsKey(booleanPreferencesKey("smart_formatting_close_html_tag"), false)
    val closeBbcodeTag = SettingsKey(booleanPreferencesKey("smart_formatting_close_bbcode_tag"), false)
}

/** Session-wide switch; the user's last manual choice is persisted separately. */
object SmartFormattingSession {
    var enabled by mutableStateOf(false)
        private set

    fun onKeyboardShown(ime: LatinIME) {
        enabled = when (ime.getSetting(SmartFormattingSettings.startup)) {
            "enabled" -> true
            "remember" -> ime.getSetting(SmartFormattingSettings.lastEnabled)
            else -> false
        }
    }

    fun toggle(ime: LatinIME) {
        enabled = !enabled
        ime.deferSetSetting(ime, SmartFormattingSettings.lastEnabled, enabled)
    }
}

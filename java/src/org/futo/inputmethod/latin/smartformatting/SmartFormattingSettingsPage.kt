package org.futo.inputmethod.latin.smartformatting

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.userSettingDecorationOnly
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore

val SmartFormattingMenu = UserSettingsMenu(
    title = R.string.smart_formatting_settings_title,
    navPath = "smartFormatting",
    registerNavPath = true,
    settings = listOf(
        UserSetting(R.string.smart_formatting_startup) {
            val setting = useDataStore(SmartFormattingSettings.startup)
            val choices = mapOf(
                "disabled" to stringResource(R.string.smart_formatting_startup_disabled),
                "enabled" to stringResource(R.string.smart_formatting_startup_enabled),
                "remember" to stringResource(R.string.smart_formatting_startup_remember),
            )
            DropDownPickerSettingItem(
                label = stringResource(R.string.smart_formatting_startup),
                options = listOf("disabled", "enabled", "remember"),
                selection = setting.value,
                onSet = { setting.setValue(it) },
                getDisplayName = { choices[it] ?: it },
            )
        },
        userSettingDecorationOnly { ScreenTitle(stringResource(R.string.smart_formatting_lists)) },
        userSettingToggleDataStore(R.string.smart_formatting_continue_lists, SmartFormattingSettings.listContinuation),
        UserSetting(R.string.smart_formatting_spaces_per_tab) {
            val setting = useDataStore(SmartFormattingSettings.spacesPerTab)
            DropDownPickerSettingItem(
                label = stringResource(R.string.smart_formatting_spaces_per_tab),
                options = listOf(1, 2, 4, 8, 16),
                selection = setting.value,
                onSet = { setting.setValue(it) },
                getDisplayName = { it.toString() },
            )
        },
        userSettingToggleDataStore(R.string.smart_formatting_indent_shortcut, SmartFormattingSettings.indentShortcut),
        userSettingToggleDataStore(R.string.smart_formatting_deindent_shortcut, SmartFormattingSettings.deindentShortcut),
        userSettingDecorationOnly { ScreenTitle(stringResource(R.string.smart_formatting_characters)) },
        userSettingToggleDataStore(R.string.smart_formatting_close_round, SmartFormattingSettings.closeRound),
        userSettingToggleDataStore(R.string.smart_formatting_close_square, SmartFormattingSettings.closeSquare),
        userSettingToggleDataStore(R.string.smart_formatting_close_curly, SmartFormattingSettings.closeCurly),
        userSettingToggleDataStore(R.string.smart_formatting_close_angle, SmartFormattingSettings.closeAngle),
        userSettingToggleDataStore(R.string.smart_formatting_close_single_quote, SmartFormattingSettings.closeSingleQuote),
        userSettingToggleDataStore(R.string.smart_formatting_close_double_quote, SmartFormattingSettings.closeDoubleQuote),
        userSettingToggleDataStore(R.string.smart_formatting_close_asterisk, SmartFormattingSettings.closeAsterisk),
        userSettingToggleDataStore(R.string.smart_formatting_close_backtick, SmartFormattingSettings.closeBacktick),
        userSettingDecorationOnly { ScreenTitle(stringResource(R.string.smart_formatting_tags)) },
        userSettingToggleDataStore(R.string.smart_formatting_close_html_tag, SmartFormattingSettings.closeHtmlTag),
        userSettingToggleDataStore(R.string.smart_formatting_close_bbcode_tag, SmartFormattingSettings.closeBbcodeTag),
    )
)

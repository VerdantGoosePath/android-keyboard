package org.futo.inputmethod.latin.uix.actions

import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.programmer.ProgrammerModeEnabled
import org.futo.inputmethod.latin.uix.setSetting

/** A normal configurable Action: available in the action editor, favourites and action key. */
val ToggleProgrammerModeAction = Action(
    icon = R.drawable.keyboard_regular,
    name = R.string.programmer_mode_toggle_action,
    simplePressImpl = { manager, _ ->
        val context = manager.getContext()
        manager.getLifecycleScope().launch {
            context.setSetting(ProgrammerModeEnabled, !context.getSetting(ProgrammerModeEnabled))
        }
    },
    windowImpl = null
)

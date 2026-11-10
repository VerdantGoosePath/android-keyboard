package org.futo.inputmethod.latin.uix.programmer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.settings.BottomSpacer
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingToggleRaw
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import org.futo.inputmethod.latin.uix.setSettingBlocking
import java.util.UUID
import kotlin.math.abs

private val supportedIcons = listOf("", "⌃", "⌥", "⇧", "↹", "⌫", "⌦", "←", "→", "↑", "↓", "⌂", "⚙", "✦", "{ }")

private fun typeName(type: ProgrammerKeyType): String = when(type) {
    ProgrammerKeyType.Unconfigured -> "Unconfigured"
    ProgrammerKeyType.Text -> "String"
    else -> type.name
}

private fun responseName(response: ModifierResponse) = when(response) {
    ModifierResponse.Toggle -> "Toggle"
    ModifierResponse.Tap -> "Tap"
    ModifierResponse.Hold -> "Hold"
}

@Composable
private fun BehaviourSelector(
    type: ProgrammerKeyType,
    gesture: ModifierGesture
) {
    val key = if(gesture == ModifierGesture.Tap) tapSetting(type) else holdSetting(type)
    val setting = useDataStore(key)
    val options = if(gesture == ModifierGesture.Tap)
        listOf(ModifierResponse.Toggle, ModifierResponse.Tap)
    else ModifierResponse.entries
    DropDownPickerSettingItem(
        label = "${type.name} · ${gesture.name}",
        options = options,
        selection = responseFromOrdinal(setting.value),
        onSet = { setting.setValue(it.ordinal) },
        getDisplayName = ::responseName
    )
}

@Composable
fun ProgrammerModeSettings(navController: NavHostController) {
    val saved = useDataStore(ProgrammerLayoutSetting)
    var layout by remember { mutableStateOf(decodeProgrammerLayout(saved.value)) }
    var selected by remember { mutableStateOf<String?>(null) }
    val style = useDataStore(ProgrammerKeyStyle)
    val autoRelease = useDataStore(ProgrammerAutoRelease)
    val enabled = useDataStore(ProgrammerModeEnabled)
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    val context = LocalContext.current
    val latestLayout by rememberUpdatedState(layout)
    DisposableEffect(Unit) {
        onDispose {
            context.setSettingBlocking(ProgrammerLayoutSetting.key, encodeProgrammerLayout(latestLayout))
        }
    }

    fun update(newLayout: ProgrammerLayout) {
        layout = newLayout
        saved.setValue(encodeProgrammerLayout(newLayout))
    }

    ScrollableList(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if(!rowBounds.contains(down.position)) selected = null
            }
        }
    ) {
        ScreenTitle(stringResource(R.string.programmer_mode_title), showBack = true, navController = navController)
        SettingToggleRaw(
            title = stringResource(R.string.programmer_mode_enabled),
            enabled = enabled.value,
            setValue = enabled.setValue
        )
        Text(
            stringResource(R.string.programmer_mode_keys),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 12.dp)
        )
        ProgrammerRow(
            keys = layout.keys,
            style = style.value,
            selectedId = selected,
            onSelect = { selected = it },
            modifier = Modifier.onGloballyPositioned { rowBounds = it.boundsInParent() },
            onResize = { index, right, change ->
                update(layout.copy(keys = resizeProgrammerKeys(layout.keys, index, right, change)))
            }
        )
        layout.keys.forEachIndexed { index, key ->
            val density = LocalDensity.current
            var dragDistance by remember(key.id) { mutableStateOf(0f) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.programmer_mode_reorder),
                    modifier = Modifier
                        .size(42.dp)
                        .pointerInput(key.id) {
                            val threshold = with(density) { 35.dp.toPx() }
                            detectDragGestures(
                                onDragEnd = { dragDistance = 0f },
                                onDragCancel = { dragDistance = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragDistance += drag.y
                                    if(abs(dragDistance) > threshold) {
                                        val currentIndex = latestLayout.keys.indexOfFirst { it.id == key.id }
                                        val next = currentIndex + if(dragDistance > 0f) 1 else -1
                                        if(currentIndex >= 0 && next in latestLayout.keys.indices) {
                                            val reordered = latestLayout.keys.toMutableList()
                                            reordered.add(next, reordered.removeAt(currentIndex))
                                            update(latestLayout.copy(keys = reordered))
                                            selected = key.id
                                        }
                                        dragDistance = 0f
                                    }
                                }
                            )
                        }
                        .padding(9.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        context.setSettingBlocking(ProgrammerLayoutSetting.key, encodeProgrammerLayout(layout))
                        selected = key.id
                        navController.navigate("programmerMode/key/${key.id}")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(key.displayLabel().ifBlank { typeName(key.type) }, maxLines = 1)
                        Text(
                            buildString {
                                append(typeName(key.type))
                                when(key.type) {
                                    ProgrammerKeyType.Tab -> append(if(key.tabSpaces == 0) " · Tab" else " · ${key.tabSpaces} spaces")
                                    ProgrammerKeyType.Action -> if(key.value.isNotBlank()) append(" · ${key.value}")
                                    ProgrammerKeyType.Text -> if(key.value.isNotBlank()) append(" · ${key.value.take(24)}")
                                    else -> Unit
                                }
                            },
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                IconButton(onClick = {
                    update(layout.copy(keys = layout.keys.filterNot { it.id == key.id }))
                    if(selected == key.id) selected = null
                }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.programmer_mode_delete))
                }
            }
        }
        Button(
            onClick = {
                val newKey = ProgrammerKey(UUID.randomUUID().toString())
                update(layout.copy(keys = layout.keys + newKey))
                selected = newKey.id
            },
            enabled = layout.keys.size < 24,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.programmer_mode_add))
        }
        HorizontalDivider()
        Text(
            stringResource(R.string.programmer_mode_appearance),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 12.dp)
        )
        DropDownPickerSettingItem(
            label = stringResource(R.string.programmer_mode_style),
            options = listOf(KEYCAPS, FLAT),
            selection = style.value,
            onSet = style.setValue,
            getDisplayName = { if(it == KEYCAPS) "Keycaps" else "Bare" }
        )
        HorizontalDivider()
        Text(
            stringResource(R.string.programmer_mode_modifiers),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp, 12.dp)
        )
        for(type in listOf(ProgrammerKeyType.Ctrl, ProgrammerKeyType.Alt, ProgrammerKeyType.Shift)) {
            BehaviourSelector(type, ModifierGesture.Tap)
            BehaviourSelector(type, ModifierGesture.Hold)
        }
        SettingToggleRaw(
            title = stringResource(R.string.programmer_mode_auto_release),
            enabled = autoRelease.value,
            setValue = autoRelease.setValue
        )
        BottomSpacer()
    }
}

@Composable
fun ProgrammerKeySettings(navController: NavHostController, id: String) {
    val saved = useDataStore(ProgrammerLayoutSetting)
    var layout by remember { mutableStateOf(decodeProgrammerLayout(saved.value)) }
    val style = useDataStoreValue(ProgrammerKeyStyle)
    val key = layout.keys.firstOrNull { it.id == id }
    val context = LocalContext.current
    val latestLayout by rememberUpdatedState(layout)
    DisposableEffect(Unit) {
        onDispose {
            context.setSettingBlocking(ProgrammerLayoutSetting.key, encodeProgrammerLayout(latestLayout))
        }
    }

    fun updateKey(replacement: ProgrammerKey) {
        val changed = layout.copy(keys = layout.keys.map { if(it.id == id) replacement else it })
        layout = changed
        saved.setValue(encodeProgrammerLayout(changed))
    }

    ScrollableList {
        ScreenTitle(stringResource(R.string.programmer_mode_edit_key), showBack = true, navController = navController)
        if(key != null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ProgrammerKeyFace(
                    key = key,
                    style = style,
                    active = false,
                    pressed = false,
                    selected = false,
                    modifier = Modifier.width(76.dp).height(42.dp)
                )
            }
            DropDownPickerSettingItem(
                label = stringResource(R.string.programmer_mode_key_type),
                options = ProgrammerKeyType.entries,
                selection = key.type,
                onSet = { updateKey(key.copy(type = it)) },
                getDisplayName = ::typeName
            )
            DropDownPickerSettingItem(
                label = stringResource(R.string.programmer_mode_icon),
                options = supportedIcons,
                selection = key.icon,
                onSet = { updateKey(key.copy(icon = it)) },
                getDisplayName = { it.ifEmpty { "None" } }
            )
            OutlinedTextField(
                value = key.label,
                onValueChange = { updateKey(key.copy(label = it)) },
                label = { Text(stringResource(R.string.programmer_mode_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            when(key.type) {
                ProgrammerKeyType.Tab -> {
                    Text(
                        stringResource(R.string.programmer_mode_tab_spaces, key.tabSpaces),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Slider(
                        value = key.tabSpaces.toFloat(),
                        valueRange = 0f..8f,
                        steps = 7,
                        onValueChange = { updateKey(key.copy(tabSpaces = it.toInt())) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                ProgrammerKeyType.Text -> OutlinedTextField(
                    value = key.value,
                    onValueChange = { updateKey(key.copy(value = it)) },
                    label = { Text(stringResource(R.string.programmer_mode_string)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                ProgrammerKeyType.Action -> {
                    val context = LocalContext.current
                    val names = remember(context) {
                        org.futo.inputmethod.latin.uix.actions.AllActionsMap.mapValues {
                            context.getString(it.value.name)
                        }
                    }
                    DropDownPickerSettingItem(
                        label = stringResource(R.string.programmer_mode_action),
                        options = listOf("") + names.keys,
                        selection = key.value,
                        onSet = { updateKey(key.copy(value = it)) },
                        getDisplayName = { names[it] ?: "Choose action" }
                    )
                }
                else -> Unit
            }
        }
        BottomSpacer()
    }
}

package org.futo.inputmethod.latin.uix.programmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import org.futo.inputmethod.latin.uix.LocalKeyboardScheme
import kotlin.math.max

/** Shared drawing for both the IME and the two settings previews. */
@Composable
fun ProgrammerKeyFace(
    key: ProgrammerKey,
    style: Int,
    active: Boolean,
    pressed: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalKeyboardScheme.current
    val shape = RoundedCornerShape(6.dp)
    val background = when {
        pressed || active -> colors.keyboardContainerPressed
        style == KEYCAPS -> colors.keyboardContainer
        else -> Color.Transparent
    }
    val foreground = if(pressed || active) colors.onKeyboardContainerPressed else colors.onKeyboardContainer

    Surface(
        modifier = modifier,
        shape = shape,
        color = background,
        border = if(selected) BorderStroke(2.dp, colors.primary) else null,
        shadowElevation = if(style == KEYCAPS) 1.dp else 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if(key.icon.isNotBlank()) {
                    Text(
                        key.icon,
                        color = foreground,
                        fontSize = if(key.label.isBlank()) 20.sp else 17.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
                if(key.label.isNotBlank() || key.icon.isBlank()) {
                    Text(
                        key.displayLabel(),
                        color = foreground,
                        fontSize = if(key.icon.isNotBlank()) 10.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Stable 48dp key geometry, same painter and width distribution in keyboard and settings.
 * Clicking the surrounding space in the editor clears its selected key.
 */
@Composable
fun ProgrammerRow(
    keys: List<ProgrammerKey>,
    style: Int,
    modifiers: ProgrammerModifiers? = null,
    onKey: ((ProgrammerKey) -> Unit)? = null,
    selectedId: String? = null,
    onSelect: ((String?) -> Unit)? = null,
    onResize: ((Int, Boolean, Float) -> Unit)? = null,
    onModifierDown: ((ProgrammerKeyType) -> Unit)? = null,
    onModifierRelease: ((ProgrammerKeyType) -> Unit)? = null,
    onModifierTap: ((ProgrammerKeyType) -> Unit)? = null,
    onModifierLong: ((ProgrammerKeyType) -> Unit)? = null,
    onModifierLongRelease: ((ProgrammerKeyType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var rowWidth by remember { mutableIntStateOf(1) }
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(LocalKeyboardScheme.current.keyboardSurface)
            .onSizeChanged { rowWidth = max(1, it.width) }
            .clickable(enabled = onSelect != null) { onSelect?.invoke(null) }
    ) {
        keys.forEachIndexed { index, key ->
            val isSelected = selectedId == key.id && onSelect != null
            var pressed by remember(key.id) { mutableStateOf(false) }
            var longPressed by remember(key.id) { mutableStateOf(false) }
            val isEditing = onSelect != null
            val isModifier = isModifier(key.type)
            val density = LocalDensity.current
            val keyModifier = Modifier.weight(key.width.coerceAtLeast(0.25f))
                .fillMaxHeight()
                .padding(horizontal = 2.dp, vertical = 3.dp)
                .let { base ->
                    if(isEditing) {
                        base.clickable { onSelect?.invoke(key.id) }
                    } else {
                        base.pointerInput(key.id, onKey, onModifierTap, onModifierLong, onModifierLongRelease) {
                            detectTapGestures(
                                onPress = {
                                    pressed = true
                                    longPressed = false
                                    if(isModifier) onModifierDown?.invoke(key.type)
                                    val released = tryAwaitRelease()
                                    pressed = false
                                    if(isModifier) {
                                        onModifierRelease?.invoke(key.type)
                                        if(released && longPressed) onModifierLongRelease?.invoke(key.type)
                                    }
                                },
                                onTap = {
                                    if(isModifier) onModifierTap?.invoke(key.type)
                                    else onKey?.invoke(key)
                                },
                                onLongPress = {
                                    longPressed = true
                                    if(isModifier) onModifierLong?.invoke(key.type)
                                }
                            )
                        }
                    }
                }

            Box(keyModifier) {
                ProgrammerKeyFace(
                    key = key,
                    style = style,
                    active = modifiers?.active(key.type) == true,
                    pressed = pressed,
                    selected = isSelected,
                    modifier = Modifier.fillMaxSize()
                )

                if(pressed && style == KEYCAPS && !isEditing) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = IntOffset(0, with(density) { (-54).dp.roundToPx() })
                    ) {
                        ProgrammerKeyFace(
                            key = key,
                            style = KEYCAPS,
                            active = true,
                            pressed = true,
                            selected = false,
                            modifier = Modifier.width(54.dp).height(48.dp)
                        )
                    }
                }

                if(isSelected && onResize != null) {
                    for(rightEdge in listOf(false, true)) {
                        Box(
                            Modifier
                                .align(if(rightEdge) Alignment.CenterEnd else Alignment.CenterStart)
                                .width(9.dp)
                                .fillMaxHeight()
                                .background(
                                    LocalKeyboardScheme.current.primary.copy(alpha = 0.75f),
                                    RoundedCornerShape(4.dp)
                                )
                                .pointerInput(index, rightEdge, rowWidth) {
                                    detectHorizontalDragGestures { change, drag ->
                                        change.consume()
                                        onResize(index, rightEdge, drag / rowWidth.toFloat())
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

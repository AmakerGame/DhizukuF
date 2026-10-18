package com.EdS.DhizukuF.ui.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Visible focus frame for D-pad / keyboard navigation (Android TV, game pads, keyboards).
 * Put it BEFORE the focusable part of the modifier chain (before clickable / toggleable).
 */
@Composable
fun Modifier.tvFocusBorder(shape: RoundedCornerShape = RoundedCornerShape(16.dp)): Modifier {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    return this
        .onFocusChanged { focused = it.hasFocus }
        .then(if (focused) Modifier.border(3.dp, color, shape) else Modifier)
}

/**
 * A row that toggles a value. It is a plain clickable (a normal D-pad focus target, the same as the
 * other rows of the app) that reports its checked state to accessibility services.
 */
fun Modifier.tvToggle(checked: Boolean, role: Role, onValueChange: (Boolean) -> Unit): Modifier =
    this.clickable(role = role, onClick = { onValueChange(!checked) })
        .semantics { toggleableState = ToggleableState(checked) }

/**
 * A settings row with a title, a description and a switch. The whole row is one focus target, so it
 * can be toggled with the D-pad centre / Enter key as well as by touch.
 */
@Composable
fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    titleColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusBorder(RoundedCornerShape(12.dp))
            .tvToggle(checked = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun Shape.frame(): RoundedCornerShape = this as? RoundedCornerShape ?: RoundedCornerShape(50)

/** TextButton with a visible focus frame for D-pad navigation. */
@Composable
fun TvTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.tvFocusBorder(RoundedCornerShape(50)),
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/** Button with a visible focus frame for D-pad navigation. */
@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.tvFocusBorder(shape.frame()),
        enabled = enabled,
        shape = shape,
        colors = colors,
        content = content
    )
}

/** OutlinedButton with a visible focus frame for D-pad navigation. */
@Composable
fun TvOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.tvFocusBorder(shape.frame()),
        enabled = enabled,
        shape = shape,
        colors = colors,
        content = content
    )
}

/** IconButton with a visible focus frame for D-pad navigation (app bar buttons, row actions). */
@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.tvFocusBorder(RoundedCornerShape(50)),
        enabled = enabled,
        content = content
    )
}

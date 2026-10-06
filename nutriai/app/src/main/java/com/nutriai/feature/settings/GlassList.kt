package com.nutriai.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.components.glass

/** Grupo de filas al estilo de los ajustes de iOS, sobre cristal. */
@Composable
fun GlassGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        if (title != null) {
            Text(
                title.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
        }
        Column(Modifier.fillMaxWidth().glass(MaterialTheme.shapes.medium), content = content)
    }
}

@Composable
fun GroupRow(
    title: String,
    value: String? = null,
    emoji: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) { Text(emoji); Spacer(Modifier.width(12.dp)) }
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            trailing?.invoke()
            if (onClick != null && trailing == null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showDivider) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }
}

@Composable
fun GroupSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit, emoji: String? = null, showDivider: Boolean = true) {
    GroupRow(title, emoji = emoji, showDivider = showDivider, trailing = {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f), uncheckedBorderColor = Color.Transparent),
        )
    })
}

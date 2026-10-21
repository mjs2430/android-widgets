package com.sassoapps.widgets.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A titled group of settings. */
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(top = 4.dp), content = content)
    }
    HorizontalDivider()
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** An integer slider that shows its current value, e.g. "Time size · 56". */
@Composable
fun SliderRow(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    valueLabel: (Int) -> String = { it.toString() },
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$title · ${valueLabel(value)}", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

/** Pick one of a few options, shown as chips. */
@Composable
fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

/** Pick a color from a palette of swatches. Colors are ARGB ints. */
@Composable
fun ColorRow(title: String, color: Int, onColorChange: (Int) -> Unit, swatches: List<Int> = DefaultSwatches) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            swatches.forEach { swatch ->
                val selected = swatch == color
                Row(
                    Modifier
                        .size(36.dp)
                        .border(
                            BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            CircleShape,
                        )
                        .padding(4.dp)
                        .background(Color(swatch), CircleShape)
                        .clickable { onColorChange(swatch) },
                ) {}
            }
        }
    }
}

val DefaultSwatches: List<Int> = listOf(
    0xFFFFFFFF, 0xFFE0E0E0, 0xFF9E9E9E, 0xFF424242, 0xFF000000,
    0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFE57373, 0xFFF06292,
    0xFFBA68C8, 0xFF9575CD, 0xFF7986CB, 0xFF3D5AFE, 0xFF64B5F6,
    0xFF4DD0E1, 0xFF4DB6AC, 0xFF81C784, 0xFF1E2A44, 0xFF263238,
).map { it.toInt() }

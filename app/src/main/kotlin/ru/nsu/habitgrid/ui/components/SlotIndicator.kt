package ru.nsu.habitgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nsu.habitgrid.R

@Composable
fun SlotIndicator(slot: Int, modifier: Modifier = Modifier, cellSize: Dp = 12.dp) {
    val description = stringResource(R.string.habit_slot, slot + 1)
    Column(
        modifier = modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) { column ->
                    val color = if (row * 2 + column == slot) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        Modifier
                            .size(cellSize)
                            .background(color, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

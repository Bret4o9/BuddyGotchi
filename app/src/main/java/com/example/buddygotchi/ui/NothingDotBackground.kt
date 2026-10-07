package com.example.buddygotchi.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.nothingDotGrid(
    dotColor: Color = Color.White.copy(alpha = 0.09f),
    dotRadius: Dp = 1.2.dp,
    gridSpacing: Dp = 22.dp
): Modifier = this.drawBehind {
    val spacingPx = gridSpacing.toPx()
    val radiusPx = dotRadius.toPx()
    val width = size.width
    val height = size.height

    var x = spacingPx / 2
    while (x < width) {
        var y = spacingPx / 2
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(x, y)
            )
            y += spacingPx
        }
        x += spacingPx
    }
}

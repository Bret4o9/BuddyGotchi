package com.example.buddygotchi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.buddygotchi.DashboardGridDefaults
import com.example.buddygotchi.DashboardWidgetId
import com.example.buddygotchi.DashboardWidgetItem
import com.example.buddygotchi.WidgetSize
import com.example.buddygotchi.ui.theme.NothingRed
import kotlin.math.max

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

/**
 * True modular Nothing OS grid background for the Dashboard.
 * 
 * 1. Background dots are aligned precisely with the 4-column modular grid slots and row units.
 * 2. In Edit Mode, connecting lines light up between the dots, revealing the blueprint grid.
 * 3. During widget resize, the target grid cells light up in pulsing Nothing Red.
 */
@Composable
fun DashboardGridBackground(
    isEditMode: Boolean,
    packedRows: List<List<DashboardWidgetItem>>,
    activeResizeWidgetId: DashboardWidgetId?,
    activeResizePreviewSize: WidgetSize?,
    scrollY: Float,
    modifier: Modifier = Modifier
) {
    val editModeProgress by animateFloatAsState(
        targetValue = if (isEditMode) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "editModeProgress"
    )

    val pulseAlpha = remember { Animatable(0.08f) }
    LaunchedEffect(activeResizeWidgetId) {
        if (activeResizeWidgetId != null) {
            pulseAlpha.snapTo(0.06f)
            pulseAlpha.animateTo(
                targetValue = 0.16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(850, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0.08f)
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val horizontalPaddingPx = DashboardGridDefaults.HORIZONTAL_PADDING.toPx()
        val gapPx = DashboardGridDefaults.GAP.toPx()
        val unitHPx = DashboardGridDefaults.UNIT_HEIGHT.toPx()
        val topPaddingPx = 2.dp.toPx()

        val contentWPx = canvasWidth - horizontalPaddingPx * 2
        val unitWPx = (contentWPx - gapPx * (DashboardGridDefaults.COLUMNS - 1)) / DashboardGridDefaults.COLUMNS

        fun getColLeft(col: Int): Float = horizontalPaddingPx + col * (unitWPx + gapPx)
        fun getColRight(col: Int): Float = getColLeft(col) + unitWPx
        fun getRowTop(row: Int): Float = topPaddingPx + row * (unitHPx + gapPx) - scrollY
        fun getRowBottom(row: Int): Float = getRowTop(row) + unitHPx

        // Calculate widget bounds and target illumination rect
        var currentRow = 0
        var resizingRect: Rect? = null

        for (row in packedRows) {
            var currentCol = 0
            val maxSpan = row.maxOfOrNull { it.size.rows } ?: 1
            for (item in row) {
                if (item.widgetId == activeResizeWidgetId && activeResizePreviewSize != null) {
                    val previewCols = activeResizePreviewSize.cols
                    val previewRows = activeResizePreviewSize.rows
                    val startCol = if (currentCol + previewCols > DashboardGridDefaults.COLUMNS) {
                        (DashboardGridDefaults.COLUMNS - previewCols).coerceAtLeast(0)
                    } else {
                        currentCol
                    }
                    val left = getColLeft(startCol)
                    val top = getRowTop(currentRow)
                    val rectWidth = previewCols * unitWPx + (previewCols - 1) * gapPx
                    val rectHeight = previewRows * unitHPx + (previewRows - 1) * gapPx
                    resizingRect = Rect(left, top, left + rectWidth, top + rectHeight)
                }
                currentCol += item.size.cols
            }
            currentRow += maxSpan
        }

        val totalGridRows = max(currentRow + 3, 8)

        // 1. Draw connecting blueprint grid in Edit Mode
        if (editModeProgress > 0.01f) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
            val strokeWidth = 1.dp.toPx()
            val slotCornerRadius = CornerRadius(16.dp.toPx())

            for (r in 0 until totalGridRows) {
                val rTop = getRowTop(r)
                if (rTop > canvasHeight + 20 || rTop + unitHPx < -20) continue

                for (c in 0 until DashboardGridDefaults.COLUMNS) {
                    val cLeft = getColLeft(c)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f * editModeProgress),
                        topLeft = Offset(cLeft, rTop),
                        size = Size(unitWPx, unitHPx),
                        cornerRadius = slotCornerRadius,
                        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                    )
                }
            }
        }

        // 2. Draw Illuminated Target Footprint when resizing
        if (resizingRect != null) {
            val rect = resizingRect
            val targetCorner = CornerRadius(22.dp.toPx())

            // Glowing red fill
            drawRoundRect(
                color = NothingRed.copy(alpha = pulseAlpha.value),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = targetCorner
            )

            // High-visibility glowing Nothing Red perimeter border
            drawRoundRect(
                color = NothingRed,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = targetCorner,
                style = Stroke(width = 2.dp.toPx())
            )

            // Glowing corner vertices
            val glowRadiusPx = 3.5.dp.toPx()
            val corners = listOf(
                Offset(rect.left, rect.top),
                Offset(rect.right, rect.top),
                Offset(rect.left, rect.bottom),
                Offset(rect.right, rect.bottom)
            )
            for (pt in corners) {
                drawCircle(
                    color = NothingRed.copy(alpha = 0.35f),
                    radius = glowRadiusPx * 2.2f,
                    center = pt
                )
                drawCircle(
                    color = NothingRed,
                    radius = glowRadiusPx,
                    center = pt
                )
            }
        }

        // 3. Draw Background Dots aligned with the modular grid
        val normalDotRadius = 1.2.dp.toPx()
        val illuminatedDotRadius = 2.0.dp.toPx()
        val baseAlpha = 0.08f + 0.12f * editModeProgress

        // Collect X coordinates across the 4 columns (strictly on the grid cells, no gap or margin dots)
        val xCoords = mutableListOf<Float>()
        for (c in 0 until DashboardGridDefaults.COLUMNS) {
            val left = getColLeft(c)
            xCoords.add(left)
            xCoords.add(left + unitWPx * (1f / 3f))
            xCoords.add(left + unitWPx * (2f / 3f))
            val right = getColRight(c)
            xCoords.add(right)
        }

        // For each row, collect Y coordinates (strictly on the grid cells, no gap dots)
        for (r in 0 until totalGridRows) {
            val top = getRowTop(r)
            if (top > canvasHeight + 20 || top + unitHPx + gapPx < -20) continue

            val yCoords = listOf(
                top,
                top + unitHPx * (1f / 3f),
                top + unitHPx * (2f / 3f),
                getRowBottom(r)
            )

            for (y in yCoords) {
                if (y < -10 || y > canvasHeight + 10) continue

                for (x in xCoords) {
                    val isInsideTarget = resizingRect != null &&
                            x >= (resizingRect.left - 2f) && x <= (resizingRect.right + 2f) &&
                            y >= (resizingRect.top - 2f) && y <= (resizingRect.bottom + 2f)

                    if (isInsideTarget) {
                        drawCircle(
                            color = NothingRed.copy(alpha = 0.95f),
                            radius = illuminatedDotRadius,
                            center = Offset(x, y)
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = baseAlpha),
                            radius = normalDotRadius,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

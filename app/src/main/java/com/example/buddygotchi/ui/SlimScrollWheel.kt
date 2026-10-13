package com.example.buddygotchi.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.buddygotchi.ui.theme.NothingRed
import com.example.buddygotchi.ui.theme.NothingWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A slim Nothing OS mechanical scroll wheel indicator.
 * Displays on the right side of the screen when scrolling.
 * Features:
 * - Slim translucent vertical track with knurled micro-ticks (scroll wheel texture).
 * - A glowing Nothing Red dot indicator that tracks the exact page position.
 * - Interactive: dragging the wheel/dot smoothly scrolls the page.
 * - Fades in upon scroll/interaction and gently fades out when idle.
 */
@Composable
fun SlimScrollWheel(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    width: Dp = 16.dp,
    dotRadius: Dp = 4.dp,
    trackWidth: Dp = 2.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val canScroll = scrollState.maxValue > 0
    val scrollFraction = if (canScroll) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isDraggingWheel by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState, isDraggingWheel) {
        if (isDraggingWheel) {
            isVisible = true
            return@LaunchedEffect
        }
        var isFirstEmission = true
        snapshotFlow { scrollState.value }
            .collectLatest {
                if (isFirstEmission) {
                    isFirstEmission = false
                } else {
                    isVisible = true
                    delay(1200L)
                    isVisible = false
                }
            }
    }

    val alpha by animateFloatAsState(
        targetValue = if ((isVisible || isDraggingWheel) && canScroll) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "scrollWheelAlpha"
    )

    if (alpha <= 0.01f && !isVisible) {
        return
    }

    val dotRadiusPx = with(density) { dotRadius.toPx() }
    val trackWidthPx = with(density) { trackWidth.toPx() }
    val tickWidthPx = with(density) { 4.5.dp.toPx() }
    val tickIntervalPx = with(density) { 14.dp.toPx() }.coerceAtLeast(10f)

    var measuredHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .pointerInput(canScroll) {
                if (canScroll) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDraggingWheel = true
                            if (measuredHeightPx > 0f) {
                                val marginY = dotRadiusPx + 4f
                                val travel = (measuredHeightPx - marginY * 2).coerceAtLeast(1f)
                                val fraction = ((offset.y - marginY) / travel).coerceIn(0f, 1f)
                                val targetScroll = (fraction * scrollState.maxValue).roundToInt()
                                coroutineScope.launch { scrollState.scrollTo(targetScroll) }
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            if (measuredHeightPx > 0f) {
                                val marginY = dotRadiusPx + 4f
                                val travel = (measuredHeightPx - marginY * 2).coerceAtLeast(1f)
                                val fraction = ((change.position.y - marginY) / travel).coerceIn(0f, 1f)
                                val targetScroll = (fraction * scrollState.maxValue).roundToInt()
                                coroutineScope.launch { scrollState.scrollTo(targetScroll) }
                            }
                        },
                        onDragEnd = { isDraggingWheel = false },
                        onDragCancel = { isDraggingWheel = false }
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (alpha <= 0f || size.height <= 0f) return@Canvas
            measuredHeightPx = size.height

            val availableHeightPx = size.height
            val marginYPx = dotRadiusPx + 4f
            val travelDistancePx = (availableHeightPx - marginYPx * 2).coerceAtLeast(0f)
            if (travelDistancePx <= 0f) return@Canvas

            val dotCenterY = marginYPx + travelDistancePx * scrollFraction
            val centerX = size.width / 2

            // 1. Slim Vertical Track (translucent capsule)
            val trackLeft = centerX - trackWidthPx / 2
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f * alpha),
                topLeft = Offset(trackLeft, marginYPx),
                size = Size(trackWidthPx, travelDistancePx),
                cornerRadius = CornerRadius(trackWidthPx / 2, trackWidthPx / 2)
            )

            // 2. Tactile Scroll Wheel Ridges / Micro-Ticks (strictly bounded for-loop)
            val tickCount = (travelDistancePx / tickIntervalPx).toInt().coerceIn(0, 60)
            val proximityRangePx = 40.dp.toPx()
            for (i in 0..tickCount) {
                val tickY = marginYPx + (i * tickIntervalPx)
                if (tickY > marginYPx + travelDistancePx) break

                val distToDot = kotlin.math.abs(tickY - dotCenterY)
                val proximityAlpha = (1f - (distToDot / proximityRangePx)).coerceIn(0f, 1f)
                val tickAlpha = (0.15f + proximityAlpha * 0.25f) * alpha

                drawLine(
                    color = Color.White.copy(alpha = tickAlpha),
                    start = Offset(centerX - tickWidthPx / 2, tickY),
                    end = Offset(centerX + tickWidthPx / 2, tickY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Dot Indicator (indicates position on page)
            // Outer glow
            drawCircle(
                color = NothingRed.copy(alpha = 0.28f * alpha),
                radius = dotRadiusPx * 1.8f,
                center = Offset(centerX, dotCenterY)
            )
            // White ring
            drawCircle(
                color = NothingWhite.copy(alpha = 0.95f * alpha),
                radius = dotRadiusPx,
                center = Offset(centerX, dotCenterY)
            )
            // Signature Nothing Red Core
            drawCircle(
                color = NothingRed.copy(alpha = 1f * alpha),
                radius = dotRadiusPx * 0.55f,
                center = Offset(centerX, dotCenterY)
            )
        }
    }
}

package com.example.buddygotchi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.theme.NothingRed
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardWidgetContainer(
    item: DashboardWidgetItem,
    isEditMode: Boolean,
    onEnterEditMode: () -> Unit,
    onResize: (WidgetSize) -> Unit,
    onDragReorderStart: () -> Unit,
    onDragReorderMove: (dragY: Float) -> Unit,
    onDragReorderEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (liveSize: WidgetSize) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentItem by rememberUpdatedState(item)
    val currentOnResize by rememberUpdatedState(onResize)
    val currentDensity by rememberUpdatedState(density)

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun triggerSnap() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val isClock = item.widgetId == DashboardWidgetId.CLOCK
    val isResizable = !isClock

    // Measured dimensions of the container card for accurate scale calculations
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }

    // Live dragging offsets for the 4 resize dots
    var activeDot by remember { mutableStateOf<ResizeDotType?>(null) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    // Live preview size calculated during drag
    var livePreviewSize by remember(item.size) { mutableStateOf(item.size) }

    // Spring animatables for smooth release transition
    val animStretchX = remember { Animatable(1f) }
    val animStretchY = remember { Animatable(1f) }
    var isSettling by remember { mutableStateOf(false) }

    // TransformOrigin anchors opposite edge to ensure true directional expansion
    val activeOrigin = when (activeDot) {
        ResizeDotType.RIGHT -> TransformOrigin(0f, 0.5f)  // Left edge anchored -> expands right
        ResizeDotType.LEFT -> TransformOrigin(1f, 0.5f)   // Right edge anchored -> expands left
        ResizeDotType.BOTTOM -> TransformOrigin(0.5f, 0f) // Top edge anchored -> expands down
        ResizeDotType.TOP -> TransformOrigin(0.5f, 1f)    // Bottom edge anchored -> expands UP
        null -> TransformOrigin.Center
    }
    var lastActiveOrigin by remember { mutableStateOf(TransformOrigin.Center) }
    if (activeDot != null) {
        lastActiveOrigin = activeOrigin
    }
    val currentTransformOrigin = if (activeDot != null || isSettling) lastActiveOrigin else TransformOrigin.Center

    val effectiveW = if (cardWidthPx > 0f) cardWidthPx else with(density) { 160.dp.toPx() }
    val effectiveH = if (cardHeightPx > 0f) cardHeightPx else with(density) { 120.dp.toPx() }

    val targetScaleX = when (activeDot) {
        ResizeDotType.RIGHT -> (1f + (dragDeltaX / effectiveW)).coerceIn(0.45f, 2.6f)
        ResizeDotType.LEFT -> (1f - (dragDeltaX / effectiveW)).coerceIn(0.45f, 2.6f)
        else -> 1f
    }

    val targetScaleY = when (activeDot) {
        ResizeDotType.BOTTOM -> (1f + (dragDeltaY / effectiveH)).coerceIn(0.45f, 2.6f)
        ResizeDotType.TOP -> (1f - (dragDeltaY / effectiveH)).coerceIn(0.45f, 2.6f)
        else -> 1f
    }

    val currentStretchX = if (isSettling) animStretchX.value else targetScaleX
    val currentStretchY = if (isSettling) animStretchY.value else targetScaleY

    val cornerRadius = if (item.size.cols == 1) 20.dp else 24.dp

    Box(
        modifier = modifier
            .zIndex(if (activeDot != null || isSettling) 80f else 1f)
            .onSizeChanged { size ->
                cardWidthPx = size.width.toFloat()
                cardHeightPx = size.height.toFloat()
            }
            // 1. Corner Tap-and-Hold detector to enter edit mode (active when not in edit mode)
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    val cornerPx = 64.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        val pos = down.position
                        val w = size.width
                        val h = size.height

                        val isCorner = (pos.x <= cornerPx || pos.x >= w - cornerPx) &&
                                (pos.y <= cornerPx || pos.y >= h - cornerPx)

                        if (isCorner) {
                            val timeoutMs = 320L
                            var isHeld = false
                            try {
                                withTimeout(timeoutMs) {
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) return@withTimeout
                                        val dist = (change.position - pos).getDistance()
                                        if (dist > 24.dp.toPx()) return@withTimeout
                                    }
                                }
                            } catch (_: TimeoutCancellationException) {
                                isHeld = true
                            } catch (_: PointerEventTimeoutCancellationException) {
                                isHeld = true
                            }

                            if (isHeld) {
                                down.consume()
                                triggerSnap()
                                onEnterEditMode()
                            }
                        }
                    }
                }
            }
            // 2. In edit mode: Card body dragging for reordering widgets (disabled while resizing)
            .pointerInput(isEditMode, activeDot, isSettling) {
                if (isEditMode && activeDot == null && !isSettling) {
                    detectDragGestures(
                        onDragStart = {
                            triggerTick()
                            onDragReorderStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragReorderMove(dragAmount.y)
                        },
                        onDragEnd = {
                            triggerTick()
                            onDragReorderEnd()
                        },
                        onDragCancel = {
                            onDragReorderEnd()
                        }
                    )
                }
            }
            .graphicsLayer {
                scaleX = currentStretchX
                scaleY = currentStretchY
                this.transformOrigin = currentTransformOrigin
                clip = false
            }
            .then(
                if (isEditMode) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (activeDot != null || isSettling) NothingRed else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Main Widget Content (counter-scaled so content NEVER distorts, locked to item.size during drag)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f / currentStretchX
                    scaleY = 1f / currentStretchY
                    this.transformOrigin = currentTransformOrigin
                }
        ) {
            content(item.size)
        }

        // Floating Snap Target Pill Badge (visible when actively dragging or settling)
        if (activeDot != null || isSettling) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(100f)
                    .graphicsLayer {
                        scaleX = 1f / currentStretchX
                        scaleY = 1f / currentStretchY
                        this.transformOrigin = currentTransformOrigin
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, NothingRed),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Text(
                            text = "${livePreviewSize.cols}×${livePreviewSize.rows}" + when (livePreviewSize) {
                                WidgetSize.CUBE -> " CUBE"
                                WidgetSize.HALF -> " HALF"
                                WidgetSize.SLIM -> " SLIM"
                                WidgetSize.WIDE -> " WIDE"
                                WidgetSize.MAX -> " MAX"
                                WidgetSize.TALL -> " TALL"
                                else -> ""
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4 Resize Dots (Visible strictly in Edit Mode, except for the Clock widget)
        if (isEditMode && isResizable) {
            // Helper to handle horizontal drag on Left/Right dots
            fun handleHorizontalDrag(dotType: ResizeDotType, delta: Float) {
                dragDeltaX += delta
                val pullDp = with(currentDensity) {
                    (if (dotType == ResizeDotType.RIGHT) dragDeltaX else -dragDeltaX).toDp().value
                }
                val startCols = currentItem.size.cols
                val colDelta = when {
                    pullDp > 75f -> 3
                    pullDp > 45f -> 2
                    pullDp > 18f -> 1
                    pullDp < -75f -> -3
                    pullDp < -45f -> -2
                    pullDp < -18f -> -1
                    else -> 0
                }
                val targetCols = (startCols + colDelta).coerceIn(1, 4)
                val newPreview = WidgetSize.from(targetCols, currentItem.size.rows)
                if (newPreview != livePreviewSize) {
                    livePreviewSize = newPreview
                    triggerTick()
                }
            }

            // Helper to handle vertical drag on Top/Bottom dots
            fun handleVerticalDrag(dotType: ResizeDotType, delta: Float) {
                dragDeltaY += delta
                val pullDp = with(currentDensity) {
                    (if (dotType == ResizeDotType.BOTTOM) dragDeltaY else -dragDeltaY).toDp().value
                }
                val startRows = currentItem.size.rows
                val rowDelta = when {
                    pullDp > 75f -> 3
                    pullDp > 45f -> 2
                    pullDp > 18f -> 1
                    pullDp < -75f -> -3
                    pullDp < -45f -> -2
                    pullDp < -18f -> -1
                    else -> 0
                }
                val targetRows = (startRows + rowDelta).coerceIn(1, 4)
                val newPreview = WidgetSize.from(currentItem.size.cols, targetRows)
                if (newPreview != livePreviewSize) {
                    livePreviewSize = newPreview
                    triggerTick()
                }
            }

            fun finishDrag() {
                if (activeDot == null) return
                val finalSize = livePreviewSize
                val oldSize = currentItem.size

                coroutineScope.launch {
                    isSettling = true

                    val (oldW, oldH) = getEstimatedSizeDp(oldSize)
                    val (newW, newH) = getEstimatedSizeDp(finalSize)
                    val startScaleX = (targetScaleX * (oldW / newW)).coerceIn(0.4f, 2.5f)
                    val startScaleY = (targetScaleY * (oldH / newH)).coerceIn(0.4f, 2.5f)

                    animStretchX.snapTo(startScaleX)
                    animStretchY.snapTo(startScaleY)

                    if (finalSize != oldSize) {
                        triggerSnap()
                        currentOnResize(finalSize)
                    }

                    launch {
                        animStretchX.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        animStretchY.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }.join()

                    activeDot = null
                    dragDeltaX = 0f
                    dragDeltaY = 0f
                    isSettling = false
                }
            }

            // 1. Top Dot
            ResizeDot(
                alignment = Alignment.TopCenter,
                isDragging = activeDot == ResizeDotType.TOP,
                isVisible = activeDot == null || activeDot == ResizeDotType.TOP,
                counterScaleX = 1f / currentStretchX,
                counterScaleY = 1f / currentStretchY,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .pointerInput(item.widgetId, item.size) {
                        detectDragGestures(
                            onDragStart = {
                                activeDot = ResizeDotType.TOP
                                dragDeltaY = 0f
                                livePreviewSize = currentItem.size
                                triggerTick()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                handleVerticalDrag(ResizeDotType.TOP, dragAmount.y)
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    }
            )

            // 2. Bottom Dot
            ResizeDot(
                alignment = Alignment.BottomCenter,
                isDragging = activeDot == ResizeDotType.BOTTOM,
                isVisible = activeDot == null || activeDot == ResizeDotType.BOTTOM,
                counterScaleX = 1f / currentStretchX,
                counterScaleY = 1f / currentStretchY,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .pointerInput(item.widgetId, item.size) {
                        detectDragGestures(
                            onDragStart = {
                                activeDot = ResizeDotType.BOTTOM
                                dragDeltaY = 0f
                                livePreviewSize = currentItem.size
                                triggerTick()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                handleVerticalDrag(ResizeDotType.BOTTOM, dragAmount.y)
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    }
            )

            // 3. Left Dot
            ResizeDot(
                alignment = Alignment.CenterStart,
                isDragging = activeDot == ResizeDotType.LEFT,
                isVisible = activeDot == null || activeDot == ResizeDotType.LEFT,
                counterScaleX = 1f / currentStretchX,
                counterScaleY = 1f / currentStretchY,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-4).dp)
                    .pointerInput(item.widgetId, item.size) {
                        detectDragGestures(
                            onDragStart = {
                                activeDot = ResizeDotType.LEFT
                                dragDeltaX = 0f
                                livePreviewSize = currentItem.size
                                triggerTick()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                handleHorizontalDrag(ResizeDotType.LEFT, dragAmount.x)
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    }
            )

            // 4. Right Dot
            ResizeDot(
                alignment = Alignment.CenterEnd,
                isDragging = activeDot == ResizeDotType.RIGHT,
                isVisible = activeDot == null || activeDot == ResizeDotType.RIGHT,
                counterScaleX = 1f / currentStretchX,
                counterScaleY = 1f / currentStretchY,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 4.dp)
                    .pointerInput(item.widgetId, item.size) {
                        detectDragGestures(
                            onDragStart = {
                                activeDot = ResizeDotType.RIGHT
                                dragDeltaX = 0f
                                livePreviewSize = currentItem.size
                                triggerTick()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                handleHorizontalDrag(ResizeDotType.RIGHT, dragAmount.x)
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    }
            )
        }
    }
}

enum class ResizeDotType {
    TOP, BOTTOM, LEFT, RIGHT
}

@Composable
private fun ResizeDot(
    alignment: Alignment,
    isDragging: Boolean,
    isVisible: Boolean,
    counterScaleX: Float = 1f,
    counterScaleY: Float = 1f,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dotAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dotScale"
    )

    if (alpha <= 0.01f && !isVisible) return

    Box(
        modifier = modifier
            .size(44.dp) // comfortable touch target
            .zIndex(50f)
            .graphicsLayer {
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow/ring
        Box(
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    scaleX = scale * counterScaleX
                    scaleY = scale * counterScaleY
                }
                .clip(CircleShape)
                .background(if (isDragging) NothingRed.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    color = if (isDragging) NothingRed else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner Nothing dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isDragging) NothingRed else MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

private fun getEstimatedSizeDp(size: WidgetSize): Pair<Float, Float> {
    val w = when (size.cols) {
        1 -> 88f
        2 -> 170f
        3 -> 260f
        else -> 360f
    }
    val h = when (size.rows) {
        1 -> if (size.cols == 2) 110f else 88f
        2 -> 180f
        3 -> 276f
        else -> 372f
    }
    return Pair(w, h)
}


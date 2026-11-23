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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import com.example.buddygotchi.ui.theme.NothingBorder
import com.example.buddygotchi.ui.theme.NothingRed
import com.example.buddygotchi.ui.theme.NothingWhite
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
    onDragReorderEnd: (isDroppedOnShelf: Boolean) -> Unit,
    onResizePreview: ((WidgetSize?, ResizeDotType?) -> Unit)? = null,
    onStashToShelf: (() -> Unit)? = null,
    isPickedUp: Boolean = false,
    isHoveringShelf: Boolean = false,
    onCheckOverShelf: ((pointerInRoot: Offset?, cardTopInRoot: Float?) -> Boolean)? = null,
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

    // Animatables for the shake/anticipation feedback during long-press
    val shakeRotationZ = remember { Animatable(0f) }
    val shakeScale = remember { Animatable(1f) }

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

    val cornerRadius = if (item.size.cols == 1) 20.dp else 24.dp

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isOverShelf by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .zIndex(if (activeDot != null || isSettling) 80f else 1f)
            .onGloballyPositioned { coords ->
                containerCoords = coords
            }
            .onSizeChanged { size ->
                cardWidthPx = size.width.toFloat()
                cardHeightPx = size.height.toFloat()
            }
            // 1. Full-card Tap-and-Hold detector to enter edit mode (active anywhere on the card when not in edit mode)
            // Includes a mechanical anticipation shake animation and extended hold duration to avoid accidental triggers.
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        val pos = down.position

                        val holdDurationMs = 600L
                        val shakeStartDelayMs = 260L

                        var isHeld = false
                        val shakeJob = coroutineScope.launch {
                            kotlinx.coroutines.delay(shakeStartDelayMs)
                            shakeScale.animateTo(
                                targetValue = 0.98f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
                            )
                            val shakeAngles = listOf(1.4f, -1.4f, 1.0f, -1.0f, 0.6f, -0.6f, 0f)
                            for (angle in shakeAngles) {
                                shakeRotationZ.snapTo(angle)
                                kotlinx.coroutines.delay(45L)
                            }
                        }

                        try {
                            withTimeout(holdDurationMs) {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) return@withTimeout
                                    val dist = (change.position - pos).getDistance()
                                    if (dist > 28.dp.toPx()) return@withTimeout
                                }
                            }
                        } catch (_: TimeoutCancellationException) {
                            isHeld = true
                        } catch (_: PointerEventTimeoutCancellationException) {
                            isHeld = true
                        } finally {
                            shakeJob.cancel()
                            coroutineScope.launch {
                                shakeRotationZ.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                shakeScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }

                        if (isHeld) {
                            down.consume()
                            triggerSnap()
                            onEnterEditMode()
                        }
                    }
                }
            }
            // 2. In edit mode: Card body dragging for reordering widgets (disabled while resizing or when touching a resize handle)
            .pointerInput(isEditMode, activeDot, isSettling, isResizable) {
                if (isEditMode && activeDot == null && !isSettling) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        if (activeDot != null) return@awaitEachGesture

                        // Do not intercept touches that land on any of the 4 edge resize handles
                        if (isResizable && cardWidthPx > 0f && cardHeightPx > 0f) {
                            val handleHalfSpanPx = 42.dp.toPx()
                            val handleEdgeDepthPx = 32.dp.toPx()
                            val x = down.position.x
                            val y = down.position.y
                            val midX = cardWidthPx / 2f
                            val midY = cardHeightPx / 2f

                            val onTopHandle = y <= handleEdgeDepthPx && kotlin.math.abs(x - midX) <= handleHalfSpanPx
                            val onBottomHandle = y >= (cardHeightPx - handleEdgeDepthPx) && kotlin.math.abs(x - midX) <= handleHalfSpanPx
                            val onLeftHandle = x <= handleEdgeDepthPx && kotlin.math.abs(y - midY) <= handleHalfSpanPx
                            val onRightHandle = x >= (cardWidthPx - handleEdgeDepthPx) && kotlin.math.abs(y - midY) <= handleHalfSpanPx

                            if (onTopHandle || onBottomHandle || onLeftHandle || onRightHandle) {
                                return@awaitEachGesture
                            }
                        }

                        val startRootPos = containerCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                        var lastRootY = startRootPos?.y ?: down.position.y
                        val startY = lastRootY
                        var isDragging = false
                        val dragSlopPx = 8.dp.toPx()

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            if (activeDot != null) break
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                if (isDragging) {
                                    change.consume()
                                    val dropped = isOverShelf
                                    isOverShelf = false
                                    onCheckOverShelf?.invoke(null, null)
                                    onDragReorderEnd(dropped)
                                }
                                break
                            }

                            val coords = containerCoords
                            val currentRootPos = coords?.takeIf { it.isAttached }?.localToRoot(change.position)
                            val currentRootY = currentRootPos?.y ?: change.position.y
                            val cardTop = coords?.takeIf { it.isAttached }?.localToRoot(Offset.Zero)?.y

                            val totalDist = kotlin.math.abs(currentRootY - startY)
                            if (!isDragging && totalDist > dragSlopPx) {
                                isDragging = true
                                down.consume()
                                change.consume()
                                triggerTick()
                                val over = if (item.widgetId.canRemove) {
                                    onCheckOverShelf?.invoke(currentRootPos, cardTop) ?: false
                                } else false
                                isOverShelf = over
                                onDragReorderStart()
                            }

                            if (isDragging) {
                                val deltaY = currentRootY - lastRootY
                                lastRootY = currentRootY
                                change.consume()
                                val over = if (item.widgetId.canRemove) {
                                    onCheckOverShelf?.invoke(currentRootPos, cardTop) ?: false
                                } else false
                                if (over != isOverShelf) {
                                    isOverShelf = over
                                    if (over) triggerTick()
                                }
                                onDragReorderMove(deltaY)
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = shakeScale.value
                scaleY = shakeScale.value
                rotationZ = shakeRotationZ.value
                clip = false
            }
            .then(
                if (isEditMode) {
                    Modifier.border(
                        width = if (isPickedUp) 2.dp else 1.5.dp,
                        color = if (activeDot != null) NothingRed.copy(alpha = 0.85f)
                        else if (isPickedUp && isHoveringShelf) NothingRed
                        else if (isPickedUp) NothingRed.copy(alpha = 0.85f)
                        else if (isSettling) NothingRed
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Main Widget Content (never non-uniformly stretched so 3D/2D Buddy preserves 1:1 aspect ratio)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            content(item.size)
        }

        // Floating Snap Target Pill Badge (visible when actively dragging a resize dot)
        if (activeDot != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(100f)
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

        // Floating "SHELF ↘" stash button removed: user can drag & drop widgets directly onto the shelf strip

        // Visual indicator when dragged card is hovering over the shelf drop zone
        if (isPickedUp && (isHoveringShelf || isOverShelf) && item.widgetId.canRemove) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Black.copy(alpha = 0.52f))
                    .zIndex(150f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xEE1E0000),
                    border = BorderStroke(1.dp, NothingRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Text(
                            text = "DROP TO STASH IN SHELF ↘",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = NothingRed
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
                    onResizePreview?.invoke(newPreview, dotType)
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
                    onResizePreview?.invoke(newPreview, dotType)
                }
            }

            fun finishDrag() {
                if (activeDot == null) return
                onResizePreview?.invoke(null, null)
                val finalSize = livePreviewSize
                val oldSize = currentItem.size
                activeDot = null
                dragDeltaX = 0f
                dragDeltaY = 0f
                isSettling = false

                if (finalSize != oldSize) {
                    triggerSnap()
                    currentOnResize(finalSize)
                }
            }

            // 1. Top Dot
            ResizeDot(
                alignment = Alignment.TopCenter,
                isDragging = activeDot == ResizeDotType.TOP,
                isVisible = activeDot == null || activeDot == ResizeDotType.TOP,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .offset {
                        IntOffset(
                            0,
                            if (activeDot == ResizeDotType.TOP) dragDeltaY.roundToInt() else 0
                        )
                    }
                    .pointerInput(item.widgetId, item.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            down.consume()
                            val startRoot = containerCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                            var lastRootY = startRoot?.y ?: down.position.y
                            activeDot = ResizeDotType.TOP
                            dragDeltaY = 0f
                            livePreviewSize = currentItem.size
                            onResizePreview?.invoke(livePreviewSize, ResizeDotType.TOP)
                            triggerTick()

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val curRootY = containerCoords?.takeIf { it.isAttached }?.localToRoot(change.position)?.y
                                        ?: change.position.y
                                    val dy = curRootY - lastRootY
                                    lastRootY = curRootY
                                    change.consume()
                                    if (dy != 0f) {
                                        handleVerticalDrag(ResizeDotType.TOP, dy)
                                    }
                                }
                            } finally {
                                finishDrag()
                            }
                        }
                    }
            )

            // 2. Bottom Dot
            ResizeDot(
                alignment = Alignment.BottomCenter,
                isDragging = activeDot == ResizeDotType.BOTTOM,
                isVisible = activeDot == null || activeDot == ResizeDotType.BOTTOM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .offset {
                        IntOffset(
                            0,
                            if (activeDot == ResizeDotType.BOTTOM) dragDeltaY.roundToInt() else 0
                        )
                    }
                    .pointerInput(item.widgetId, item.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            down.consume()
                            val startRoot = containerCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                            var lastRootY = startRoot?.y ?: down.position.y
                            activeDot = ResizeDotType.BOTTOM
                            dragDeltaY = 0f
                            livePreviewSize = currentItem.size
                            onResizePreview?.invoke(livePreviewSize, ResizeDotType.BOTTOM)
                            triggerTick()

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val curRootY = containerCoords?.takeIf { it.isAttached }?.localToRoot(change.position)?.y
                                        ?: change.position.y
                                    val dy = curRootY - lastRootY
                                    lastRootY = curRootY
                                    change.consume()
                                    if (dy != 0f) {
                                        handleVerticalDrag(ResizeDotType.BOTTOM, dy)
                                    }
                                }
                            } finally {
                                finishDrag()
                            }
                        }
                    }
            )

            // 3. Left Dot
            ResizeDot(
                alignment = Alignment.CenterStart,
                isDragging = activeDot == ResizeDotType.LEFT,
                isVisible = activeDot == null || activeDot == ResizeDotType.LEFT,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-4).dp)
                    .offset {
                        IntOffset(
                            if (activeDot == ResizeDotType.LEFT) dragDeltaX.roundToInt() else 0,
                            0
                        )
                    }
                    .pointerInput(item.widgetId, item.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            down.consume()
                            val startRoot = containerCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                            var lastRootX = startRoot?.x ?: down.position.x
                            activeDot = ResizeDotType.LEFT
                            dragDeltaX = 0f
                            livePreviewSize = currentItem.size
                            onResizePreview?.invoke(livePreviewSize, ResizeDotType.LEFT)
                            triggerTick()

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val curRootX = containerCoords?.takeIf { it.isAttached }?.localToRoot(change.position)?.x
                                        ?: change.position.x
                                    val dx = curRootX - lastRootX
                                    lastRootX = curRootX
                                    change.consume()
                                    if (dx != 0f) {
                                        handleHorizontalDrag(ResizeDotType.LEFT, dx)
                                    }
                                }
                            } finally {
                                finishDrag()
                            }
                        }
                    }
            )

            // 4. Right Dot
            ResizeDot(
                alignment = Alignment.CenterEnd,
                isDragging = activeDot == ResizeDotType.RIGHT,
                isVisible = activeDot == null || activeDot == ResizeDotType.RIGHT,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 4.dp)
                    .offset {
                        IntOffset(
                            if (activeDot == ResizeDotType.RIGHT) dragDeltaX.roundToInt() else 0,
                            0
                        )
                    }
                    .pointerInput(item.widgetId, item.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            down.consume()
                            val startRoot = containerCoords?.takeIf { it.isAttached }?.localToRoot(down.position)
                            var lastRootX = startRoot?.x ?: down.position.x
                            activeDot = ResizeDotType.RIGHT
                            dragDeltaX = 0f
                            livePreviewSize = currentItem.size
                            onResizePreview?.invoke(livePreviewSize, ResizeDotType.RIGHT)
                            triggerTick()

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val curRootX = containerCoords?.takeIf { it.isAttached }?.localToRoot(change.position)?.x
                                        ?: change.position.x
                                    val dx = curRootX - lastRootX
                                    lastRootX = curRootX
                                    change.consume()
                                    if (dx != 0f) {
                                        handleHorizontalDrag(ResizeDotType.RIGHT, dx)
                                    }
                                }
                            } finally {
                                finishDrag()
                            }
                        }
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
    val h = DashboardGridDefaults.getWidgetHeight(size.rows).value
    return Pair(w, h)
}



package com.example.buddygotchi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PagesMenuDropdown(
    isOpen: Boolean,
    pages: List<DashboardPageItem>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onCreatePage: (DashboardPageTemplate) -> Unit,
    onDeletePage: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerPickUpHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun triggerTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun triggerDropHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    var isTemplateSelectorOpen by remember { mutableStateOf(false) }

    // Long-press drag reordering state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dropAnimOffsetY = remember { Animatable(0f) }
    var isDropping by remember { mutableStateOf(false) }

    val activeDragY = if (isDropping) dropAnimOffsetY.value else dragOffsetY
    val itemHeight = 54.dp
    val itemGap = 6.dp
    val itemSlotPx = with(density) { (itemHeight + itemGap).toPx() }

    val currentTargetIndex by remember(pages.size, itemSlotPx) {
        derivedStateOf {
            draggedIndex?.let { d ->
                val offsetSlots = (activeDragY / itemSlotPx).roundToInt()
                (d + offsetSlots).coerceIn(0, pages.size - 1)
            }
        }
    }

    // Haptic tick when passing into a new slot
    var lastTargetIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(currentTargetIndex, draggedIndex) {
        val target = currentTargetIndex
        if (draggedIndex != null && target != null && target != lastTargetIndex) {
            if (lastTargetIndex != null) {
                triggerTick()
            }
            lastTargetIndex = target
        }
        if (draggedIndex == null) {
            lastTargetIndex = null
        }
    }

    // Reset template selector & drag state when menu closes
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            isTemplateSelectorOpen = false
            draggedIndex = null
            dragOffsetY = 0f
            dropAnimOffsetY.snapTo(0f)
            isDropping = false
        }
    }

    if (isOpen) {
        // Scrim backdrop to close on outside click
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(150f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (draggedIndex == null && !isDropping) {
                        onDismiss()
                    }
                }
        ) {
            // Dropdown panel expanding downwards from the top header opposite BUDDYGOTCHI
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            expandFrom = Alignment.Top
                        ),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        shrinkVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            shrinkTowards = Alignment.Top
                        ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 88.dp,
                        end = DashboardGridDefaults.HORIZONTAL_PADDING
                    )
            ) {
                val scrollState = rememberScrollState()

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NothingCardSurface,
                    border = BorderStroke(1.dp, NothingBorder),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .width(314.dp)
                        .heightIn(max = 560.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks inside menu */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Consume all clicks inside menu */ }
                            .verticalScroll(scrollState, enabled = draggedIndex == null)
                            .padding(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAGES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.5.sp,
                                    color = NothingWhite
                                )
                            }

                            Text(
                                text = "${pages.size} TOTAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NothingTextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (draggedIndex != null) "DRAGGING PAGE // RELEASE TO SET" else "HOLD & DRAG TO MOVE // ▲ UP = LEFT • ▼ DOWN = RIGHT",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp,
                            color = if (draggedIndex != null) NothingRed else NothingTextTertiary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Pages List
                        val mainIndex = pages.indexOfFirst { it.type == DashboardPageType.MAIN }.takeIf { it != -1 } ?: 0

                        Column(
                            verticalArrangement = Arrangement.spacedBy(itemGap),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pages.forEachIndexed { index, page ->
                                key(page.id) {
                                    val isThisItemDragged = (index == draggedIndex)
                                    val isActive = (index == currentPageIndex)
                                    val isMain = (page.type == DashboardPageType.MAIN)
                                    val distFromMain = index - mainIndex
                                    val canMoveUp = (index > 0)
                                    val canMoveDown = (index < pages.size - 1)

                                    val targetDisplacement = remember(index, draggedIndex, currentTargetIndex, itemSlotPx) {
                                        val d = draggedIndex
                                        val target = currentTargetIndex
                                        if (d == null || target == null || index == d) 0f
                                        else if (target > d) {
                                            if (index in (d + 1)..target) -itemSlotPx else 0f
                                        } else if (target < d) {
                                            if (index in target until d) itemSlotPx else 0f
                                        } else 0f
                                    }

                                    val animatedDisplacement by animateFloatAsState(
                                        targetValue = targetDisplacement,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "disp_${page.id}"
                                    )

                                    val effectiveOffsetY = if (isThisItemDragged) activeDragY else animatedDisplacement

                                    val itemScale by animateFloatAsState(
                                        targetValue = if (isThisItemDragged) 1.03f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "scale_${page.id}"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(itemHeight)
                                            .zIndex(if (isThisItemDragged) 100f else 1f)
                                            .offset { IntOffset(0, effectiveOffsetY.roundToInt()) }
                                            .graphicsLayer {
                                                scaleX = itemScale
                                                scaleY = itemScale
                                                shadowElevation = if (isThisItemDragged) with(density) { 14.dp.toPx() } else 0f
                                                clip = false
                                            }
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isThisItemDragged -> Color(0xFF261818)
                                                    isActive -> Color(0x1AD71920)
                                                    isMain -> Color(0x0ED71920)
                                                    else -> NothingSurfaceVariant.copy(alpha = 0.5f)
                                                }
                                            )
                                            .border(
                                                if (isThisItemDragged) 1.5.dp else 1.dp,
                                                when {
                                                    isThisItemDragged -> NothingRed.copy(alpha = 0.95f)
                                                    isActive -> NothingRed.copy(alpha = 0.8f)
                                                    isMain -> NothingRed.copy(alpha = 0.35f)
                                                    else -> NothingBorder.copy(alpha = 0.5f)
                                                },
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(start = 8.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Draggable & Tappable Body (Page Number, Title, Position Badge, Tactile Grip Handle)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .pointerInput(page.id, index) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggedIndex = index
                                                            dragOffsetY = 0f
                                                            isDropping = false
                                                            triggerPickUpHaptic()
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                        },
                                                        onDragEnd = {
                                                            val d = draggedIndex
                                                            if (d != null) {
                                                                val offsetSlots = (dragOffsetY / itemSlotPx).roundToInt()
                                                                val t = (d + offsetSlots).coerceIn(0, pages.size - 1)
                                                                coroutineScope.launch {
                                                                    try {
                                                                        isDropping = true
                                                                        dropAnimOffsetY.snapTo(dragOffsetY)
                                                                        if (t != d) {
                                                                            val landingOffset = (t - d) * itemSlotPx
                                                                            dropAnimOffsetY.animateTo(
                                                                                targetValue = landingOffset,
                                                                                animationSpec = spring(
                                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                                    stiffness = Spring.StiffnessMedium
                                                                                )
                                                                            )
                                                                            onMovePage(d, t)
                                                                            triggerDropHaptic()
                                                                        } else {
                                                                            dropAnimOffsetY.animateTo(
                                                                                targetValue = 0f,
                                                                                animationSpec = spring(
                                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                                    stiffness = Spring.StiffnessMedium
                                                                                )
                                                                            )
                                                                        }
                                                                    } finally {
                                                                        dragOffsetY = 0f
                                                                        dropAnimOffsetY.snapTo(0f)
                                                                        isDropping = false
                                                                        draggedIndex = null
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onDragCancel = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    isDropping = true
                                                                    dropAnimOffsetY.snapTo(dragOffsetY)
                                                                    dropAnimOffsetY.animateTo(
                                                                        targetValue = 0f,
                                                                        animationSpec = spring(
                                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                                            stiffness = Spring.StiffnessMedium
                                                                        )
                                                                    )
                                                                } finally {
                                                                    dragOffsetY = 0f
                                                                    dropAnimOffsetY.snapTo(0f)
                                                                    isDropping = false
                                                                    draggedIndex = null
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (draggedIndex == null && !isDropping) {
                                                        onSelectPage(index)
                                                        onDismiss()
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tactile Nothing OS 6-dot grip handle
                                            Column(
                                                modifier = Modifier.padding(end = 7.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                repeat(3) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(2.5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isThisItemDragged) NothingRed
                                                                    else NothingTextTertiary.copy(alpha = 0.45f)
                                                                )
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(2.5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isThisItemDragged) NothingRed
                                                                    else NothingTextTertiary.copy(alpha = 0.45f)
                                                                )
                                                        )
                                                    }
                                                }
                                            }

                                            // Page Number in DSEG7
                                            Text(
                                                text = String.format("%02d", index + 1),
                                                fontFamily = Dseg7FontFamily,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isThisItemDragged || isActive || isMain) NothingRed else NothingTextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Title & Position Tag
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = page.title,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        letterSpacing = 0.5.sp,
                                                        color = if (isThisItemDragged || isActive || isMain) NothingWhite else NothingTextSecondary,
                                                        maxLines = 1
                                                    )
                                                    if (isActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(NothingRed.copy(alpha = 0.2f))
                                                                .border(0.5.dp, NothingRed, RoundedCornerShape(3.dp))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "LIVE",
                                                                fontSize = 6.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = NothingRed
                                                            )
                                                        }
                                                    }
                                                }
                                                // Position relative to MAIN badge
                                                Spacer(modifier = Modifier.height(2.dp))
                                                when {
                                                    page.type == DashboardPageType.SQUAD -> {
                                                        Text(
                                                            text = "◈ DEDICATED SQUAD HUB",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NothingRed.copy(alpha = 0.9f),
                                                            letterSpacing = 0.3.sp
                                                        )
                                                    }
                                                    distFromMain < 0 -> {
                                                        val stepsLeft = -distFromMain
                                                        Text(
                                                            text = "← L$stepsLeft (LEFT)",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NothingTextTertiary,
                                                            letterSpacing = 0.3.sp
                                                        )
                                                    }
                                                    distFromMain == 0 -> {
                                                        Text(
                                                            text = "⊙ CENTER POINT",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NothingRed.copy(alpha = 0.9f),
                                                            letterSpacing = 0.3.sp
                                                        )
                                                    }
                                                    else -> {
                                                        val stepsRight = distFromMain
                                                        Text(
                                                            text = "R$stepsRight → (RIGHT)",
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NothingTextTertiary,
                                                            letterSpacing = 0.3.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Action buttons: [▲] [▼] [✕]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Move Up button
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(if (canMoveUp) Color(0xFF242424) else Color(0xFF141414))
                                                    .border(0.5.dp, if (canMoveUp) Color(0xFF3B3B3B) else Color(0xFF222222), RoundedCornerShape(7.dp))
                                                    .clickable(enabled = canMoveUp) {
                                                        onMovePage(index, index - 1)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "▲",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (canMoveUp) NothingWhite else NothingTextTertiary.copy(alpha = 0.25f)
                                                )
                                            }

                                            // Move Down button
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(if (canMoveDown) Color(0xFF242424) else Color(0xFF141414))
                                                    .border(0.5.dp, if (canMoveDown) Color(0xFF3B3B3B) else Color(0xFF222222), RoundedCornerShape(7.dp))
                                                    .clickable(enabled = canMoveDown) {
                                                        onMovePage(index, index + 1)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "▼",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (canMoveDown) NothingWhite else NothingTextTertiary.copy(alpha = 0.25f)
                                                )
                                            }

                                            // Delete button (for custom deletable pages)
                                            if (page.canDelete) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF241515))
                                                        .border(0.5.dp, NothingRed.copy(alpha = 0.4f), CircleShape)
                                                        .clickable { onDeletePage(page.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "✕",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NothingRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Add / Create Page Section
                        AnimatedContent(
                            targetState = isTemplateSelectorOpen,
                            transitionSpec = {
                                (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
                            },
                            label = "AddPageSectionTransition"
                        ) { isSelectorOpen ->
                            if (!isSelectorOpen) {
                                // + NEW PAGE Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingSurfaceVariant)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                        .clickable { isTemplateSelectorOpen = true }
                                        .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ NEW PAGE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.2.sp,
                                        color = NothingWhite
                                    )
                                }
                            } else {
                                // Template Selection list
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CHOOSE TEMPLATE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp,
                                            color = NothingRed
                                        )
                                        Text(
                                            text = "BACK",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = NothingTextTertiary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { isTemplateSelectorOpen = false }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DashboardPageTemplates.TEMPLATES.forEach { template ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NothingDarkBackground)
                                                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        onCreatePage(template)
                                                        isTemplateSelectorOpen = false
                                                        onDismiss()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = template.iconEmoji,
                                                            fontSize = 12.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = template.name,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            letterSpacing = 1.sp,
                                                            color = NothingWhite
                                                        )
                                                    }

                                                    Text(
                                                        text = "+ ADD",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = NothingRed
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = template.description,
                                                    fontSize = 8.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = NothingTextTertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isTemplateSelectorOpen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingDarkBackground)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val appWidgetManager = context.getSystemService(android.appwidget.AppWidgetManager::class.java)
                                            val myProvider = android.content.ComponentName(context, BuddyTaskWidgetProvider::class.java)
                                            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                            }
                                        }
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⊞ PIN WIDGET TO HOME SCREEN",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = NothingTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

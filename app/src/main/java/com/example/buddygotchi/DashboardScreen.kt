package com.example.buddygotchi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.buddygotchi.ui.theme.Dseg7FontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.nothingDotGrid
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerDropHaptic() {
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

    fun triggerTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun triggerConfirmPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    val livePageFloat = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val currentSnappedPage = livePageFloat.roundToInt().coerceIn(0, 2)
    var lastTickedPage by remember { mutableIntStateOf(1) }
    var hasInitializedSettled by remember { mutableStateOf(false) }
    LaunchedEffect(currentSnappedPage) {
        if (currentSnappedPage != lastTickedPage) {
            triggerTick()
            lastTickedPage = currentSnappedPage
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (hasInitializedSettled) {
            triggerConfirmPulse()
        } else {
            hasInitializedSettled = true
        }
    }

    var tasks by remember {
        mutableStateOf(TaskManager.getTasks(context))
    }
    var buddyXp by remember {
        mutableIntStateOf(TaskManager.getBuddyXp(context))
    }
    val buddyLevel = remember(buddyXp) { (buddyXp / 50) + 1 }

    val nextTask = remember(tasks) {
        val now = System.currentTimeMillis()
        tasks.filter { !it.isCompleted && it.taskTime.time >= now }
            .minByOrNull { it.taskTime.time }
            ?: tasks.filter { !it.isCompleted }.minByOrNull { it.taskTime.time }
    }

    var layout by remember {
        mutableStateOf(DashboardLayoutManager.loadLayout(context))
    }

    // Edit Mode state
    var isEditMode by remember { mutableStateOf(false) }

    fun updateLayout(newLayout: List<DashboardWidgetItem>) {
        layout = newLayout
        DashboardLayoutManager.saveLayout(context, newLayout)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val unusedWidgets = remember(layout) {
        DashboardLayoutManager.getUnusedWidgets(layout)
    }

    val scrollState = rememberScrollState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dropAnimOffsetY = remember { Animatable(0f) }
    var isDropping by remember { mutableStateOf(false) }
    val gapPx = with(density) { 14.dp.toPx() }

    val activeDragY = if (isDropping) dropAnimOffsetY.value else dragOffsetY
    val currentTargetIndex = remember(draggedIndex, activeDragY, layout) {
        draggedIndex?.let { d ->
            computeTargetIndex(d, activeDragY, layout, density, gapPx)
        } ?: 0
    }

    val packedRows = remember(layout) {
        DashboardLayoutManager.packIntoRows(layout)
    }

    val globalTouchPosition = remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nothingDotGrid()
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.firstOrNull { it.pressed }
                            globalTouchPosition.value = pressed?.position
                        }
                    }
                } finally {
                    globalTouchPosition.value = null
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Dashboard Header (shows "EDITING LAYOUT" + "[✓ DONE]" when in edit mode, drum roll opposite BUDDYGOTCHI)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 54.dp,
                        bottom = 10.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Text(
                        text = if (isEditMode) "EDITING LAYOUT" else "BUDDYGOTCHI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = NothingRed
                    )
                }

                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(NothingRed)
                            .clickable {
                                isEditMode = false
                                triggerDropHaptic()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✓ DONE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (unusedWidgets.isNotEmpty() && pagerState.currentPage == 1) {
                            var showAddMenu by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showAddMenu = true }
                                ) {
                                    Text(
                                        text = "+ Add",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    unusedWidgets.forEach { widgetId ->
                                        DropdownMenuItem(
                                            text = { Text("Add ${widgetId.title}") },
                                            onClick = {
                                                updateLayout(DashboardLayoutManager.addWidget(layout, widgetId))
                                                showAddMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Drum Roll Page Indicator opposite to "BUDDYGOTCHI"
                        PageDrumRollIndicator(
                            pagerState = pagerState,
                            onTabClick = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        )
                    }
                }
            }

            // HorizontalPager holding Left Page, Main Dashboard, Right Page
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isEditMode && draggedIndex == null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val pageOffset = ((pageIndex - pagerState.currentPage) - pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val clampedOffset = pageOffset.coerceIn(-1f, 1f)
                            val isLeft = clampedOffset < 0f

                            // Pivot at the seam between adjacent pages
                            transformOrigin = TransformOrigin(
                                pivotFractionX = if (isLeft) 1f else 0f,
                                pivotFractionY = 0.5f
                            )

                            // 3D Drum roll cylinder rotation (55 deg polygon face)
                            rotationY = clampedOffset * 55f

                            // Perspective camera distance for pronounced mechanical depth
                            cameraDistance = 12f * density.density

                            // Cylinder scale: curves away into depth
                            val scale = (1f - absOffset * 0.14f).coerceIn(0.82f, 1f)
                            scaleX = scale
                            scaleY = scale

                            // Atmospheric cylindrical lighting fade
                            alpha = (1f - absOffset * 0.45f).coerceIn(0.50f, 1f)
                        }
                ) {
                    when (pageIndex) {
                    0 -> BlankTabPage(pageNumber = "01", label = "LEFT")
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState, enabled = draggedIndex == null)
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 2.dp,
                                    bottom = 32.dp
                                )
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Render Rows of Widgets
                            packedRows.forEach { rowItems ->
                if (rowItems.size == 1) {
                    val item = rowItems[0]
                    val itemIndex = layout.indexOfFirst { it.widgetId == item.widgetId }
                    val isThisItemDragged = (itemIndex == draggedIndex && itemIndex != -1)

                    val itemScale by animateFloatAsState(
                        targetValue = if (isThisItemDragged) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "itemScale_${item.widgetId}"
                    )
                    val targetDisplacement = remember(itemIndex, draggedIndex, currentTargetIndex, layout) {
                        if (itemIndex != -1) {
                            computeDisplacementForIndex(itemIndex, draggedIndex, currentTargetIndex, layout, density, gapPx)
                        } else 0f
                    }
                    val animatedDisplacement by animateFloatAsState(
                        targetValue = targetDisplacement,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "itemDisplacement_${item.widgetId}"
                    )
                    val effectiveOffsetY = if (isThisItemDragged) activeDragY else animatedDisplacement

                    val widgetWidthModifier = when (item.size.cols) {
                        1 -> Modifier.width(88.dp)
                        2 -> Modifier.fillMaxWidth(0.5f)
                        3 -> Modifier.fillMaxWidth(0.75f)
                        else -> Modifier.fillMaxWidth()
                    }

                    key(item.widgetId) {
                        DashboardWidgetContainer(
                            item = item,
                            isEditMode = isEditMode,
                            onEnterEditMode = { isEditMode = true },
                            onResize = { newSize ->
                                updateLayout(DashboardLayoutManager.updateWidgetSize(layout, item.widgetId, newSize))
                            },
                            onDragReorderStart = {
                                if (itemIndex != -1) {
                                    draggedIndex = itemIndex
                                    dragOffsetY = 0f
                                    isDropping = false
                                }
                            },
                            onDragReorderMove = { dy ->
                                dragOffsetY += dy
                            },
                            onDragReorderEnd = {
                                coroutineScope.launch {
                                    val d = draggedIndex
                                    if (d != null && d != -1) {
                                        try {
                                            val t = computeTargetIndex(d, dragOffsetY, layout, density, gapPx)
                                            isDropping = true
                                            dropAnimOffsetY.snapTo(dragOffsetY)
                                            if (t != d) {
                                                val landingOffset = computeLandingOffset(d, t, layout, density, gapPx)
                                                dropAnimOffsetY.animateTo(
                                                    targetValue = landingOffset,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                                val newLayout = DashboardLayoutManager.moveItem(layout, d, t)
                                                updateLayout(newLayout)
                                                triggerDropHaptic()
                                            } else {
                                                dropAnimOffsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy))
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
                            modifier = widgetWidthModifier
                                .zIndex(if (isThisItemDragged) 100f else 1f)
                                .offset { IntOffset(0, effectiveOffsetY.roundToInt()) }
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    shadowElevation = if (isThisItemDragged) with(density) { 24.dp.toPx() } else 0f
                                    shape = RoundedCornerShape(if (item.size.cols == 1 && item.size.rows == 1) 20.dp else 24.dp)
                                    clip = false
                                }
                        ) { liveSize ->
                            RenderDashboardWidgetContent(
                                item = item,
                                liveSize = liveSize,
                                tasks = tasks,
                                onToggleTask = { task -> tasks = TaskManager.toggleTask(context, task.id) },
                                onClearDoneTasks = { tasks = TaskManager.clearDoneTasks(context) },
                                nextTask = nextTask,
                                buddyXp = buddyXp,
                                buddyLevel = buddyLevel,
                                onTaskCreated = { newTask ->
                                    tasks = TaskManager.addTask(context, newTask)
                                    buddyXp = TaskManager.getBuddyXp(context)
                                },
                                getTouchPosition = { globalTouchPosition.value }
                            )
                        }
                    }
                } else {
                    // Multiple widgets side-by-side in this row (e.g. Next + Buddy, or Cubes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        rowItems.forEach { item ->
                            val itemIndex = layout.indexOfFirst { it.widgetId == item.widgetId }
                            val isThisItemDragged = (itemIndex == draggedIndex && itemIndex != -1)

                            val itemScale by animateFloatAsState(
                                targetValue = if (isThisItemDragged) 1.04f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "itemScale_${item.widgetId}"
                            )
                            val targetDisplacement = remember(itemIndex, draggedIndex, currentTargetIndex, layout) {
                                if (itemIndex != -1) {
                                    computeDisplacementForIndex(itemIndex, draggedIndex, currentTargetIndex, layout, density, gapPx)
                                } else 0f
                            }
                            val animatedDisplacement by animateFloatAsState(
                                targetValue = targetDisplacement,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "itemDisplacement_${item.widgetId}"
                            )
                            val effectiveOffsetY = if (isThisItemDragged) activeDragY else animatedDisplacement

                            val cellModifier = Modifier.weight(item.size.cols.toFloat())

                            key(item.widgetId) {
                                DashboardWidgetContainer(
                                    item = item,
                                    isEditMode = isEditMode,
                                    onEnterEditMode = { isEditMode = true },
                                    onResize = { newSize ->
                                        updateLayout(DashboardLayoutManager.updateWidgetSize(layout, item.widgetId, newSize))
                                    },
                                    onDragReorderStart = {
                                        if (itemIndex != -1) {
                                            draggedIndex = itemIndex
                                            dragOffsetY = 0f
                                            isDropping = false
                                        }
                                    },
                                    onDragReorderMove = { dy ->
                                        dragOffsetY += dy
                                    },
                                    onDragReorderEnd = {
                                        coroutineScope.launch {
                                            val d = draggedIndex
                                            if (d != null && d != -1) {
                                                try {
                                                    val t = computeTargetIndex(d, dragOffsetY, layout, density, gapPx)
                                                    isDropping = true
                                                    dropAnimOffsetY.snapTo(dragOffsetY)
                                                    if (t != d) {
                                                        val landingOffset = computeLandingOffset(d, t, layout, density, gapPx)
                                                        dropAnimOffsetY.animateTo(
                                                            targetValue = landingOffset,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                                stiffness = Spring.StiffnessMedium
                                                            )
                                                        )
                                                        val newLayout = DashboardLayoutManager.moveItem(layout, d, t)
                                                        updateLayout(newLayout)
                                                        triggerDropHaptic()
                                                    } else {
                                                        dropAnimOffsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy))
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
                                    modifier = cellModifier
                                        .zIndex(if (isThisItemDragged) 100f else 1f)
                                        .offset { IntOffset(0, effectiveOffsetY.roundToInt()) }
                                        .graphicsLayer {
                                            scaleX = itemScale
                                            scaleY = itemScale
                                            shadowElevation = if (isThisItemDragged) with(density) { 24.dp.toPx() } else 0f
                                            shape = RoundedCornerShape(if (item.size.cols == 1 && item.size.rows == 1) 20.dp else 24.dp)
                                            clip = false
                                        }
                                ) { liveSize ->
                                    RenderDashboardWidgetContent(
                                        item = item,
                                        liveSize = liveSize,
                                        tasks = tasks,
                                        onToggleTask = { task -> tasks = TaskManager.toggleTask(context, task.id) },
                                        onClearDoneTasks = { tasks = TaskManager.clearDoneTasks(context) },
                                        nextTask = nextTask,
                                        buddyXp = buddyXp,
                                        buddyLevel = buddyLevel,
                                        onTaskCreated = { newTask ->
                                            tasks = TaskManager.addTask(context, newTask)
                                            buddyXp = TaskManager.getBuddyXp(context)
                                        },
                                        getTouchPosition = { globalTouchPosition.value }
                                    )
                                }
                            }
                        }
                        val totalCols = rowItems.sumOf { it.size.cols }
                        if (totalCols < 4) {
                            Spacer(modifier = Modifier.weight((4 - totalCols).toFloat()))
                        }
                    }
                }
                            }
                        }
                    }
                    2 -> BlankTabPage(pageNumber = "03", label = "RIGHT")
                }
            }
        }
    }
}
}

private fun getItemHeightPx(item: DashboardWidgetItem, density: Density): Float = with(density) {
    when (item.widgetId) {
        DashboardWidgetId.CLOCK -> 276.dp.toPx()
        else -> when (item.size.rows) {
            1 -> if (item.size.cols == 2) 110.dp.toPx() else 88.dp.toPx()
            2 -> 180.dp.toPx()
            3 -> 276.dp.toPx()
            4 -> 372.dp.toPx()
            else -> 180.dp.toPx()
        }
    }
}

private fun computeTargetIndex(
    draggedIndex: Int,
    dy: Float,
    layout: List<DashboardWidgetItem>,
    density: Density,
    gapPx: Float
): Int {
    var target = draggedIndex
    if (dy > 0) {
        var threshold = 0f
        for (i in (draggedIndex + 1)..layout.lastIndex) {
            val h = getItemHeightPx(layout[i], density) + gapPx
            if (dy > threshold + h * 0.45f) {
                target = i
                threshold += h
            } else {
                break
            }
        }
    } else if (dy < 0) {
        var threshold = 0f
        val absDy = abs(dy)
        for (i in (draggedIndex - 1) downTo 0) {
            val h = getItemHeightPx(layout[i], density) + gapPx
            if (absDy > threshold + h * 0.45f) {
                target = i
                threshold += h
            } else {
                break
            }
        }
    }
    return target
}

private fun computeLandingOffset(
    draggedIndex: Int,
    targetIndex: Int,
    layout: List<DashboardWidgetItem>,
    density: Density,
    gapPx: Float
): Float {
    if (targetIndex > draggedIndex) {
        var offset = 0f
        for (k in (draggedIndex + 1)..targetIndex) {
            offset += getItemHeightPx(layout[k], density) + gapPx
        }
        return offset
    } else if (targetIndex < draggedIndex) {
        var offset = 0f
        for (k in targetIndex until draggedIndex) {
            offset -= (getItemHeightPx(layout[k], density) + gapPx)
        }
        return offset
    }
    return 0f
}

private fun computeDisplacementForIndex(
    i: Int,
    draggedIndex: Int?,
    targetIndex: Int,
    layout: List<DashboardWidgetItem>,
    density: Density,
    gapPx: Float
): Float {
    if (draggedIndex == null || i == draggedIndex) return 0f
    val draggedHeight = getItemHeightPx(layout[draggedIndex], density) + gapPx
    return if (targetIndex > draggedIndex) {
        if (i in (draggedIndex + 1)..targetIndex) -draggedHeight else 0f
    } else if (targetIndex < draggedIndex) {
        if (i in targetIndex until draggedIndex) draggedHeight else 0f
    } else {
        0f
    }
}

@Composable
fun RenderDashboardWidgetContent(
    item: DashboardWidgetItem,
    liveSize: WidgetSize,
    tasks: List<ReminderTask>,
    onToggleTask: (ReminderTask) -> Unit,
    onClearDoneTasks: () -> Unit = {},
    nextTask: ReminderTask?,
    buddyXp: Int,
    buddyLevel: Int,
    onTaskCreated: (ReminderTask) -> Unit,
    getTouchPosition: () -> Offset? = { null }
) {
    when (item.widgetId) {
        DashboardWidgetId.CLOCK -> {
            ClockWidget(onTaskCreated = onTaskCreated)
        }
        DashboardWidgetId.TODAY -> {
            TodayWidget(
                tasks = tasks,
                onToggleTask = onToggleTask,
                onClearDoneTasks = onClearDoneTasks,
                size = liveSize
            )
        }
        DashboardWidgetId.NEXT -> {
            NextWidget(
                nextTask = nextTask,
                size = liveSize
            )
        }
        DashboardWidgetId.BUDDY -> {
            BuddyWidget(
                xp = buddyXp,
                level = buddyLevel,
                size = liveSize,
                getTouchPosition = getTouchPosition
            )
        }
    }
}

data class PageTabInfo(val number: String, val title: String)

@Composable
fun PageDrumRollIndicator(
    pagerState: PagerState,
    onTabClick: (Int) -> Unit
) {
    val tabs = remember {
        listOf(
            PageTabInfo("01", "LEFT"),
            PageTabInfo("02", "MAIN"),
            PageTabInfo("03", "RIGHT")
        )
    }

    val itemHeight = 24.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val density = LocalDensity.current

    val livePage = pagerState.currentPage + pagerState.currentPageOffsetFraction

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NothingCardSurface,
        border = BorderStroke(1.dp, NothingBorder),
        modifier = Modifier
            .width(106.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val nextPage = (pagerState.currentPage + 1) % 3
                onTabClick(nextPage)
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            tabs.forEachIndexed { i, tab ->
                val relPos = i - livePage
                val yOffset = relPos * itemHeightPx
                val absDist = abs(relPos)
                val scale = (1f - absDist * 0.16f).coerceIn(0.72f, 1f)
                val alpha = (1f - absDist * 0.70f).coerceIn(0f, 1f)
                val rotX = (-relPos * 36f).coerceIn(-75f, 75f)

                if (absDist < 1.4f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, yOffset.roundToInt()) }
                            .graphicsLayer {
                                rotationX = rotX
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                cameraDistance = 16f * density.density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = tab.number,
                                fontFamily = Dseg7FontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (absDist < 0.45f) NothingRed else NothingTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(if (absDist < 0.45f) NothingRed else NothingTextTertiary)
                            )
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (absDist < 0.45f) NothingWhite else NothingTextSecondary
                            )
                        }
                    }
                }
            }

            // Top fade slit mask
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NothingCardSurface,
                                NothingCardSurface.copy(alpha = 0f)
                            )
                        )
                    )
            )

            // Bottom fade slit mask
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NothingCardSurface.copy(alpha = 0f),
                                NothingCardSurface
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun BlankTabPage(
    pageNumber: String,
    label: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = pageNumber,
                    fontFamily = Dseg7FontFamily,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NothingWhite.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PAGE $pageNumber // $label",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NothingTextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "CANVAS EMPTY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp,
                    color = NothingTextTertiary
                )
            }
        }
    }
}
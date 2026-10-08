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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.DashboardGridBackground
import com.example.buddygotchi.ui.theme.NothingDarkBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LeftDashboardPage(
    isEditMode: Boolean = false,
    onSetEditMode: (Boolean) -> Unit = {},
    onNotesExpandedChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
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

    var notes by remember { mutableStateOf(NotesManager.getNotes(context)) }
    var isNotesExpanded by remember { mutableStateOf(false) }
    var initialEditingNoteId by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    var layout by remember {
        mutableStateOf(LeftDashboardLayoutManager.loadLayout(context))
    }

    val packedRows = remember(layout) {
        LeftDashboardLayoutManager.packIntoRows(layout)
    }

    // Active resize preview state
    var activeResizeWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var activeResizePreviewSize by remember { mutableStateOf<WidgetSize?>(null) }

    // Drag-and-drop vertical reordering state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dropAnimOffsetY = remember { Animatable(0f) }
    var isDropping by remember { mutableStateOf(false) }
    val activeDragY = if (isDropping) dropAnimOffsetY.value else dragOffsetY

    val gapPx = with(density) { DashboardGridDefaults.GAP.toPx() }

    val currentTargetIndex = remember(draggedIndex, dragOffsetY, layout) {
        if (draggedIndex != null && draggedIndex != -1) {
            DashboardDragUtils.computeTargetIndex(draggedIndex!!, dragOffsetY, layout, density, gapPx)
        } else {
            0
        }
    }

    fun updateLayout(newLayout: List<DashboardWidgetItem>) {
        layout = newLayout
        LeftDashboardLayoutManager.saveLayout(context, newLayout)
        triggerDropHaptic()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        AnimatedContent(
            targetState = isNotesExpanded,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleOut(targetScale = 1.04f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                } else {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 1.04f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                }
            },
            label = "NotesExpandTransition"
        ) { expanded ->
            if (expanded) {
                // Occupies the entire page
                NotesExpandedView(
                    notes = notes,
                    initialEditingNoteId = initialEditingNoteId,
                    onNotesUpdated = { updated ->
                        notes = updated
                    },
                    onCollapse = {
                        isNotesExpanded = false
                        onNotesExpandedChanged(false)
                        initialEditingNoteId = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Modular Grid Dashboard layout for Left Page
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val contentWidth = maxWidth - (DashboardGridDefaults.HORIZONTAL_PADDING * 2)

                    // Aligned Modular Dot Grid Background (with connecting blueprint lines in edit mode & resize illumination)
                    DashboardGridBackground(
                        isEditMode = isEditMode,
                        packedRows = packedRows,
                        activeResizeWidgetId = activeResizeWidgetId,
                        activeResizePreviewSize = activeResizePreviewSize,
                        scrollY = scrollState.value.toFloat(),
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState, enabled = draggedIndex == null)
                            .padding(
                                start = DashboardGridDefaults.HORIZONTAL_PADDING,
                                end = DashboardGridDefaults.HORIZONTAL_PADDING,
                                top = 2.dp,
                                bottom = 32.dp
                            )
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        verticalArrangement = Arrangement.spacedBy(DashboardGridDefaults.GAP)
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
                                        DashboardDragUtils.computeDisplacementForIndex(itemIndex, draggedIndex, currentTargetIndex, layout, density, gapPx)
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

                                val widgetWidth = DashboardGridDefaults.getWidgetWidth(item.size.cols, contentWidth)

                                key(item.widgetId) {
                                    DashboardWidgetContainer(
                                        item = item,
                                        isEditMode = isEditMode,
                                        onEnterEditMode = { onSetEditMode(true) },
                                        onResize = { newSize ->
                                            updateLayout(LeftDashboardLayoutManager.updateWidgetSize(layout, item.widgetId, newSize))
                                        },
                                        onResizePreview = { previewSize, _ ->
                                            activeResizeWidgetId = if (previewSize != null) item.widgetId else null
                                            activeResizePreviewSize = previewSize
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
                                                        val t = DashboardDragUtils.computeTargetIndex(d, dragOffsetY, layout, density, gapPx)
                                                        isDropping = true
                                                        dropAnimOffsetY.snapTo(dragOffsetY)
                                                        if (t != d) {
                                                            val landingOffset = DashboardDragUtils.computeLandingOffset(d, t, layout, density, gapPx)
                                                            dropAnimOffsetY.animateTo(
                                                                targetValue = landingOffset,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                    stiffness = Spring.StiffnessMedium
                                                                )
                                                            )
                                                            val newLayout = LeftDashboardLayoutManager.moveItem(layout, d, t)
                                                            updateLayout(newLayout)
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
                                        modifier = Modifier
                                            .width(widgetWidth)
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
                                        when (item.widgetId) {
                                            DashboardWidgetId.WATER -> {
                                                WaterTrackerWidget(size = liveSize)
                                            }
                                            DashboardWidgetId.NOTES -> {
                                                NotesCollapsedWidget(
                                                    notes = notes,
                                                    size = liveSize,
                                                    onExpand = {
                                                        if (!isEditMode) {
                                                            initialEditingNoteId = null
                                                            isNotesExpanded = true
                                                            onNotesExpandedChanged(true)
                                                        }
                                                    },
                                                    onQuickCreate = {
                                                        if (!isEditMode) {
                                                            val newNote = NoteItem(
                                                                title = "",
                                                                content = "",
                                                                updatedAt = System.currentTimeMillis()
                                                            )
                                                            notes = NotesManager.upsertNote(context, newNote)
                                                            initialEditingNoteId = newNote.id
                                                            isNotesExpanded = true
                                                            onNotesExpandedChanged(true)
                                                        }
                                                    }
                                                )
                                            }
                                            else -> {
                                                // Fallback if other widget appears
                                                Box(modifier = Modifier.size(80.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Multiple widgets side-by-side in this row (e.g. 2x2 Water + 2x2 Notes, or Cubes)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(DashboardGridDefaults.GAP),
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
                                                DashboardDragUtils.computeDisplacementForIndex(itemIndex, draggedIndex, currentTargetIndex, layout, density, gapPx)
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

                                        val widgetWidth = DashboardGridDefaults.getWidgetWidth(item.size.cols, contentWidth)

                                        key(item.widgetId) {
                                            DashboardWidgetContainer(
                                                item = item,
                                                isEditMode = isEditMode,
                                                onEnterEditMode = { onSetEditMode(true) },
                                                onResize = { newSize ->
                                                    updateLayout(LeftDashboardLayoutManager.updateWidgetSize(layout, item.widgetId, newSize))
                                                },
                                                onResizePreview = { previewSize, _ ->
                                                    activeResizeWidgetId = if (previewSize != null) item.widgetId else null
                                                    activeResizePreviewSize = previewSize
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
                                                                val t = DashboardDragUtils.computeTargetIndex(d, dragOffsetY, layout, density, gapPx)
                                                                isDropping = true
                                                                dropAnimOffsetY.snapTo(dragOffsetY)
                                                                if (t != d) {
                                                                    val landingOffset = DashboardDragUtils.computeLandingOffset(d, t, layout, density, gapPx)
                                                                    dropAnimOffsetY.animateTo(
                                                                        targetValue = landingOffset,
                                                                        animationSpec = spring(
                                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                                            stiffness = Spring.StiffnessMedium
                                                                        )
                                                                    )
                                                                    val newLayout = LeftDashboardLayoutManager.moveItem(layout, d, t)
                                                                    updateLayout(newLayout)
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
                                                modifier = Modifier
                                                    .width(widgetWidth)
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
                                                when (item.widgetId) {
                                                    DashboardWidgetId.WATER -> {
                                                        WaterTrackerWidget(size = liveSize)
                                                    }
                                                    DashboardWidgetId.NOTES -> {
                                                        NotesCollapsedWidget(
                                                            notes = notes,
                                                            size = liveSize,
                                                            onExpand = {
                                                                if (!isEditMode) {
                                                                    initialEditingNoteId = null
                                                                    isNotesExpanded = true
                                                                    onNotesExpandedChanged(true)
                                                                }
                                                            },
                                                            onQuickCreate = {
                                                                if (!isEditMode) {
                                                                    val newNote = NoteItem(
                                                                        title = "",
                                                                        content = "",
                                                                        updatedAt = System.currentTimeMillis()
                                                                    )
                                                                    notes = NotesManager.upsertNote(context, newNote)
                                                                    initialEditingNoteId = newNote.id
                                                                    isNotesExpanded = true
                                                                    onNotesExpandedChanged(true)
                                                                }
                                                            }
                                                        )
                                                    }
                                                    else -> {
                                                        Box(modifier = Modifier.size(80.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

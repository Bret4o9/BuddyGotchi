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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.DashboardGridBackground
import com.example.buddygotchi.ui.SlimScrollWheel
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class LeftPageExpandedState {
    COLLAPSED,
    NOTES,
    PASSWORDS,
    GROCERY
}

@Composable
fun LeftDashboardPage(
    layout: List<DashboardWidgetItem>,
    onUpdateLayout: (List<DashboardWidgetItem>) -> Unit,
    notes: List<NoteItem>,
    onNotesUpdated: (List<NoteItem>) -> Unit,
    passwords: List<PasswordItem> = emptyList(),
    onPasswordsUpdated: (List<PasswordItem>) -> Unit = {},
    groceryLists: List<GroceryList> = emptyList(),
    onGroceryListsUpdated: (List<GroceryList>) -> Unit = {},
    onFinishGroceryTaskReward: () -> Unit = {},
    tasks: List<ReminderTask> = emptyList(),
    onToggleTask: (ReminderTask) -> Unit = {},
    onClearDoneTasks: () -> Unit = {},
    nextTask: ReminderTask? = null,
    buddyXp: Int = 0,
    buddyLevel: Int = 1,
    onTaskCreated: (ReminderTask) -> Unit = {},
    saveLayout: ((List<DashboardWidgetItem>) -> Unit)? = null,
    isEditMode: Boolean = false,
    onSetEditMode: (Boolean) -> Unit = {},
    onNotesExpandedChanged: (Boolean) -> Unit = {},
    onPasswordsExpandedChanged: (Boolean) -> Unit = {},
    onGroceryExpandedChanged: (Boolean) -> Unit = {},
    onDraggingChanged: (Boolean) -> Unit = {},
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

    var expandedState by remember { mutableStateOf(LeftPageExpandedState.COLLAPSED) }
    var initialEditingNoteId by remember { mutableStateOf<String?>(null) }
    var initialEditingPasswordId by remember { mutableStateOf<String?>(null) }
    var initialEditingGroceryId by remember { mutableStateOf<String?>(null) }

    // Auto-expand if NOTES, PASSWORDS, or GROCERY is the ONLY widget on the page;
    // Auto-collapse into a smaller widget when a second widget is added.
    val singleWidgetId = if (layout.size == 1) layout[0].widgetId else null
    LaunchedEffect(layout.size, singleWidgetId) {
        if (layout.size == 1) {
            when (layout[0].widgetId) {
                DashboardWidgetId.NOTES -> {
                    expandedState = LeftPageExpandedState.NOTES
                    onNotesExpandedChanged(true)
                    onPasswordsExpandedChanged(false)
                    onGroceryExpandedChanged(false)
                    if (isEditMode) onSetEditMode(false)
                }
                DashboardWidgetId.PASSWORDS -> {
                    expandedState = LeftPageExpandedState.PASSWORDS
                    onPasswordsExpandedChanged(true)
                    onNotesExpandedChanged(false)
                    onGroceryExpandedChanged(false)
                    if (isEditMode) onSetEditMode(false)
                }
                DashboardWidgetId.GROCERY -> {
                    expandedState = LeftPageExpandedState.GROCERY
                    onGroceryExpandedChanged(true)
                    onNotesExpandedChanged(false)
                    onPasswordsExpandedChanged(false)
                    if (isEditMode) onSetEditMode(false)
                }
                else -> {
                    expandedState = LeftPageExpandedState.COLLAPSED
                    onNotesExpandedChanged(false)
                    onPasswordsExpandedChanged(false)
                    onGroceryExpandedChanged(false)
                }
            }
        } else {
            expandedState = LeftPageExpandedState.COLLAPSED
            onNotesExpandedChanged(false)
            onPasswordsExpandedChanged(false)
            onGroceryExpandedChanged(false)
        }
    }
    val scrollState = rememberScrollState()

    val packedRows = remember(layout) {
        LeftDashboardLayoutManager.packIntoRows(layout)
    }

    // Active resize preview state
    var activeResizeWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var activeResizePreviewSize by remember { mutableStateOf<WidgetSize?>(null) }

    // Drag-and-drop vertical reordering state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(draggedIndex) {
        onDraggingChanged(draggedIndex != null)
    }
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
        onUpdateLayout(newLayout)
        if (saveLayout != null) {
            saveLayout(newLayout)
        } else {
            LeftDashboardLayoutManager.saveLayout(context, newLayout)
        }
        triggerDropHaptic()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        AnimatedContent(
            targetState = expandedState,
            transitionSpec = {
                if (targetState != LeftPageExpandedState.COLLAPSED) {
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
            label = "LeftPageExpandTransition"
        ) { state ->
            when (state) {
                LeftPageExpandedState.NOTES -> {
                    // Occupies the entire page for Notes
                    NotesExpandedView(
                        notes = notes,
                        initialEditingNoteId = initialEditingNoteId,
                        onNotesUpdated = { updated ->
                            onNotesUpdated(updated)
                        },
                        onCollapse = {
                            expandedState = LeftPageExpandedState.COLLAPSED
                            onNotesExpandedChanged(false)
                            initialEditingNoteId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LeftPageExpandedState.PASSWORDS -> {
                    // Occupies the entire page for Passwords Vault
                    PasswordsExpandedView(
                        passwords = passwords,
                        initialEditingPasswordId = initialEditingPasswordId,
                        onPasswordsUpdated = { updated ->
                            onPasswordsUpdated(updated)
                        },
                        onCollapse = {
                            expandedState = LeftPageExpandedState.COLLAPSED
                            onPasswordsExpandedChanged(false)
                            initialEditingPasswordId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LeftPageExpandedState.GROCERY -> {
                    // Occupies the entire page for Grocery Lists
                    GroceryExpandedView(
                        groceryLists = groceryLists,
                        initialEditingListId = initialEditingGroceryId,
                        onListsUpdated = { updated ->
                            onGroceryListsUpdated(updated)
                        },
                        onFinishTaskReward = onFinishGroceryTaskReward,
                        onCollapse = {
                            expandedState = LeftPageExpandedState.COLLAPSED
                            onGroceryExpandedChanged(false)
                            initialEditingGroceryId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LeftPageExpandedState.COLLAPSED -> {
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
                        if (layout.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(NothingRed)
                                    )
                                    Text(
                                        text = "NO WIDGETS DEPLOYED",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = NothingTextSecondary
                                    )
                                    Text(
                                        text = "PULL DOWN SHELF ABOVE TO DEPLOY WIDGETS",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = NothingTextTertiary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Render Rows of Widgets
                        packedRows.forEach { rowItems ->
                            if (rowItems.size == 1) {
                                val item = rowItems[0]
                                val itemIndex = layout.indexOfFirst { it.widgetId == item.widgetId }
                                val isThisItemDragged = (itemIndex == draggedIndex && itemIndex != -1)

                                val itemScale by animateFloatAsState(
                                    targetValue = if (isThisItemDragged) 0.94f else 1f,
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
                                        onStashToShelf = {
                                            updateLayout(LeftDashboardLayoutManager.removeWidget(layout, item.widgetId))
                                        },
                                        isPickedUp = isThisItemDragged,
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
                                                shadowElevation = if (isThisItemDragged) with(density) { 12.dp.toPx() } else 0f
                                                shape = RoundedCornerShape(if (item.size.cols == 1 && item.size.rows == 1) 20.dp else 24.dp)
                                                clip = false
                                            }
                                    ) { liveSize ->
                                        RenderDashboardWidgetContent(
                                            item = item,
                                            liveSize = liveSize,
                                            tasks = tasks,
                                            onToggleTask = onToggleTask,
                                            onClearDoneTasks = onClearDoneTasks,
                                            nextTask = nextTask,
                                            buddyXp = buddyXp,
                                            buddyLevel = buddyLevel,
                                            onTaskCreated = onTaskCreated,
                                            notes = notes,
                                            onNotesUpdated = onNotesUpdated,
                                            onExpandNotes = {
                                                if (!isEditMode) {
                                                    initialEditingNoteId = null
                                                    expandedState = LeftPageExpandedState.NOTES
                                                    onNotesExpandedChanged(true)
                                                }
                                            },
                                            onQuickCreateNote = {
                                                if (!isEditMode) {
                                                    val newNote = NoteItem(
                                                        title = "",
                                                        content = "",
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                    onNotesUpdated(NotesManager.upsertNote(context, newNote))
                                                    initialEditingNoteId = newNote.id
                                                    expandedState = LeftPageExpandedState.NOTES
                                                    onNotesExpandedChanged(true)
                                                }
                                            },
                                            passwords = passwords,
                                            onPasswordsUpdated = onPasswordsUpdated,
                                            onExpandPasswords = {
                                                if (!isEditMode) {
                                                    initialEditingPasswordId = null
                                                    expandedState = LeftPageExpandedState.PASSWORDS
                                                    onPasswordsExpandedChanged(true)
                                                }
                                            },
                                            onQuickCreatePassword = {
                                                if (!isEditMode) {
                                                    val newPass = PasswordItem(
                                                        title = "",
                                                        password = "",
                                                        username = "",
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                    onPasswordsUpdated(PasswordsManager.upsertPassword(context, newPass))
                                                    initialEditingPasswordId = newPass.id
                                                    expandedState = LeftPageExpandedState.PASSWORDS
                                                    onPasswordsExpandedChanged(true)
                                                }
                                            },
                                            groceryLists = groceryLists,
                                            onGroceryListsUpdated = onGroceryListsUpdated,
                                            onExpandGrocery = {
                                                if (!isEditMode) {
                                                    initialEditingGroceryId = null
                                                    expandedState = LeftPageExpandedState.GROCERY
                                                    onGroceryExpandedChanged(true)
                                                }
                                            },
                                            onQuickCreateGrocery = {
                                                if (!isEditMode) {
                                                    val newGrocery = GroceryList(
                                                        title = "GROCERY LIST",
                                                        entries = emptyList(),
                                                        viewMode = GroceryViewMode.LIST,
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                    onGroceryListsUpdated(GroceryManager.upsertList(context, newGrocery))
                                                    initialEditingGroceryId = newGrocery.id
                                                    expandedState = LeftPageExpandedState.GROCERY
                                                    onGroceryExpandedChanged(true)
                                                }
                                            },
                                            onFinishGroceryTaskReward = onFinishGroceryTaskReward
                                        )
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
                                            targetValue = if (isThisItemDragged) 0.94f else 1f,
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
                                                onStashToShelf = {
                                                    updateLayout(LeftDashboardLayoutManager.removeWidget(layout, item.widgetId))
                                                },
                                                isPickedUp = isThisItemDragged,
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
                                                        shadowElevation = if (isThisItemDragged) with(density) { 12.dp.toPx() } else 0f
                                                        shape = RoundedCornerShape(if (item.size.cols == 1 && item.size.rows == 1) 20.dp else 24.dp)
                                                        clip = false
                                                    }
                                            ) { liveSize ->
                                                RenderDashboardWidgetContent(
                                                    item = item,
                                                    liveSize = liveSize,
                                                    tasks = tasks,
                                                    onToggleTask = onToggleTask,
                                                    onClearDoneTasks = onClearDoneTasks,
                                                    nextTask = nextTask,
                                                    buddyXp = buddyXp,
                                                    buddyLevel = buddyLevel,
                                                    onTaskCreated = onTaskCreated,
                                                    notes = notes,
                                                    onNotesUpdated = onNotesUpdated,
                                                    onExpandNotes = {
                                                        if (!isEditMode) {
                                                            initialEditingNoteId = null
                                                            expandedState = LeftPageExpandedState.NOTES
                                                            onNotesExpandedChanged(true)
                                                        }
                                                    },
                                                    onQuickCreateNote = {
                                                        if (!isEditMode) {
                                                            val newNote = NoteItem(
                                                                title = "",
                                                                content = "",
                                                                updatedAt = System.currentTimeMillis()
                                                            )
                                                            onNotesUpdated(NotesManager.upsertNote(context, newNote))
                                                            initialEditingNoteId = newNote.id
                                                            expandedState = LeftPageExpandedState.NOTES
                                                            onNotesExpandedChanged(true)
                                                        }
                                                    },
                                                    passwords = passwords,
                                                    onPasswordsUpdated = onPasswordsUpdated,
                                                    onExpandPasswords = {
                                                        if (!isEditMode) {
                                                            initialEditingPasswordId = null
                                                            expandedState = LeftPageExpandedState.PASSWORDS
                                                            onPasswordsExpandedChanged(true)
                                                        }
                                                    },
                                                    onQuickCreatePassword = {
                                                        if (!isEditMode) {
                                                            val newPass = PasswordItem(
                                                                title = "",
                                                                password = "",
                                                                username = "",
                                                                updatedAt = System.currentTimeMillis()
                                                            )
                                                            onPasswordsUpdated(PasswordsManager.upsertPassword(context, newPass))
                                                            initialEditingPasswordId = newPass.id
                                                            expandedState = LeftPageExpandedState.PASSWORDS
                                                            onPasswordsExpandedChanged(true)
                                                        }
                                                    },
                                                    groceryLists = groceryLists,
                                                    onGroceryListsUpdated = onGroceryListsUpdated,
                                                    onExpandGrocery = {
                                                        if (!isEditMode) {
                                                            initialEditingGroceryId = null
                                                            expandedState = LeftPageExpandedState.GROCERY
                                                            onGroceryExpandedChanged(true)
                                                        }
                                                    },
                                                    onQuickCreateGrocery = {
                                                        if (!isEditMode) {
                                                            val newGrocery = GroceryList(
                                                                title = "GROCERY LIST",
                                                                entries = emptyList(),
                                                                viewMode = GroceryViewMode.LIST,
                                                                updatedAt = System.currentTimeMillis()
                                                            )
                                                            onGroceryListsUpdated(GroceryManager.upsertList(context, newGrocery))
                                                            initialEditingGroceryId = newGrocery.id
                                                            expandedState = LeftPageExpandedState.GROCERY
                                                            onGroceryExpandedChanged(true)
                                                        }
                                                    },
                                                    onFinishGroceryTaskReward = onFinishGroceryTaskReward
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        SlimScrollWheel(
                            scrollState = scrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(top = 16.dp, bottom = 48.dp, end = 3.dp)
                                .fillMaxHeight()
                        )
                    }
                }
        }
    }
}
}

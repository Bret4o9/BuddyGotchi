package com.example.buddygotchi

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.ui.theme.*

/**
 * Thin strip residing directly below the BUDDYGOTCHI title row and above the widget grid.
 * Similar to the page menu drum roll in Nothing OS aesthetics, but spans the full screen width.
 */
@Composable
fun ShelfStrip(
    isOpen: Boolean,
    stashedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isOpen) NothingCardSurface else NothingCardSurface.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOpen) NothingRed.copy(alpha = 0.85f) else if (stashedCount > 0) NothingBorder else NothingBorder.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Red dot indicator + SHELF label + stashed count badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (stashedCount > 0) NothingRed else NothingTextTertiary)
                )
                Text(
                    text = "SHELF",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isOpen) NothingRed else NothingWhite
                )
                Text(
                    text = "[ ${stashedCount.toString().padStart(2, '0')} ]",
                    fontFamily = Dseg7FontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (stashedCount > 0) NothingRed else NothingTextTertiary
                )
            }

            // Center: Mechanical Grip Handle (drawer slot)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (isOpen) NothingRed.copy(alpha = 0.7f) else Color(0x33FFFFFF))
                )
            }

            // Right: Expand/Collapse indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isOpen) "COLLAPSE ▲" else if (stashedCount > 0) "OPEN ▼" else "EMPTY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = if (isOpen) NothingRed else if (stashedCount > 0) NothingTextSecondary else NothingTextTertiary
                )
            }
        }
    }
}

/**
 * Full overlay that extends down over everything else when the shelf is opened.
 * Displays a realistic industrial shelf structure with metallic ledges holding the undeployed widgets.
 */
@Composable
fun ShelfOverlay(
    isOpen: Boolean,
    shelfWidgets: List<DashboardWidgetId>,
    currentPageIndex: Int,
    onClose: () -> Unit,
    onDeployWidget: (DashboardWidgetId, Int) -> Unit,
    tasks: List<ReminderTask> = emptyList(),
    onToggleTask: (ReminderTask) -> Unit = {},
    onClearDoneTasks: () -> Unit = {},
    nextTask: ReminderTask? = null,
    buddyXp: Int = 0,
    buddyLevel: Int = 1,
    notes: List<NoteItem> = emptyList(),
    onExpandNotes: () -> Unit = {},
    onQuickCreateNote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerSnap() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (_: Exception) {}
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    BackHandler(enabled = isOpen) {
        onClose()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Semi-transparent Backdrop Scrim (clicking anywhere outside collapses the shelf)
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                    }
            )
        }

        // 2. Shelf Dropdown Panel extending downwards over everything else
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it / 3 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(),
            modifier = Modifier
                .zIndex(200f)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 130.dp, // Positioned directly below the shelf strip
                    bottom = 24.dp
                )
        ) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xFF131313),
                border = BorderStroke(1.dp, NothingBorder),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Shelf Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
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
                                    text = "SHELF STORAGE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = NothingWhite
                                )
                                Text(
                                    text = "[ 0${shelfWidgets.size} ]",
                                    fontFamily = Dseg7FontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingRed
                                )
                            }
                            Text(
                                text = "STASHED WIDGETS · TAP TO DEPLOY TO ANY PAGE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp,
                                color = NothingTextTertiary
                            )
                        }

                        // Close button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NothingSurfaceVariant,
                            border = BorderStroke(1.dp, NothingBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    triggerSnap()
                                    onClose()
                                }
                        ) {
                            Text(
                                text = "✕ CLOSE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Blueprint etched divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NothingBorder)
                    )

                    // Scrollable Shelf Racks
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (shelfWidgets.isEmpty()) {
                            // Empty Shelf Slot
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = NothingDarkBackground,
                                border = BorderStroke(1.dp, NothingBorder.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NothingTextTertiary)
                                    )
                                    Text(
                                        text = "ALL WIDGETS ARE DEPLOYED",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = NothingTextSecondary
                                    )
                                    Text(
                                        text = "ENTER EDIT MODE ON ANY PAGE AND TAP [SHELF ↘] TO STASH WIDGETS HERE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = NothingTextTertiary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        } else {
                            shelfWidgets.forEachIndexed { index, widgetId ->
                                ShelfRackItem(
                                    rackIndex = index + 1,
                                    widgetId = widgetId,
                                    currentPageIndex = currentPageIndex,
                                    onDeploy = { targetPage ->
                                        triggerSnap()
                                        onDeployWidget(widgetId, targetPage)
                                    },
                                    tasks = tasks,
                                    onToggleTask = onToggleTask,
                                    onClearDoneTasks = onClearDoneTasks,
                                    nextTask = nextTask,
                                    buddyXp = buddyXp,
                                    buddyLevel = buddyLevel,
                                    notes = notes,
                                    onExpandNotes = onExpandNotes,
                                    onQuickCreateNote = onQuickCreateNote
                                )
                            }
                        }
                    }

                    // Bottom Drawer Pull Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                triggerSnap()
                                onClose()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x44FFFFFF))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Shelf Rack displaying an authentic 3D metallic shelf ledge
 * with the widget seated on top and deployment actions on the rack header.
 */
@Composable
private fun ShelfRackItem(
    rackIndex: Int,
    widgetId: DashboardWidgetId,
    currentPageIndex: Int,
    onDeploy: (Int) -> Unit,
    tasks: List<ReminderTask>,
    onToggleTask: (ReminderTask) -> Unit,
    onClearDoneTasks: () -> Unit,
    nextTask: ReminderTask?,
    buddyXp: Int,
    buddyLevel: Int,
    notes: List<NoteItem>,
    onExpandNotes: () -> Unit,
    onQuickCreateNote: () -> Unit
) {
    val activePageTitle = when (currentPageIndex) {
        0 -> "LEFT"
        1 -> "MAIN"
        else -> "RIGHT"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Rack Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "RACK 0$rackIndex",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed
                )
                Text(
                    text = "// ${widgetId.title.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            }

            // Deployment Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary quick deploy to active page
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NothingRed.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDeploy(currentPageIndex) }
                ) {
                    Text(
                        text = "+ DEPLOY TO $activePageTitle",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // If not already deploying to left or main, provide quick target pills
                if (currentPageIndex != 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDeploy(0) }
                    ) {
                        Text(
                            text = "LEFT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                if (currentPageIndex != 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDeploy(1) }
                    ) {
                        Text(
                            text = "MAIN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Widget Slot on the Shelf
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            when (widgetId) {
                DashboardWidgetId.WATER -> {
                    WaterTrackerWidget(size = WidgetSize.WIDE)
                }
                DashboardWidgetId.NOTES -> {
                    NotesCollapsedWidget(
                        notes = notes,
                        size = WidgetSize.WIDE,
                        onExpand = onExpandNotes,
                        onQuickCreate = onQuickCreateNote
                    )
                }
                DashboardWidgetId.TODAY -> {
                    TodayWidget(
                        tasks = tasks,
                        onToggleTask = onToggleTask,
                        onClearDoneTasks = onClearDoneTasks,
                        size = WidgetSize.WIDE
                    )
                }
                DashboardWidgetId.NEXT -> {
                    NextWidget(
                        nextTask = nextTask,
                        size = WidgetSize.WIDE
                    )
                }
                DashboardWidgetId.BUDDY -> {
                    BuddyWidget(
                        xp = buddyXp,
                        level = buddyLevel,
                        size = WidgetSize.WIDE
                    )
                }
                DashboardWidgetId.CLOCK -> {
                    ClockWidget(size = WidgetSize.MAX)
                }
            }
        }

        // 3D Industrial Metallic Shelf Ledge / Plank underneath the widget
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF383838), // top metallic highlight
                            Color(0xFF222222), // beam face
                            Color(0xFF101010)  // underside shadow
                        )
                    )
                )
        ) {
            // Bevel highlight rim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x66FFFFFF))
                    .align(Alignment.TopCenter)
            )

            // Corner rivets / bolts on the shelf ledge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color(0x88FFFFFF))
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color(0x88FFFFFF))
                )
            }
        }
    }
}

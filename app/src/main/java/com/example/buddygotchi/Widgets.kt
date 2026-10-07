package com.example.buddygotchi

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.NothingRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun TodayWidget(
    tasks: List<ReminderTask> = emptyList(),
    onToggleTask: (ReminderTask) -> Unit = {},
    onClearDoneTasks: () -> Unit = {},
    size: WidgetSize = WidgetSize.WIDE,
    height: Dp = when (size) {
        WidgetSize.CUBE, WidgetSize.SLIM -> 88.dp
        WidgetSize.HALF -> 120.dp
        WidgetSize.WIDE -> 180.dp
        WidgetSize.MAX, WidgetSize.TALL -> 276.dp
    },
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val hasCompleted = remember(tasks) { tasks.any { it.isCompleted } }
    val activeTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val nextActiveTask = remember(activeTasks) {
        val now = System.currentTimeMillis()
        activeTasks.filter { it.taskTime.time >= now }.minByOrNull { it.taskTime.time }
            ?: activeTasks.minByOrNull { it.taskTime.time }
    }

    WidgetCard(
        title = "TODAY",
        height = height,
        size = size,
        modifier = modifier,
        headerAction = {
            if (hasCompleted && (size == WidgetSize.WIDE || size == WidgetSize.MAX || size == WidgetSize.SLIM)) {
                Text(
                    text = "CLEAR DONE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClearDoneTasks() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    ) {
        when (size) {
            WidgetSize.CUBE -> {
                // Compact 1x1 Cube Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${activeTasks.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTasks.isEmpty()) MaterialTheme.colorScheme.outline else NothingRed,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = if (activeTasks.isEmpty()) "ALL DONE" else "TASKS",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (nextActiveTask != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeFormat.format(nextActiveTask.taskTime),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            WidgetSize.HALF -> {
                // Compact 2x1 Half-width Layout
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No tasks today",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tasks.take(2).forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable { onToggleTask(task) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timeFormat.format(task.taskTime),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (task.isCompleted) "✓" else task.category.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            WidgetSize.SLIM -> {
                // 4x1 Full-width Slim Banner Layout (88.dp)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${activeTasks.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTasks.isEmpty()) MaterialTheme.colorScheme.outline else NothingRed
                        )
                        Column {
                            Text(
                                text = if (activeTasks.isEmpty()) "ALL TASKS DONE" else "PENDING TODAY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (nextActiveTask != null) {
                                Text(
                                    text = "Next: ${timeFormat.format(nextActiveTask.taskTime)} (${nextActiveTask.category.label})",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            WidgetSize.TALL -> {
                // 1x4 Vertical Strip Layout (88.dp width, 276.dp height)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${activeTasks.size}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTasks.isEmpty()) MaterialTheme.colorScheme.outline else NothingRed
                        )
                        Text(
                            text = if (activeTasks.isEmpty()) "DONE" else "TASKS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (tasks.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            tasks.take(4).forEach { task ->
                                Text(
                                    text = timeFormat.format(task.taskTime),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    if (hasCompleted) {
                        Text(
                            text = "CLEAR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClearDoneTasks() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            WidgetSize.WIDE, WidgetSize.MAX -> {
                // Expanded 4x2 or 4x3 MAX Layout
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No reminders yet",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = if (size == WidgetSize.MAX) 16.sp else 14.sp
                            )
                            if (size == WidgetSize.MAX) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Scroll down to add a new task",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            val todayCal = Calendar.getInstance().apply { time = Date() }
                            val taskCal = Calendar.getInstance().apply { time = task.taskTime }

                            val isToday = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                                    todayCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)

                            val isTomorrow = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                                    todayCal.get(Calendar.DAY_OF_YEAR) + 1 == taskCal.get(Calendar.DAY_OF_YEAR)

                            val dayLabel = when {
                                isToday -> ""
                                isTomorrow -> "Tomorrow "
                                else -> "${dateFormat.format(task.taskTime)} "
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable { onToggleTask(task) }
                                    .padding(horizontal = 12.dp, vertical = if (size == WidgetSize.MAX) 10.dp else 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dayLabel.isNotEmpty()) {
                                            Text(
                                                text = dayLabel,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = timeFormat.format(task.taskTime),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (size == WidgetSize.MAX) 18.sp else 16.sp,
                                            letterSpacing = 1.sp,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (task.offsetMinutes > 0) {
                                        Text(
                                            text = "Alert at ${timeFormat.format(task.reminderTime)} (-${task.offsetMinutes}m)",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Text(
                                    text = if (task.isCompleted) "DONE ✓" else task.category.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NextWidget(
    nextTask: ReminderTask? = null,
    size: WidgetSize = WidgetSize.HALF,
    height: Dp = when (size) {
        WidgetSize.CUBE, WidgetSize.SLIM -> 88.dp
        WidgetSize.HALF -> 120.dp
        WidgetSize.WIDE -> 180.dp
        WidgetSize.MAX, WidgetSize.TALL -> 276.dp
    },
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    WidgetCard(
        title = if (size == WidgetSize.MAX) "UPCOMING REMINDER" else "NEXT",
        height = height,
        size = size,
        modifier = modifier
    ) {
        when (size) {
            WidgetSize.CUBE -> {
                // 1x1 Cube Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (nextTask == null) {
                        Text(
                            text = "FREE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = timeFormat.format(nextTask.taskTime),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextTask.category.label.uppercase(Locale.getDefault()),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            WidgetSize.HALF -> {
                // 2x1 Half-width Layout
                if (nextTask == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nothing upcoming",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    val todayCal = Calendar.getInstance().apply { time = Date() }
                    val taskCal = Calendar.getInstance().apply { time = nextTask.taskTime }
                    val isToday = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!isToday) {
                            Text(
                                text = "Tomorrow",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = timeFormat.format(nextTask.taskTime),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = nextTask.category.label,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            WidgetSize.SLIM -> {
                // 4x1 Full-width Slim Banner Layout (88.dp)
                if (nextTask == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO UPCOMING REMINDERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    val todayCal = Calendar.getInstance().apply { time = Date() }
                    val taskCal = Calendar.getInstance().apply { time = nextTask.taskTime }
                    val isToday = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = timeFormat.format(nextTask.taskTime),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isToday) "TODAY" else "TOMORROW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = nextTask.category.label.uppercase(Locale.getDefault()),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            WidgetSize.TALL -> {
                // 1x4 Vertical Strip Layout (88.dp width, 276.dp height)
                if (nextTask == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FREE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NEXT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeFormat.format(nextTask.taskTime),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = nextTask.category.label.uppercase(Locale.getDefault()),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            WidgetSize.WIDE -> {
                // 4x2 Wide Layout (180.dp)
                if (nextTask == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No upcoming reminders scheduled",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val todayCal = Calendar.getInstance().apply { time = Date() }
                    val taskCal = Calendar.getInstance().apply { time = nextTask.taskTime }
                    val isToday = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isToday) "Today" else "Tomorrow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = timeFormat.format(nextTask.taskTime),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            if (nextTask.offsetMinutes > 0) {
                                Text(
                                    text = "Alert ${nextTask.offsetMinutes}m earlier",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = nextTask.category.label.uppercase(Locale.getDefault()),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            WidgetSize.MAX -> {
                // 4x3 MAX Layout (276.dp, matching Clock)
                if (nextTask == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FREE AND CLEAR",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No pending reminders on schedule",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    val todayCal = Calendar.getInstance().apply { time = Date() }
                    val taskCal = Calendar.getInstance().apply { time = nextTask.taskTime }
                    val isToday = todayCal.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = if (isToday) "TODAY" else "TOMORROW",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = timeFormat.format(nextTask.taskTime),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NothingRed.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = nextTask.category.label.uppercase(Locale.getDefault()),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingRed
                            )
                        }

                        if (nextTask.offsetMinutes > 0) {
                            Text(
                                text = "Alarm notification: ${timeFormat.format(nextTask.reminderTime)} (-${nextTask.offsetMinutes}m)",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class EyeConfig(
    val canvasWidth: Dp,
    val canvasHeight: Dp,
    val eyeRadius: Dp,
    val eyeSpacing: Dp,
    val strokeWidth: Dp,
    val pupilRadius: Dp,
    val maxTravel: Dp
)

private fun getEyeConfig(size: WidgetSize): EyeConfig {
    return when (size) {
        WidgetSize.CUBE, WidgetSize.SLIM, WidgetSize.TALL -> EyeConfig(
            canvasWidth = 38.dp,
            canvasHeight = 18.dp,
            eyeRadius = 7.dp,
            eyeSpacing = 18.dp,
            strokeWidth = 1.8.dp,
            pupilRadius = 3.dp,
            maxTravel = 2.4.dp
        )
        WidgetSize.HALF -> EyeConfig(
            canvasWidth = 56.dp,
            canvasHeight = 26.dp,
            eyeRadius = 11.dp,
            eyeSpacing = 28.dp,
            strokeWidth = 2.2.dp,
            pupilRadius = 4.5.dp,
            maxTravel = 4.2.dp
        )
        WidgetSize.WIDE -> EyeConfig(
            canvasWidth = 72.dp,
            canvasHeight = 34.dp,
            eyeRadius = 14.dp,
            eyeSpacing = 36.dp,
            strokeWidth = 2.8.dp,
            pupilRadius = 6.dp,
            maxTravel = 5.5.dp
        )
        WidgetSize.MAX -> EyeConfig(
            canvasWidth = 110.dp,
            canvasHeight = 50.dp,
            eyeRadius = 22.dp,
            eyeSpacing = 56.dp,
            strokeWidth = 3.5.dp,
            pupilRadius = 9.5.dp,
            maxTravel = 9.5.dp
        )
    }
}

@Composable
fun BuddyEyes(
    size: WidgetSize,
    getTouchPosition: () -> Offset? = { null },
    modifier: Modifier = Modifier
) {
    val config = remember(size) { getEyeConfig(size) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val eyeColor = MaterialTheme.colorScheme.onSurface

    val eyeRadiusPx = with(density) { config.eyeRadius.toPx() }
    val eyeSpacingPx = with(density) { config.eyeSpacing.toPx() }
    val strokeWidthPx = with(density) { config.strokeWidth.toPx() }
    val pupilRadiusPx = with(density) { config.pupilRadius.toPx() }
    val maxTravelPx = with(density) { config.maxTravel.toPx() }
    val minFocusDistancePx = with(density) { 50.dp.toPx() }

    var eyesCenterInRoot by remember { mutableStateOf<Offset?>(null) }
    val activeTouch = getTouchPosition()

    val leftEyeRoot = eyesCenterInRoot?.let { Offset(it.x - eyeSpacingPx / 2f, it.y) }
    val rightEyeRoot = eyesCenterInRoot?.let { Offset(it.x + eyeSpacingPx / 2f, it.y) }

    // Direct tracking when user is touching anywhere on the dashboard
    val touchTargetLeft: Offset? = if (activeTouch != null && leftEyeRoot != null) {
        val dx = activeTouch.x - leftEyeRoot.x
        val dy = activeTouch.y - leftEyeRoot.y
        val dist = hypot(dx, dy)
        val angle = atan2(dy, dx)
        val factor = (dist / minFocusDistancePx).coerceIn(0.2f, 1f)
        Offset(cos(angle) * maxTravelPx * factor, sin(angle) * maxTravelPx * factor)
    } else null

    val touchTargetRight: Offset? = if (activeTouch != null && rightEyeRoot != null) {
        val dx = activeTouch.x - rightEyeRoot.x
        val dy = activeTouch.y - rightEyeRoot.y
        val dist = hypot(dx, dy)
        val angle = atan2(dy, dx)
        val factor = (dist / minFocusDistancePx).coerceIn(0.2f, 1f)
        Offset(cos(angle) * maxTravelPx * factor, sin(angle) * maxTravelPx * factor)
    } else null

    var lastTouchTargetLeft by remember { mutableStateOf(Offset.Zero) }
    var lastTouchTargetRight by remember { mutableStateOf(Offset.Zero) }
    var idleGaze by remember { mutableStateOf(Offset.Zero) }
    var isIdleLingering by remember { mutableStateOf(false) }

    LaunchedEffect(touchTargetLeft, touchTargetRight) {
        if (touchTargetLeft != null) lastTouchTargetLeft = touchTargetLeft
        if (touchTargetRight != null) lastTouchTargetRight = touchTargetRight
    }

    LaunchedEffect(activeTouch == null) {
        if (activeTouch == null) {
            // Linger gaze on the last interaction spot for 600ms before returning to center
            if (lastTouchTargetLeft != Offset.Zero || lastTouchTargetRight != Offset.Zero) {
                isIdleLingering = true
                delay(600)
                isIdleLingering = false
            }
            idleGaze = Offset.Zero

            // Idle look-around loop: glance around naturally every few seconds
            while (true) {
                delay(kotlin.random.Random.nextLong(2000, 4200))
                val gazeChoices = listOf(
                    Offset(0f, 0f),           // Center
                    Offset(0f, 0f),           // Center (higher probability)
                    Offset(0f, 0f),           // Center
                    Offset(-0.7f, 0f),        // Look left
                    Offset(0.7f, 0f),         // Look right
                    Offset(-0.5f, -0.45f),    // Look up-left
                    Offset(0.5f, -0.45f),     // Look up-right
                    Offset(0f, 0.4f),         // Look down
                    Offset(0f, -0.5f)         // Look up
                )
                val choice = gazeChoices.random()
                idleGaze = Offset(choice.x * maxTravelPx, choice.y * maxTravelPx)
            }
        } else {
            isIdleLingering = false
            idleGaze = Offset.Zero
        }
    }

    val targetLeftOffset = when {
        touchTargetLeft != null -> touchTargetLeft
        isIdleLingering -> lastTouchTargetLeft
        else -> idleGaze
    }

    val targetRightOffset = when {
        touchTargetRight != null -> touchTargetRight
        isIdleLingering -> lastTouchTargetRight
        else -> idleGaze
    }

    val pupilAnimSpec = if (activeTouch != null) {
        spring<Offset>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    } else {
        tween<Offset>(
            durationMillis = 380,
            easing = FastOutSlowInEasing
        )
    }

    val animatedLeftOffset by animateOffsetAsState(
        targetValue = targetLeftOffset,
        animationSpec = pupilAnimSpec,
        label = "leftPupilOffset"
    )

    val animatedRightOffset by animateOffsetAsState(
        targetValue = targetRightOffset,
        animationSpec = pupilAnimSpec,
        label = "rightPupilOffset"
    )

    // Blinking animation
    var isBlinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(kotlin.random.Random.nextLong(3000, 6000))
            isBlinking = true
            delay(120)
            isBlinking = false
            if (kotlin.random.Random.nextFloat() < 0.2f) {
                delay(100)
                isBlinking = true
                delay(110)
                isBlinking = false
            }
        }
    }

    val blinkScaleY by animateFloatAsState(
        targetValue = if (isBlinking) 0.06f else 1f,
        animationSpec = tween(durationMillis = 65, easing = FastOutLinearInEasing),
        label = "blinkScaleY"
    )

    Canvas(
        modifier = modifier
            .size(config.canvasWidth, config.canvasHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    isBlinking = true
                    delay(130)
                    isBlinking = false
                }
            }
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                eyesCenterInRoot = Offset(
                    pos.x + coordinates.size.width / 2f,
                    pos.y + coordinates.size.height / 2f
                )
            }
    ) {
        val centerCanvas = Offset(this.size.width / 2f, this.size.height / 2f)
        val leftCenter = Offset(centerCanvas.x - eyeSpacingPx / 2f, centerCanvas.y)
        val rightCenter = Offset(centerCanvas.x + eyeSpacingPx / 2f, centerCanvas.y)

        // Left Eye
        scale(scaleX = 1f, scaleY = blinkScaleY, pivot = leftCenter) {
            drawCircle(
                color = Color(0xFF141414),
                radius = eyeRadiusPx,
                center = leftCenter
            )
            drawCircle(
                color = eyeColor,
                radius = eyeRadiusPx,
                center = leftCenter,
                style = Stroke(width = strokeWidthPx)
            )
            drawCircle(
                color = eyeColor,
                radius = pupilRadiusPx,
                center = leftCenter + animatedLeftOffset
            )
        }

        // Right Eye
        scale(scaleX = 1f, scaleY = blinkScaleY, pivot = rightCenter) {
            drawCircle(
                color = Color(0xFF141414),
                radius = eyeRadiusPx,
                center = rightCenter
            )
            drawCircle(
                color = eyeColor,
                radius = eyeRadiusPx,
                center = rightCenter,
                style = Stroke(width = strokeWidthPx)
            )
            drawCircle(
                color = eyeColor,
                radius = pupilRadiusPx,
                center = rightCenter + animatedRightOffset
            )
        }
    }
}

@Composable
fun BuddyWidget(
    xp: Int = 0,
    level: Int = 1,
    size: WidgetSize = WidgetSize.HALF,
    height: Dp = when (size) {
        WidgetSize.CUBE, WidgetSize.SLIM -> 88.dp
        WidgetSize.HALF -> 120.dp
        WidgetSize.WIDE -> 180.dp
        WidgetSize.MAX, WidgetSize.TALL -> 276.dp
    },
    getTouchPosition: () -> Offset? = { null },
    modifier: Modifier = Modifier
) {
    WidgetCard(
        title = if (size == WidgetSize.MAX) "BUDDYGOTCHI COMPANION" else "BUDDY",
        height = height,
        size = size,
        modifier = modifier
    ) {
        when (size) {
            WidgetSize.CUBE -> {
                // 1x1 Cube Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyEyes(
                        size = size,
                        getTouchPosition = getTouchPosition
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Lv $level",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NothingRed
                    )
                    Text(
                        text = "$xp XP",
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            WidgetSize.HALF -> {
                // 2x1 Half-width Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyEyes(
                        size = size,
                        getTouchPosition = getTouchPosition
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Level $level",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$xp XP",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            WidgetSize.SLIM -> {
                // 4x1 Full-width Slim Banner Layout (88.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BuddyEyes(
                            size = size,
                            getTouchPosition = getTouchPosition
                        )
                        Column {
                            Text(
                                text = "BUDDY • LVL $level",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$xp XP earned",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val progress = ((xp % 50) / 50f).coerceIn(0f, 1f)
                        Text(
                            text = "${(progress * 100).toInt()}% to Lv ${level + 1}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(70.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NothingRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            WidgetSize.TALL -> {
                // 1x4 Vertical Strip Layout (88.dp width, 276.dp height)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyEyes(
                        size = size,
                        getTouchPosition = getTouchPosition
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LVL $level",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NothingRed
                        )
                        Text(
                            text = "$xp XP",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        val progress = ((xp % 50) / 50f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NothingRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Text(
                        text = "BUDDY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            WidgetSize.WIDE -> {
                // 4x2 Wide Layout (180.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BuddyEyes(
                            size = size,
                            getTouchPosition = getTouchPosition
                        )
                        Column {
                            Text(
                                text = "BUDDYGOTCHI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Level $level Companion",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$xp Total XP",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val progress = ((xp % 50) / 50f).coerceIn(0f, 1f)
                        Text(
                            text = "${(progress * 100).toInt()}% to Lv ${level + 1}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NothingRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            WidgetSize.MAX -> {
                // 4x3 MAX Layout (276.dp, matching Clock)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    BuddyEyes(
                        size = size,
                        getTouchPosition = getTouchPosition
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Level $level Companion",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val progress = ((xp % 50) / 50f).coerceIn(0f, 1f)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(160.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NothingRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${xp % 50} / 50 XP to Level ${level + 1}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "TOTAL XP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "$xp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "ACTIVE ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NothingRed)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MOOD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "HAPPY 😊", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
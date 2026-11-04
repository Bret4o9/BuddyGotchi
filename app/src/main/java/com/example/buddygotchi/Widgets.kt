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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

fun getWidgetHeight(size: WidgetSize): Dp = DashboardGridDefaults.getWidgetHeight(size)

@Composable
fun TodayWidget(
    tasks: List<ReminderTask> = emptyList(),
    onToggleTask: (ReminderTask) -> Unit = {},
    onClearDoneTasks: () -> Unit = {},
    size: WidgetSize = WidgetSize.WIDE,
    height: Dp = getWidgetHeight(size),
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
            if (hasCompleted && (size.cols >= 3 || size.rows >= 2)) {
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
        when {
            size.cols == 1 && size.rows == 1 -> {
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

            size.cols == 1 -> {
                // 1x2, 1x3, 1x4 Vertical Strip Layout (88.dp width)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${activeTasks.size}",
                            fontSize = if (size.rows == 2) 22.sp else 26.sp,
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
                            tasks.take(size.rows).forEach { task ->
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

            size.cols == 2 -> {
                // 2x1, 2x2, 2x3, 2x4 Half-width Layout
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
                        val maxTasks = if (size.rows == 1) 1 else (size.rows * 2)
                        tasks.take(maxTasks).forEach { task ->
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
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = timeFormat.format(task.taskTime),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = if (task.description.isNotBlank()) 10.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (task.isRecurring) {
                                            Text(
                                                text = " â†»",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else NothingRed
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (task.isCompleted) "âœ“" else task.category.label,
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

            size.rows == 1 -> {
                // 3x1, 4x1 Full-width Slim Banner Layout
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

            else -> {
                // Expanded 4x2, 4x3, 4x4 or 3x2, 3x3, 3x4 Layout
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No reminders yet",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = if (size.rows >= 3) 16.sp else 14.sp
                            )
                            if (size.rows >= 3) {
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
                    val todayCal = remember(tasks) { Calendar.getInstance().apply { time = Date() } }
                    val todayYear = todayCal.get(Calendar.YEAR)
                    val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            val dayLabel = remember(task.taskTime, todayYear, todayDayOfYear) {
                                val taskCal = Calendar.getInstance().apply { time = task.taskTime }
                                val taskYear = taskCal.get(Calendar.YEAR)
                                val taskDay = taskCal.get(Calendar.DAY_OF_YEAR)
                                val isToday = todayYear == taskYear && todayDayOfYear == taskDay
                                val isTomorrow = todayYear == taskYear && todayDayOfYear + 1 == taskDay
                                when {
                                    isToday -> ""
                                    isTomorrow -> "Tomorrow "
                                    else -> "${dateFormat.format(task.taskTime)} "
                                }
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
                                    .padding(horizontal = 12.dp, vertical = if (size.rows >= 3) 10.dp else 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            fontSize = if (size.rows >= 3) 15.sp else 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
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
                                            fontSize = if (task.description.isNotBlank()) 13.sp else if (size.rows >= 3) 18.sp else 16.sp,
                                            letterSpacing = 1.sp,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (task.isRecurring) {
                                            Text(
                                                text = "  â†»",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else NothingRed
                                            )
                                        }
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
                                    text = if (task.isCompleted) "DONE âœ“" else task.category.label,
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
    height: Dp = getWidgetHeight(size),
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    WidgetCard(
        title = if (size.rows >= 3) "UPCOMING REMINDER" else "NEXT",
        height = height,
        size = size,
        modifier = modifier
    ) {
        when {
            size.cols == 1 && size.rows == 1 -> {
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

            size.cols == 1 -> {
                // 1x2, 1x3, 1x4 Vertical Strip Layout (88.dp width)
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
                                fontSize = if (size.rows == 2) 17.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (nextTask.description.isNotBlank() && size.rows >= 3) {
                                Text(
                                    text = nextTask.description,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = if (nextTask.isRecurring) "â†» ${nextTask.category.label}" else nextTask.category.label.uppercase(Locale.getDefault()),
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

            size.cols == 2 -> {
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
                        if (nextTask.description.isNotBlank()) {
                            Text(
                                text = nextTask.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (!isToday) {
                            Text(
                                text = "Tomorrow",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = timeFormat.format(nextTask.taskTime),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nextTask.category.label,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (nextTask.isRecurring) {
                                Text(
                                    text = " â€¢ â†»",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingRed
                                )
                            }
                        }
                    }
                }
            }

            size.rows == 1 -> {
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
                            Column {
                                if (nextTask.description.isNotBlank()) {
                                    Text(
                                        text = nextTask.description,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (isToday) "TODAY" else "TOMORROW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = if (nextTask.isRecurring) "â†» ${nextTask.category.label}" else nextTask.category.label.uppercase(Locale.getDefault()),
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

            size.rows == 2 -> {
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
                            if (nextTask.description.isNotBlank()) {
                                Text(
                                    text = nextTask.description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isToday) "Today" else "Tomorrow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                if (nextTask.isRecurring) {
                                    Text(
                                        text = "â†» RECURRING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingRed
                                    )
                                }
                            }
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

            else -> {
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
    return when {
        size.cols == 1 || size.rows == 1 -> EyeConfig(
            canvasWidth = 38.dp,
            canvasHeight = 18.dp,
            eyeRadius = 7.dp,
            eyeSpacing = 18.dp,
            strokeWidth = 1.8.dp,
            pupilRadius = 3.dp,
            maxTravel = 2.4.dp
        )
        size.cols == 2 && size.rows == 2 -> EyeConfig(
            canvasWidth = 56.dp,
            canvasHeight = 26.dp,
            eyeRadius = 11.dp,
            eyeSpacing = 28.dp,
            strokeWidth = 2.2.dp,
            pupilRadius = 4.5.dp,
            maxTravel = 4.2.dp
        )
        size.rows == 2 -> EyeConfig(
            canvasWidth = 72.dp,
            canvasHeight = 34.dp,
            eyeRadius = 14.dp,
            eyeSpacing = 36.dp,
            strokeWidth = 2.8.dp,
            pupilRadius = 6.dp,
            maxTravel = 5.5.dp
        )
        else -> EyeConfig(
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
    val context = LocalContext.current
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
            .semantics { contentDescription = "Interactive 2D Buddy Eyes Companion. Tap to blink." }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                TaskManager.petBuddy(context)
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
private fun BuddyCompanionVisual(
    size: WidgetSize,
    displayMode: BuddyDisplayMode,
    getTouchPosition: () -> Offset? = { null },
    modifier: Modifier = Modifier
) {
    if (displayMode == BuddyDisplayMode.CLASSIC_2D) {
        BuddyEyes(
            size = size,
            getTouchPosition = getTouchPosition,
            modifier = modifier
        )
    } else {
        Buddy3DView(
            size = size,
            getTouchPosition = getTouchPosition,
            modifier = modifier
        )
    }
}

@Composable
fun BuddyWidget(
    xp: Int = 0,
    level: Int = 1,
    size: WidgetSize = WidgetSize.HALF,
    height: Dp = getWidgetHeight(size),
    getTouchPosition: () -> Offset? = { null },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Buddy3DManager.ensureInitialized(context)
        AccountAndInventoryManager.ensureInitialized(context)
    }
    val displayMode by Buddy3DManager.displayModeState
    val equippedPetId by AccountAndInventoryManager.equippedPetIdState
    val petNicknames = AccountAndInventoryManager.petNicknamesState
    val buddyNickname = petNicknames[equippedPetId]?.takeIf { it.isNotBlank() } ?: "Companion"
    var show3DStudioDialog by remember { mutableStateOf(false) }
    var showNicknameEditDialog by remember { mutableStateOf(false) }

    if (show3DStudioDialog) {
        Buddy3DStudioDialog(onDismiss = { show3DStudioDialog = false })
    }

    if (showNicknameEditDialog) {
        BuddyNicknameEditDialog(
            currentNickname = if (buddyNickname == "Companion") "" else buddyNickname,
            onDismiss = { showNicknameEditDialog = false },
            onSave = { newName ->
                AccountAndInventoryManager.setPetNickname(context, newName, equippedPetId)
            }
        )
    }

    WidgetCard(
        title = if (size.rows >= 3 && size.cols >= 2) "BUDDYGOTCHI" else "BUDDY",
        height = height,
        size = size,
        modifier = modifier,
        headerAction = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1F1F1F))
                    .border(0.8.dp, Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
                    .clickable { show3DStudioDialog = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .semantics { contentDescription = "Open Buddy Studio" }
            ) {
                Text(
                    text = "Buddy Studio",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    ) {
        when {
            size.cols == 1 && size.rows == 1 -> {
                // 1x1 Cube Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyCompanionVisual(
                        size = size,
                        displayMode = displayMode,
                        getTouchPosition = getTouchPosition
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lv $level",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        color = NothingRed
                    )
                }
            }

            size.cols == 1 -> {
                // 1x2, 1x3, 1x4 Vertical Strip Layout (88.dp width)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = if (size.rows == 2) 4.dp else 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyCompanionVisual(
                        size = size,
                        displayMode = displayMode,
                        getTouchPosition = getTouchPosition
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "LVL $level",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (size.rows == 2) 11.sp else 13.sp,
                            color = NothingRed
                        )
                        Text(
                            text = "$xp XP",
                            fontSize = if (size.rows == 2) 9.sp else 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (size.rows >= 3) {
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
                    }
                    Text(
                        text = if (displayMode == BuddyDisplayMode.THREE_D) Buddy3DManager.getActiveCharacterLabel() else "BUDDY",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            size.cols == 2 -> {
                // 2x1 or 2x2 Half-width Layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BuddyCompanionVisual(
                        size = size,
                        displayMode = displayMode,
                        getTouchPosition = getTouchPosition
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Level $level",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "â€¢ $xp XP",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            size.rows == 1 -> {
                // 3x1, 4x1 Full-width Slim Banner Layout (88.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BuddyCompanionVisual(
                            size = size,
                            displayMode = displayMode,
                            getTouchPosition = getTouchPosition
                        )
                        Column {
                            Text(
                                text = "BUDDY â€¢ LVL $level",
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

            size.rows == 2 -> {
                // 3x2, 4x2 Wide Layout (180.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BuddyCompanionVisual(
                            size = size,
                            displayMode = displayMode,
                            getTouchPosition = getTouchPosition
                        )
                        Column {
                            Text(
                                text = if (displayMode == BuddyDisplayMode.THREE_D) Buddy3DManager.getActiveCharacterLabel() else "BUDDYGOTCHI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingRed
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showNicknameEditDialog = true }
                                    .semantics { contentDescription = "Edit Buddy Nickname" }
                            ) {
                                Text(
                                    text = "Level $level $buddyNickname",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                PenEditIcon(
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
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

            else -> {
                // 4x3 MAX Layout (276.dp, matching Clock)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    BuddyCompanionVisual(
                        size = size,
                        displayMode = displayMode,
                        getTouchPosition = getTouchPosition
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showNicknameEditDialog = true }
                                .semantics { contentDescription = "Edit Buddy Nickname" }
                        ) {
                            Text(
                                text = "Level $level $buddyNickname",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            PenEditIcon(
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        val progress = ((xp % 50) / 50f).coerceIn(0f, 1f)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(160.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NothingRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
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
                            Text(text = "$xp", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "CHARACTER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = if (displayMode == BuddyDisplayMode.THREE_D) Buddy3DManager.getActiveCharacterLabel() else "CLASSIC 2D",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MOOD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "HAPPY \uD83D\uDE0A", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PenEditIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.secondary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scaleX = w / 24f
        val scaleY = h / 24f

        val bodyPath = Path().apply {
            moveTo(3f * scaleX, 17.25f * scaleY)
            lineTo(3f * scaleX, 21f * scaleY)
            lineTo(6.75f * scaleX, 21f * scaleY)
            lineTo(17.81f * scaleX, 9.94f * scaleY)
            lineTo(14.06f * scaleX, 6.19f * scaleY)
            close()
        }
        drawPath(bodyPath, color = tint)

        val capPath = Path().apply {
            moveTo(20.71f * scaleX, 7.04f * scaleY)
            lineTo(18.37f * scaleX, 4.7f * scaleY)
            lineTo(16.03f * scaleX, 7.04f * scaleY)
            lineTo(18.37f * scaleX, 9.38f * scaleY)
            close()
        }
        drawPath(capPath, color = tint)
    }
}

@Composable
fun BuddyNicknameEditDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nicknameText by remember { mutableStateOf(currentNickname) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141414),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C2C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BUDDY NICKNAME",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White
                        )
                        Text(
                            text = "SET A CUSTOM NAME FOR YOUR BUDDY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF8E8E8E)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", fontSize = 12.sp, color = Color.White)
                    }
                }

                // Input Box
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NICKNAME",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    BasicTextField(
                        value = nicknameText,
                        onValueChange = { if (it.length <= 20) nicknameText = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1F1F1F))
                                    .border(1.dp, Color(0xFF383838), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                if (nicknameText.isEmpty()) {
                                    Text(
                                        text = "Companion (Default)",
                                        color = Color(0xFF6E6E6E),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (nicknameText.isBlank()) "Default: Companion" else "Custom nickname",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${nicknameText.length} / 20",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222222))
                            .clickable { nicknameText = "" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RESET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAAAAAA)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                onSave(nicknameText)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SAVE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

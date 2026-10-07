package com.example.buddygotchi

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.Dseg7FontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ClockStage {
    TIME,
    OFFSET,
    CATEGORY,
    DONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockWidget(
    onTaskCreated: (ReminderTask) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerTick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun triggerTriplePulse() {
        try {
            val timings = longArrayOf(0, 45, 60, 45, 60, 45)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun triggerConfirmPulse() {
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    var stage by remember { mutableStateOf(ClockStage.TIME) }
    var baseDate by remember { mutableStateOf(Date()) }
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Start with current live time
    val initialCal = remember { Calendar.getInstance() }
    var hourFloat by remember { mutableFloatStateOf(initialCal.get(Calendar.HOUR_OF_DAY).toFloat()) }
    var minuteFloat by remember { mutableFloatStateOf(initialCal.get(Calendar.MINUTE).toFloat()) }
    var lastTickedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var lastTickedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    val hourVelocityTracker = remember { VelocityTracker() }
    val minuteVelocityTracker = remember { VelocityTracker() }

    val offsetOptions = remember { listOf(0, 5, 10, 15, 30, 45, 60) }
    var offsetIndex by remember { mutableIntStateOf(3) } // default 15 min
    var offsetDragAccumulator by remember { mutableFloatStateOf(0f) }

    var selectedCategory by remember { mutableStateOf(TaskCategory.WORK) }

    val selectedOffsetMinutes = offsetOptions[offsetIndex]

    // Selected integers wrapped safely
    val currentSelectedHour = ((hourFloat.roundToInt() % 24) + 24) % 24
    val currentSelectedMinute = ((minuteFloat.roundToInt() % 60) + 60) % 60

    val effectiveCal = Calendar.getInstance().apply {
        time = baseDate
        add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        set(Calendar.HOUR_OF_DAY, currentSelectedHour)
        set(Calendar.MINUTE, currentSelectedMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Auto-advance to tomorrow if selected time is earlier today
    val isAutoTomorrow = remember(effectiveCal.time, selectedDayOffset) {
        selectedDayOffset == 0 && effectiveCal.time.before(baseDate)
    }

    val finalCal = Calendar.getInstance().apply {
        time = effectiveCal.time
        if (isAutoTomorrow) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val displayedTime = String.format(Locale.getDefault(), "%02d:%02d", currentSelectedHour, currentSelectedMinute)

    val displayedDateLabel = remember(selectedDayOffset, isAutoTomorrow, finalCal.time) {
        val effectiveOffset = if (isAutoTomorrow) selectedDayOffset + 1 else selectedDayOffset
        when (effectiveOffset) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(finalCal.time)
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = finalCal.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosenCal = Calendar.getInstance().apply { timeInMillis = millis }
                            val todayCal = Calendar.getInstance().apply { time = baseDate }
                            val diffDays = ((chosenCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            selectedDayOffset = diffDays.coerceAtLeast(0)
                            triggerTick()
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    WidgetCard(
        title = "",
        height = 276.dp
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ClockStageTransition"
        ) { currentStage ->
            when (currentStage) {
                ClockStage.TIME -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top: Date strip with strictly "Today", "Tomorrow", "More"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isTodaySelected = selectedDayOffset == 0 && !isAutoTomorrow
                            val isTomorrowSelected = (selectedDayOffset == 1 && !isAutoTomorrow) || isAutoTomorrow
                            val isMoreSelected = selectedDayOffset > 1 && !isAutoTomorrow

                            // 1. Today
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTodaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedDayOffset = 0
                                        triggerTick()
                                    }
                            ) {
                                Text(
                                    text = "Today",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTodaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. Tomorrow
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTomorrowSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedDayOffset = 1
                                        triggerTick()
                                    }
                            ) {
                                Text(
                                    text = "Tomorrow",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isTomorrowSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTomorrowSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 3. More
                            val moreLabel = if (isMoreSelected) {
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(finalCal.time)
                            } else {
                                "More"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMoreSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showDatePickerDialog = true
                                        triggerTick()
                                    }
                            ) {
                                Text(
                                    text = moreLabel,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isMoreSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Middle: Split 3D Rotating Drum Wheel (Hours on Left, Minutes on Right)
                        val itemHeightDp = 48.dp
                        val itemHeightPx = with(density) { itemHeightDp.toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Guide brackets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "›",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                                Text(
                                    text = "‹",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                )
                            }

                            // Split Drum Columns Row
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // --- Left Column: HOURS (00..23) ---
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { hourVelocityTracker.resetTracking() },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    hourVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                                    val deltaY = dragAmount.y
                                                    val speed = abs(deltaY)
                                                    // Continuous dynamic acceleration: 1.0x for slow precision, scaling smoothly up to 2.8x on fast flicks
                                                    val excessSpeed = (speed - 5f).coerceAtLeast(0f)
                                                    val acceleration = (1.0f + (excessSpeed / 14f)).coerceIn(1.0f, 2.8f)

                                                    // Calibrated base speed: 44.dp (~120px) per hour for calm, controlled single-digit adjustments
                                                    val basePxPerHour = with(density) { 44.dp.toPx() }
                                                    val hourDelta = (-deltaY / basePxPerHour) * acceleration
                                                    hourFloat += hourDelta

                                                    val h = ((hourFloat.roundToInt() % 24) + 24) % 24
                                                    if (h != lastTickedHour) {
                                                        triggerTick()
                                                        lastTickedHour = h
                                                    }
                                                },
                                                onDragEnd = {
                                                    val velocity = hourVelocityTracker.calculateVelocity().y
                                                    val flingHours = if (abs(velocity) > 600f) {
                                                        val sign = if (velocity < 0) 1f else -1f
                                                        val excessVelocity = abs(velocity) - 600f
                                                        sign * (excessVelocity / 900f).coerceIn(0f, 6f)
                                                    } else {
                                                        0f
                                                    }
                                                    val targetHour = (hourFloat + flingHours).roundToInt()

                                                    coroutineScope.launch {
                                                        Animatable(hourFloat).animateTo(
                                                            targetValue = targetHour.toFloat(),
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        ) {
                                                            hourFloat = this.value
                                                            val h = ((hourFloat.roundToInt() % 24) + 24) % 24
                                                            if (h != lastTickedHour) {
                                                                triggerTick()
                                                                lastTickedHour = h
                                                            }
                                                        }
                                                        hourFloat = targetHour.toFloat()
                                                        triggerConfirmPulse()
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val centerIntHour = hourFloat.roundToInt()
                                    val subHourFraction = hourFloat - centerIntHour

                                    for (k in -2..2) {
                                        val relPos = k - subHourFraction
                                        val yOffset = relPos * itemHeightPx
                                        val slotHour = (((centerIntHour + k) % 24) + 24) % 24
                                        val slotHourString = String.format(Locale.getDefault(), "%02d", slotHour)

                                        val absDist = abs(relPos)
                                        val scale = (1f - absDist * 0.15f).coerceIn(0.70f, 1f)
                                        val alpha = (1f - absDist * 0.42f).coerceIn(0.08f, 1f)
                                        val rotX = (-relPos * 25f).coerceIn(-65f, 65f)

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
                                            Text(
                                                text = slotHourString,
                                                fontFamily = Dseg7FontFamily,
                                                fontSize = if (k == 0) 58.sp else 36.sp,
                                                fontWeight = if (k == 0) FontWeight.Bold else FontWeight.Normal,
                                                letterSpacing = 2.sp,
                                                color = if (k == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // --- Center Colon Divider ---
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ":",
                                        fontFamily = Dseg7FontFamily,
                                        fontSize = 50.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                // --- Right Column: MINUTES (00..59) ---
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { minuteVelocityTracker.resetTracking() },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    minuteVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                                    val deltaY = dragAmount.y
                                                    val speed = abs(deltaY)
                                                    // Continuous dynamic acceleration: 1.0x for slow single-minute precision, scaling smoothly up to 3.2x on fast swipes
                                                    val excessSpeed = (speed - 5f).coerceAtLeast(0f)
                                                    val acceleration = (1.0f + (excessSpeed / 12f)).coerceIn(1.0f, 3.2f)

                                                    // Calibrated base speed: 16.dp (~44px) per minute for precise single-minute dialing
                                                    val basePxPerMinute = with(density) { 16.dp.toPx() }
                                                    val minuteDelta = (-deltaY / basePxPerMinute) * acceleration
                                                    minuteFloat += minuteDelta

                                                    val m = ((minuteFloat.roundToInt() % 60) + 60) % 60
                                                    if (m != lastTickedMinute) {
                                                        triggerTick()
                                                        lastTickedMinute = m
                                                    }
                                                },
                                                onDragEnd = {
                                                    val velocity = minuteVelocityTracker.calculateVelocity().y
                                                    val flingMinutes = if (abs(velocity) > 600f) {
                                                        val sign = if (velocity < 0) 1f else -1f
                                                        val excessVelocity = abs(velocity) - 600f
                                                        sign * (excessVelocity / 240f).coerceIn(0f, 15f)
                                                    } else {
                                                        0f
                                                    }
                                                    val targetMinute = (minuteFloat + flingMinutes).roundToInt()

                                                    coroutineScope.launch {
                                                        Animatable(minuteFloat).animateTo(
                                                            targetValue = targetMinute.toFloat(),
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        ) {
                                                            minuteFloat = this.value
                                                            val m = ((minuteFloat.roundToInt() % 60) + 60) % 60
                                                            if (m != lastTickedMinute) {
                                                                triggerTick()
                                                                lastTickedMinute = m
                                                            }
                                                        }
                                                        minuteFloat = targetMinute.toFloat()
                                                        triggerConfirmPulse()
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val centerIntMinute = minuteFloat.roundToInt()
                                    val subMinuteFraction = minuteFloat - centerIntMinute

                                    for (k in -2..2) {
                                        val relPos = k - subMinuteFraction
                                        val yOffset = relPos * itemHeightPx
                                        val slotMinute = (((centerIntMinute + k) % 60) + 60) % 60
                                        val slotMinuteString = String.format(Locale.getDefault(), "%02d", slotMinute)

                                        val absDist = abs(relPos)
                                        val scale = (1f - absDist * 0.15f).coerceIn(0.70f, 1f)
                                        val alpha = (1f - absDist * 0.42f).coerceIn(0.08f, 1f)
                                        val rotX = (-relPos * 25f).coerceIn(-65f, 65f)

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
                                            Text(
                                                text = slotMinuteString,
                                                fontFamily = Dseg7FontFamily,
                                                fontSize = if (k == 0) 58.sp else 36.sp,
                                                fontWeight = if (k == 0) FontWeight.Bold else FontWeight.Normal,
                                                letterSpacing = 2.sp,
                                                color = if (k == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Top & Bottom gradient fades seamlessly blending with card surface
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Explicit Confirmation Button (Finger lift keeps time intact; explicit tap confirms)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    triggerTriplePulse()
                                    stage = ClockStage.OFFSET
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$displayedDateLabel • $displayedTime",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "CONFIRM TIME →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                ClockStage.OFFSET -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "← Back to Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { stage = ClockStage.TIME }
                                    .padding(4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { _, dragAmount ->
                                            val delta = if (abs(dragAmount.y) > abs(dragAmount.x)) -dragAmount.y else -dragAmount.x
                                            offsetDragAccumulator += delta
                                            val threshold = 30f

                                            if (offsetDragAccumulator > threshold) {
                                                if (offsetIndex < offsetOptions.lastIndex) {
                                                    offsetIndex += 1
                                                    triggerTick()
                                                }
                                                offsetDragAccumulator = 0f
                                            } else if (offsetDragAccumulator < -threshold) {
                                                if (offsetIndex > 0) {
                                                    offsetIndex -= 1
                                                    triggerTick()
                                                }
                                                offsetDragAccumulator = 0f
                                            }
                                        },
                                        onDragEnd = {
                                            triggerConfirmPulse()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "ALERT OFFSET",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedOffsetMinutes == 0) "At Time (0m)" else "$selectedOffsetMinutes min before",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Swipe up/down to adjust",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Next button to Category
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    triggerConfirmPulse()
                                    stage = ClockStage.CATEGORY
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Offset: $selectedOffsetMinutes min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "NEXT →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                ClockStage.CATEGORY -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "← Back to Offset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { stage = ClockStage.OFFSET }
                                    .padding(4.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CHOOSE CATEGORY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TaskCategory.entries.forEach { category ->
                                    val isSelected = category == selectedCategory
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selectedCategory = category
                                                triggerConfirmPulse()

                                                val taskTargetTime = finalCal.time
                                                val reminderCal = Calendar.getInstance().apply {
                                                    time = taskTargetTime
                                                    add(Calendar.MINUTE, -selectedOffsetMinutes)
                                                }
                                                val reminderFireTime = reminderCal.time

                                                val task = ReminderTask(
                                                    taskTime = taskTargetTime,
                                                    offsetMinutes = selectedOffsetMinutes,
                                                    reminderTime = reminderFireTime,
                                                    category = category
                                                )
                                                onTaskCreated(task)
                                                stage = ClockStage.DONE
                                            }
                                    ) {
                                        Text(
                                            text = category.label,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }

                ClockStage.DONE -> {
                    LaunchedEffect(Unit) {
                        triggerConfirmPulse()
                        delay(900L)
                        // Reset to current real clock
                        val resetCal = Calendar.getInstance()
                        hourFloat = resetCal.get(Calendar.HOUR_OF_DAY).toFloat()
                        minuteFloat = resetCal.get(Calendar.MINUTE).toFloat()
                        offsetIndex = 3
                        selectedDayOffset = 0
                        baseDate = Date()
                        stage = ClockStage.TIME
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "REMINDER CREATED",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayedTime,
                            fontFamily = Dseg7FontFamily,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$displayedDateLabel • ${selectedCategory.label} (-$selectedOffsetMinutes min)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
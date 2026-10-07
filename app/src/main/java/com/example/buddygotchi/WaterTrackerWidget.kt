package com.example.buddygotchi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.buddygotchi.ui.theme.Dseg7FontFamily
import com.example.buddygotchi.ui.theme.NothingBorder
import com.example.buddygotchi.ui.theme.NothingCardSurface
import com.example.buddygotchi.ui.theme.NothingDarkBackground
import com.example.buddygotchi.ui.theme.NothingRed
import com.example.buddygotchi.ui.theme.NothingSurfaceVariant
import com.example.buddygotchi.ui.theme.NothingTextSecondary
import com.example.buddygotchi.ui.theme.NothingTextTertiary
import com.example.buddygotchi.ui.theme.NothingWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

@Composable
fun WaterTrackerWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var waterState by remember { mutableStateOf(WaterTrackerManager.getWaterState(context)) }
    var showCalculator by remember { mutableStateOf(false) }

    // Pouring state
    var isPouring by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Smooth animated intake progress
    val animatedProgress = remember { Animatable(waterState.percentage) }

    // Synchronize animatedProgress when waterState changes
    LaunchedEffect(waterState.currentMl, waterState.targetMl) {
        val targetP = waterState.percentage
        if (isPouring) {
            animatedProgress.animateTo(
                targetValue = targetP,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
            isPouring = false
        } else {
            animatedProgress.animateTo(
                targetValue = targetP,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NothingCardSurface),
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WATER TRACKER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                }

                // Calculator Button / Weight Indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                        .clickable { showCalculator = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${waterState.userWeightKg} KG ⚙",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Bar: Daily Guideline Recommendation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1400E5FF))
                    .border(0.5.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECOMMENDED: ${waterState.targetGlasses} GLASSES (${waterState.targetMl} ML)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF80D8FF)
                )
                Text(
                    text = "35 ML/KG",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NothingTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Interactive Row: Glass Graphic (Left) + Stats & Progress (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Physics Glass with Tilt & Pouring
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveWaterGlass(
                        fillProgress = animatedProgress.value,
                        isPouring = isPouring,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(115.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Numerical Readouts & Stats Column
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Percentage Readout
                    val displayPct = (animatedProgress.value * 100f).roundToInt()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d", displayPct.coerceAtMost(999)),
                            fontFamily = Dseg7FontFamily,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (displayPct >= 100) Color(0xFF00E5FF) else NothingWhite
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Volume Progress
                    Text(
                        text = "${waterState.currentMl} / ${waterState.targetMl} ML",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingWhite.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Glasses Counter Dots
                    Text(
                        text = "${waterState.glassesCount} OF ${waterState.targetGlasses} GLASSES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dot Matrix Progress Bar for Glasses
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val maxDisplayGlasses = min(waterState.targetGlasses, 12)
                        for (i in 0 until maxDisplayGlasses) {
                            val isFilled = i < waterState.glassesCount
                            Box(
                                modifier = Modifier
                                    .size(width = 8.dp, height = 12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isFilled) Color(0xFF00E5FF) else NothingSurfaceVariant
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isFilled) Color(0xFF80D8FF) else NothingBorder,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Quick Add Water (+250ml glass, +500ml bottle, -250ml, Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary: Add 1 Glass (250 ml) with pouring trigger
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                        .clickable {
                            isPouring = true
                            waterState = WaterTrackerManager.addWater(context, 250)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ 250 ML",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }
                }

                // Secondary: Add Bottle (+500 ml)
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            isPouring = true
                            waterState = WaterTrackerManager.addWater(context, 500)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ 500 ML",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )
                }

                // Undo: -250 ml
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (waterState.currentMl > 0) {
                                waterState = WaterTrackerManager.removeWater(context, 250)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "- 250",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextTertiary
                    )
                }

                // Reset
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            waterState = WaterTrackerManager.resetWater(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextTertiary
                    )
                }
            }
        }
    }

    // Weight & Water Recommendation Calculator Dialog
    if (showCalculator) {
        WaterCalculatorDialog(
            currentWeightKg = waterState.userWeightKg,
            currentTargetMl = waterState.targetMl,
            onDismiss = { showCalculator = false },
            onApply = { weightKg, targetMl ->
                waterState = WaterTrackerManager.updateWeight(context, weightKg)
                if (targetMl != weightKg * 35) {
                    waterState = WaterTrackerManager.updateTargetMl(context, targetMl)
                }
                showCalculator = false
            }
        )
    }
}

/**
 * Interactive Water Glass with tilt physics (accelerometer), slosh spring-damper,
 * pouring stream, bubbles, and clipped fluid meniscus.
 */
@Composable
fun InteractiveWaterGlass(
    fillProgress: Float,
    isPouring: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Physics variables
    var targetSensorAngle by remember { mutableFloatStateOf(0f) }
    var touchTiltOffset by remember { mutableFloatStateOf(0f) }
    var liquidAngle by remember { mutableFloatStateOf(0f) }
    var liquidAngularVel by remember { mutableFloatStateOf(0f) }
    var waveAmplitude by remember { mutableFloatStateOf(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }

    // Register Accelerometer Listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val ax = event.values[0] // tilt roll left/right
                val ay = event.values[1] // tilt pitch up/down

                // Calculate tilt angle in radians relative to vertical (clamped to +/- 50 degrees)
                val angleRad = atan2(ax, ay).coerceIn(-0.85f, 0.85f)
                targetSensorAngle = angleRad
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Main 60FPS Physics Simulation Loop (Harmonic Spring-Damper for realistic water inertia & slosh)
    LaunchedEffect(isPouring) {
        var lastTimeNanos = 0L
        if (isPouring) {
            waveAmplitude = 12f // Extra slosh disturbance on pour
            liquidAngularVel += (Random.nextFloat() - 0.5f) * 1.5f
        }

        while (isActive) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos == 0L) {
                    lastTimeNanos = nowNanos
                    return@withFrameNanos
                }

                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                lastTimeNanos = nowNanos

                val effectiveTargetAngle = (targetSensorAngle + touchTiltOffset).coerceIn(-0.85f, 0.85f)

                // Spring-Mass-Damper parameters for fluid sloshing
                val springK = 50f
                val dampingC = 6.2f
                val angleDiff = liquidAngle - effectiveTargetAngle
                val angularAccel = -springK * angleDiff - dampingC * liquidAngularVel

                liquidAngularVel += angularAccel * dt
                liquidAngle += liquidAngularVel * dt

                // Surface wave excitation from movement & pouring
                val disturbance = abs(angularAccel) * 0.04f + abs(liquidAngularVel) * 1.2f
                waveAmplitude = (waveAmplitude * (1f - dt * 2.5f) + disturbance * 0.15f).coerceIn(0f, 15f)
                wavePhase += dt * 9f
            }
        }
    }

    // Touch Drag gesture on the glass to allow manual swirling / testing
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { touchTiltOffset = 0f },
            onDragCancel = { touchTiltOffset = 0f },
            onDrag = { change, dragAmount ->
                change.consume()
                touchTiltOffset = (touchTiltOffset + dragAmount.x * 0.004f).coerceIn(-0.6f, 0.6f)
                waveAmplitude = (waveAmplitude + abs(dragAmount.x) * 0.15f).coerceIn(0f, 15f)
            }
        )
    }

    // Rising Bubbles State
    val bubbles = remember {
        List(6) { index ->
            BubbleState(
                xFrac = 0.2f + (index * 0.11f),
                speed = 0.4f + (index * 0.12f),
                radius = 2f + (index % 3),
                wobblePhase = index * 1.3f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glassAnimation")
    val bubbleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleTime"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(dragModifier)
    ) {
        val canvasW = size.width
        val canvasH = size.height

        // Glass Silhouette Dimensions
        val glassTopY = 24.dp.toPx()
        val glassBottomY = canvasH - 8.dp.toPx()
        val glassH = glassBottomY - glassTopY
        val glassTopW = canvasW * 0.88f
        val glassBottomW = canvasW * 0.70f

        val topCenterX = canvasW / 2f
        val topLeftX = topCenterX - glassTopW / 2f
        val topRightX = topCenterX + glassTopW / 2f

        val bottomCenterX = canvasW / 2f
        val bottomLeftX = bottomCenterX - glassBottomW / 2f
        val bottomRightX = bottomCenterX + glassBottomW / 2f

        val wallThickness = 3.dp.toPx()
        val baseThickness = 9.dp.toPx()
        val cornerRadius = 14.dp.toPx()

        // 1. Build Outer Glass Outline Path
        val outerPath = Path().apply {
            moveTo(topLeftX, glassTopY)
            lineTo(topRightX, glassTopY)
            lineTo(bottomRightX, glassBottomY - cornerRadius)
            quadraticTo(bottomRightX, glassBottomY, bottomRightX - cornerRadius, glassBottomY)
            lineTo(bottomLeftX + cornerRadius, glassBottomY)
            quadraticTo(bottomLeftX, glassBottomY, bottomLeftX, glassBottomY - cornerRadius)
            close()
        }

        // 2. Build Inner Glass Cavity Path (where water resides)
        val innerTopY = glassTopY + 2.dp.toPx()
        val innerBottomY = glassBottomY - baseThickness
        val innerTopLeftX = topLeftX + wallThickness
        val innerTopRightX = topRightX - wallThickness
        val innerBottomLeftX = bottomLeftX + wallThickness
        val innerBottomRightX = bottomRightX - wallThickness
        val innerCornerRadius = (cornerRadius - wallThickness).coerceAtLeast(4.dp.toPx())

        val innerGlassPath = Path().apply {
            moveTo(innerTopLeftX, innerTopY)
            lineTo(innerTopRightX, innerTopY)
            lineTo(innerBottomRightX, innerBottomY - innerCornerRadius)
            quadraticTo(innerBottomRightX, innerBottomY, innerBottomRightX - innerCornerRadius, innerBottomY)
            lineTo(innerBottomLeftX + innerCornerRadius, innerBottomY)
            quadraticTo(innerBottomLeftX, innerBottomY, innerBottomLeftX, innerBottomY - innerCornerRadius)
            close()
        }

        // Draw Frosted Glass Back Background
        drawPath(
            path = innerGlassPath,
            color = Color(0x1AFFFFFF)
        )

        // 3. Etched Measurement Tick Marks on Glass
        val ticks = listOf(0.25f, 0.50f, 0.75f, 1.00f)
        ticks.forEach { t ->
            val tickY = innerBottomY - (innerBottomY - innerTopY) * t
            val tickWidth = 8.dp.toPx()
            val rightWallX = innerTopRightX + (innerBottomRightX - innerTopRightX) * (1f - t)
            drawLine(
                color = Color(0x44FFFFFF),
                start = Offset(rightWallX - tickWidth, tickY),
                end = Offset(rightWallX, tickY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 4. Draw Liquid inside Glass (clipped strictly to innerGlassPath)
        clipPath(innerGlassPath) {
            val clampedP = fillProgress.coerceIn(0f, 1.2f)

            if (clampedP > 0.005f) {
                val cavityHeight = innerBottomY - innerTopY
                val waterCenterY = innerBottomY - cavityHeight * clampedP.coerceAtMost(1.0f)
                val slope = tan(liquidAngle)

                // Extend beyond glass edges to ensure complete coverage during steep tilts
                val extendedW = canvasW * 1.5f
                val leftX = topCenterX - extendedW
                val rightX = topCenterX + extendedW

                val surfaceYLeft = waterCenterY - extendedW * slope
                val surfaceYRight = waterCenterY + extendedW * slope

                // Wave oscillation across meniscus
                val waveOffset = sin(wavePhase) * waveAmplitude

                // Liquid Body Path
                val waterPath = Path().apply {
                    moveTo(leftX, surfaceYLeft)
                    // Meniscus wave
                    quadraticTo(
                        topCenterX,
                        waterCenterY + waveOffset,
                        rightX,
                        surfaceYRight
                    )
                    lineTo(rightX, canvasH * 2f)
                    lineTo(leftX, canvasH * 2f)
                    close()
                }

                // Vibrant Water Gradient
                val waterBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD900E5FF), // glowing bright cyan surface
                        Color(0xB30091EA), // oceanic blue
                        Color(0x99003366)  // deep water
                    ),
                    startY = waterCenterY - 20f,
                    endY = innerBottomY
                )
                drawPath(path = waterPath, brush = waterBrush)

                // Glowing Specular Meniscus Surface Highlight Line
                val meniscusLine = Path().apply {
                    moveTo(leftX, surfaceYLeft)
                    quadraticTo(
                        topCenterX,
                        waterCenterY + waveOffset,
                        rightX,
                        surfaceYRight
                    )
                }
                drawPath(
                    path = meniscusLine,
                    color = Color(0xFFE0F7FA),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Floating Rising Bubbles
                bubbles.forEach { b ->
                    val bCycle = (bubbleTime * b.speed + b.wobblePhase) % 1.0f
                    val bY = innerBottomY - cavityHeight * clampedP * bCycle
                    val wobbleX = sin(bubbleTime * 4f + b.wobblePhase) * 4.dp.toPx()
                    val bX = innerBottomLeftX + (innerBottomRightX - innerBottomLeftX) * b.xFrac + wobbleX

                    if (bY > waterCenterY + 4.dp.toPx()) {
                        drawCircle(
                            color = Color(0x66FFFFFF),
                            radius = b.radius.dp.toPx(),
                            center = Offset(bX, bY)
                        )
                        // tiny bubble highlight
                        drawCircle(
                            color = Color(0xCCFFFFFF),
                            radius = (b.radius * 0.35f).dp.toPx(),
                            center = Offset(bX - 0.7f, bY - 0.7f)
                        )
                    }
                }
            }
        }

        // 5. Draw Pouring Stream (if pouring)
        if (isPouring) {
            val streamCenterX = topCenterX
            val streamWidth = 5.dp.toPx()
            val cavityHeight = innerBottomY - innerTopY
            val waterCenterY = (innerBottomY - cavityHeight * fillProgress.coerceIn(0f, 1f)).coerceAtLeast(innerTopY)

            // Falling liquid jet from top
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x0000E5FF),
                        Color(0xFF00E5FF),
                        Color(0xFFE0F7FA)
                    ),
                    startY = 0f,
                    endY = waterCenterY
                ),
                topLeft = Offset(streamCenterX - streamWidth / 2f, 0f),
                size = Size(streamWidth, waterCenterY)
            )

            // Splash ripples at point of impact
            val splashWidth = 18.dp.toPx()
            drawOval(
                color = Color(0xAAFFFFFF),
                topLeft = Offset(streamCenterX - splashWidth / 2f, waterCenterY - 3.dp.toPx()),
                size = Size(splashWidth, 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 6. Outer Glass Highlights & Border (Crisp Nothing OS Highball Silhouette)
        drawPath(
            path = outerPath,
            color = Color(0x40FFFFFF),
            style = Stroke(width = wallThickness)
        )

        // Glass Base (Thick heavy bottom rim reflection)
        drawLine(
            color = Color(0x55FFFFFF),
            start = Offset(bottomLeftX + 6.dp.toPx(), glassBottomY - 2.dp.toPx()),
            end = Offset(bottomRightX - 6.dp.toPx(), glassBottomY - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // Subtle left wall specular reflection line
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(topLeftX + 2.dp.toPx(), glassTopY + 8.dp.toPx()),
            end = Offset(bottomLeftX + 2.dp.toPx(), glassBottomY - 14.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private data class BubbleState(
    val xFrac: Float,
    val speed: Float,
    val radius: Float,
    val wobblePhase: Float
)

/**
 * Nothing OS Dialog for calculating recommended water intake based on body weight.
 */
@Composable
fun WaterCalculatorDialog(
    currentWeightKg: Int,
    currentTargetMl: Int,
    onDismiss: () -> Unit,
    onApply: (weightKg: Int, targetMl: Int) -> Unit
) {
    var weight by remember { mutableFloatStateOf(currentWeightKg.toFloat()) }
    val recommendedMl = (weight * 35).roundToInt()
    val glasses = (recommendedMl + 249) / 250

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NothingDarkBackground),
            border = BorderStroke(1.5.dp, NothingBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WATER CALCULATOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NothingWhite
                        )
                    }

                    Text(
                        text = "✕",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextTertiary,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explanatory Note
                Text(
                    text = "Health standards recommend 8 glasses (2,000 ml) daily, or approximately 35 ml per kilogram of body weight for active individuals.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = NothingTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Weight Selector Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "YOUR BODY WEIGHT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NothingTextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${weight.roundToInt()} KG",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = "~${(weight * 2.20462f).roundToInt()} LBS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NothingTextSecondary
                        )
                    }

                    Slider(
                        value = weight,
                        onValueChange = { weight = it },
                        valueRange = 40f..140f,
                        steps = 99,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = NothingBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculation Result
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1A00E5FF))
                        .border(1.dp, Color(0x4D00E5FF), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RECOMMENDED GOAL",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80D8FF)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$recommendedMl ML",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }
                    Text(
                        text = "$glasses GLASSES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingSurfaceVariant)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NothingTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingRed)
                            .clickable {
                                onApply(weight.roundToInt(), recommendedMl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "APPLY TARGET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

@Composable
fun WaterTrackerWidget(
    size: WidgetSize = WidgetSize.MAX,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var waterState by remember { mutableStateOf(WaterTrackerManager.getWaterState(context)) }
    var showCalculator by remember { mutableStateOf(false) }

    // Pouring state
    var isPouring by remember { mutableStateOf(false) }

    // Smooth animated intake progress
    val animatedProgress = remember { Animatable(waterState.percentage) }

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

    val displayPct = (animatedProgress.value * 100f).roundToInt()
    val isCube = size.cols == 1 && size.rows == 1
    val isHalfRow = size.cols == 2 && size.rows == 1
    val isSlim = size.cols == 4 && size.rows == 1
    val isTallStrip = size.cols == 1 && size.rows > 1
    val isHalfBlock = size.cols == 2 && size.rows > 1
    val isCol3Row1 = size.cols == 3 && size.rows == 1

    val cornerRadius = if (isCube) 20.dp else 24.dp
    val internalPadding = when {
        size.cols == 1 -> 8.dp
        size.rows == 1 -> 10.dp
        else -> 14.dp
    }

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = NothingCardSurface),
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardGridDefaults.getWidgetHeight(size))
    ) {
        when {
            // === 1. COMPACT 1x1 CUBE ===
            isCube -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            isPouring = true
                            waterState = WaterTrackerManager.addWater(context, 250)
                        }
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${displayPct}%",
                        fontFamily = Dseg7FontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (displayPct >= 100) Color(0xFF00E5FF) else NothingWhite
                    )

                    InteractiveWaterGlass(
                        fillProgress = animatedProgress.value,
                        isPouring = isPouring,
                        modifier = Modifier.size(width = 30.dp, height = 36.dp)
                    )

                    Text(
                        text = "${waterState.currentMl}ML",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )
                }
            }

            // === 2. VERTICAL STRIP (1x2, 1x3, 1x4) ===
            isTallStrip -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WATER",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingTextSecondary
                        )
                        Text(
                            text = "${displayPct}%",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    InteractiveWaterGlass(
                        fillProgress = animatedProgress.value,
                        isPouring = isPouring,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${waterState.currentMl}ML",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = NothingTextTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(0.5.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                .clickable {
                                    isPouring = true
                                    waterState = WaterTrackerManager.addWater(context, 250)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+250",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }

            // === 3. HALF ROW (2x1, 88dp height) ===
            isHalfRow -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InteractiveWaterGlass(
                        fillProgress = animatedProgress.value,
                        isPouring = isPouring,
                        modifier = Modifier.size(width = 40.dp, height = 58.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${displayPct}%",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (displayPct >= 100) Color(0xFF00E5FF) else NothingWhite
                        )
                        Text(
                            text = "${waterState.currentMl} / ${waterState.targetMl} ML",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = NothingTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                            .clickable {
                                isPouring = true
                                waterState = WaterTrackerManager.addWater(context, 250)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+250",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }
                }
            }

            // === 4. HALF BLOCK (2x2, 2x3, 2x4) ===
            isHalfBlock -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
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
                                    .background(Color(0xFF00E5FF))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WATER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        Text(
                            text = "${waterState.userWeightKg}KG ⚙",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = NothingTextSecondary,
                            modifier = Modifier.clickable { showCalculator = true }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        InteractiveWaterGlass(
                            fillProgress = animatedProgress.value,
                            isPouring = isPouring,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "${displayPct}%",
                                fontFamily = Dseg7FontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "${waterState.currentMl}/${waterState.targetMl} ML",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingTextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                                    .clickable {
                                        isPouring = true
                                        waterState = WaterTrackerManager.addWater(context, 250)
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+250",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NothingSurfaceVariant)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (waterState.currentMl > 0) {
                                            waterState = WaterTrackerManager.removeWater(context, 250)
                                        }
                                    }
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-250",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = NothingTextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // === 5. SLIM BANNER (4x1, 3x1) ===
            isSlim || isCol3Row1 -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InteractiveWaterGlass(
                        fillProgress = animatedProgress.value,
                        isPouring = isPouring,
                        modifier = Modifier.size(width = 40.dp, height = 58.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${displayPct}%",
                                fontFamily = Dseg7FontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (displayPct >= 100) Color(0xFF00E5FF) else NothingWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${waterState.currentMl} / ${waterState.targetMl} ML",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite.copy(alpha = 0.9f)
                            )
                        }
                        Text(
                            text = "${waterState.glassesCount} OF ${waterState.targetGlasses} GLASSES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = NothingTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                                .clickable {
                                    isPouring = true
                                    waterState = WaterTrackerManager.addWater(context, 250)
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ 250 ML",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (waterState.currentMl > 0) {
                                        waterState = WaterTrackerManager.removeWater(context, 250)
                                    }
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "- 250",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }
                }
            }

            // === 6. FULL RICH DISPLAY (4x2, 4x3, 4x4) ===
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween
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

                    // Info Bar: Daily Guideline Recommendation (if rows >= 3)
                    if (size.rows >= 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1400E5FF))
                                .border(0.5.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
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
                    }

                    // Main Interactive Center Row: Glass + Numerical Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(if (size.rows >= 4) 1.25f else 1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            InteractiveWaterGlass(
                                fillProgress = animatedProgress.value,
                                isPouring = isPouring,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(if (size.rows >= 4) 16.dp else 12.dp))

                        Column(
                            modifier = Modifier
                                .weight(if (size.rows >= 4) 1f else 1.1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%02d", displayPct.coerceAtMost(999)),
                                    fontFamily = Dseg7FontFamily,
                                    fontSize = when {
                                        size.rows == 2 -> 28.sp
                                        size.rows >= 4 -> 44.sp
                                        else -> 38.sp
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (displayPct >= 100) Color(0xFF00E5FF) else NothingWhite
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (size.rows >= 4) 18.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(bottom = if (size.rows >= 4) 6.dp else 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(if (size.rows >= 4) 4.dp else 2.dp))

                            Text(
                                text = "${waterState.currentMl} / ${waterState.targetMl} ML",
                                fontFamily = FontFamily.Monospace,
                                fontSize = when {
                                    size.rows == 2 -> 11.sp
                                    size.rows >= 4 -> 13.sp
                                    else -> 12.sp
                                },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingWhite.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(if (size.rows >= 4) 4.dp else 2.dp))

                            Text(
                                text = "${waterState.glassesCount} OF ${waterState.targetGlasses} GLASSES",
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (size.rows >= 4) 10.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextSecondary
                            )

                            if (size.rows >= 3) {
                                Spacer(modifier = Modifier.height(if (size.rows >= 4) 12.dp else 6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(if (size.rows >= 4) 6.dp else 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val maxDisplayGlasses = min(waterState.targetGlasses, 12)
                                    for (i in 0 until maxDisplayGlasses) {
                                        val isFilled = i < waterState.glassesCount
                                        Box(
                                            modifier = Modifier
                                                .size(
                                                    width = if (size.rows >= 4) 10.dp else 8.dp,
                                                    height = if (size.rows >= 4) 16.dp else 12.dp
                                                )
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
                    }

                    // Action Buttons Row
                    val actionButtonHeight = when {
                        size.rows == 2 -> 34.dp
                        size.rows >= 4 -> 42.dp
                        else -> 36.dp
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.4f)
                                .height(actionButtonHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                                .clickable {
                                    isPouring = true
                                    waterState = WaterTrackerManager.addWater(context, 250)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(NothingRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ 250 ML",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (size.rows >= 4) 11.5.sp else 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(actionButtonHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    isPouring = true
                                    waterState = WaterTrackerManager.addWater(context, 500)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ 500 ML",
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (size.rows >= 4) 11.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(actionButtonHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
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
                                fontSize = if (size.rows >= 4) 10.5.sp else 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextTertiary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(actionButtonHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    waterState = WaterTrackerManager.resetWater(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RESET",
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (size.rows >= 4) 10.sp else 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextTertiary
                            )
                        }
                    }
                }
            }
        }
    }

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

    // Main 60FPS Physics Simulation Loop (Harmonic Spring-Damper for realistic fluid sloshing)
    LaunchedEffect(isPouring) {
        var lastTimeNanos = 0L
        if (isPouring) {
            waveAmplitude = 12f
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

                val springK = 50f
                val dampingC = 6.2f
                val angleDiff = liquidAngle - effectiveTargetAngle
                val angularAccel = -springK * angleDiff - dampingC * liquidAngularVel

                liquidAngularVel += angularAccel * dt
                liquidAngle += liquidAngularVel * dt

                val disturbance = abs(angularAccel) * 0.04f + abs(liquidAngularVel) * 1.2f
                waveAmplitude = (waveAmplitude * (1f - dt * 2.5f) + disturbance * 0.15f).coerceIn(0f, 15f)
                wavePhase += dt * 9f
            }
        }
    }

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

        // Strict Aspect Ratio Lock for the Water Glass:
        // TopWidth / Height = 0.65f, BottomWidth / TopWidth = 0.70f / 0.88f (0.795f taper).
        // This guarantees the cup preserves the exact iconic Nothing OS shape seen in 4x1 and 4x3 without squishing or stretching.
        val targetCupAspect = 0.65f
        val maxAvailableHeight = (canvasH - 8.dp.toPx()).coerceAtLeast(10f)
        val maxAvailableTopWidth = (canvasW * 0.88f).coerceAtLeast(10f)

        val (actualCupTopW, actualCupHeight) = if (maxAvailableTopWidth / maxAvailableHeight > targetCupAspect) {
            // Container is wider than the cup aspect ratio (e.g. 4x2): height is the constraint
            val h = maxAvailableHeight
            val w = h * targetCupAspect
            w to h
        } else {
            // Container is taller than the cup aspect ratio (e.g. 4x4, tall strips): width is the constraint
            val w = maxAvailableTopWidth
            val h = w / targetCupAspect
            w to h
        }

        val glassTopY = (canvasH - actualCupHeight) / 2f
        val glassBottomY = glassTopY + actualCupHeight
        val glassTopW = actualCupTopW
        val glassBottomW = actualCupTopW * (0.70f / 0.88f)

        val topCenterX = canvasW / 2f
        val topLeftX = topCenterX - glassTopW / 2f
        val topRightX = topCenterX + glassTopW / 2f

        val bottomCenterX = canvasW / 2f
        val bottomLeftX = bottomCenterX - glassBottomW / 2f
        val bottomRightX = bottomCenterX + glassBottomW / 2f

        val scaleFactor = (actualCupHeight / 150f).coerceIn(0.55f, 1.4f)
        val wallThickness = 2.5.dp.toPx() * scaleFactor
        val baseThickness = 6.dp.toPx() * scaleFactor
        val cornerRadius = 12.dp.toPx() * scaleFactor

        val outerPath = Path().apply {
            moveTo(topLeftX, glassTopY)
            lineTo(topRightX, glassTopY)
            lineTo(bottomRightX, glassBottomY - cornerRadius)
            quadraticTo(bottomRightX, glassBottomY, bottomRightX - cornerRadius, glassBottomY)
            lineTo(bottomLeftX + cornerRadius, glassBottomY)
            quadraticTo(bottomLeftX, glassBottomY, bottomLeftX, glassBottomY - cornerRadius)
            close()
        }

        val innerTopY = glassTopY + 2.dp.toPx() * scaleFactor
        val innerBottomY = glassBottomY - baseThickness
        val innerTopLeftX = topLeftX + wallThickness
        val innerTopRightX = topRightX - wallThickness
        val innerBottomLeftX = bottomLeftX + wallThickness
        val innerBottomRightX = bottomRightX - wallThickness
        val innerCornerRadius = (cornerRadius - wallThickness).coerceAtLeast(2.dp.toPx())

        val innerGlassPath = Path().apply {
            moveTo(innerTopLeftX, innerTopY)
            lineTo(innerTopRightX, innerTopY)
            lineTo(innerBottomRightX, innerBottomY - innerCornerRadius)
            quadraticTo(innerBottomRightX, innerBottomY, innerBottomRightX - innerCornerRadius, innerBottomY)
            lineTo(innerBottomLeftX + innerCornerRadius, innerBottomY)
            quadraticTo(innerBottomLeftX, innerBottomY, innerBottomLeftX, innerBottomY - innerCornerRadius)
            close()
        }

        drawPath(
            path = innerGlassPath,
            color = Color(0x1AFFFFFF)
        )

        // Etched ticks
        if (actualCupHeight > 55.dp.toPx()) {
            val ticks = listOf(0.25f, 0.50f, 0.75f, 1.00f)
            ticks.forEach { t ->
                val tickY = innerBottomY - (innerBottomY - innerTopY) * t
                val tickWidth = 6.dp.toPx() * scaleFactor
                val rightWallX = innerTopRightX + (innerBottomRightX - innerTopRightX) * (1f - t)
                drawLine(
                    color = Color(0x44FFFFFF),
                    start = Offset(rightWallX - tickWidth, tickY),
                    end = Offset(rightWallX, tickY),
                    strokeWidth = 1.dp.toPx() * scaleFactor
                )
            }
        }

        // Draw Liquid
        clipPath(innerGlassPath) {
            val clampedP = fillProgress.coerceIn(0f, 1.2f)

            if (clampedP > 0.005f) {
                val cavityHeight = innerBottomY - innerTopY
                val waterCenterY = innerBottomY - cavityHeight * clampedP.coerceAtMost(1.0f)
                val slope = tan(liquidAngle)

                val extendedW = glassTopW * 1.5f
                val leftX = topCenterX - extendedW
                val rightX = topCenterX + extendedW

                val surfaceYLeft = waterCenterY - extendedW * slope
                val surfaceYRight = waterCenterY + extendedW * slope
                val waveOffset = sin(wavePhase) * waveAmplitude * scaleFactor

                val waterPath = Path().apply {
                    moveTo(leftX, surfaceYLeft)
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

                val waterBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD900E5FF),
                        Color(0xB30091EA),
                        Color(0x99003366)
                    ),
                    startY = waterCenterY - 20f,
                    endY = innerBottomY
                )
                drawPath(path = waterPath, brush = waterBrush)

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

                if (actualCupHeight > 45.dp.toPx()) {
                    bubbles.forEach { b ->
                        val bCycle = (bubbleTime * b.speed + b.wobblePhase) % 1.0f
                        val bY = innerBottomY - cavityHeight * clampedP * bCycle
                        val wobbleX = sin(bubbleTime * 4f + b.wobblePhase) * 3.dp.toPx() * scaleFactor
                        val bX = innerBottomLeftX + (innerBottomRightX - innerBottomLeftX) * b.xFrac + wobbleX

                        if (bY > waterCenterY + 4.dp.toPx() * scaleFactor) {
                            drawCircle(
                                color = Color(0x66FFFFFF),
                                radius = b.radius.dp.toPx() * scaleFactor,
                                center = Offset(bX, bY)
                            )
                        }
                    }
                }
            }
        }

        // Pouring Stream
        if (isPouring) {
            val streamCenterX = topCenterX
            val streamWidth = (4.dp.toPx() * scaleFactor).coerceAtLeast(2.dp.toPx())
            val cavityHeight = innerBottomY - innerTopY
            val waterCenterY = (innerBottomY - cavityHeight * fillProgress.coerceIn(0f, 1f)).coerceAtLeast(innerTopY)

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

            val splashWidth = 14.dp.toPx() * scaleFactor
            drawOval(
                color = Color(0xAAFFFFFF),
                topLeft = Offset(streamCenterX - splashWidth / 2f, waterCenterY - 2.dp.toPx()),
                size = Size(splashWidth, 5.dp.toPx() * scaleFactor),
                style = Stroke(width = 1.5.dp.toPx() * scaleFactor)
            )
        }

        // Outer Glass Border
        drawPath(
            path = outerPath,
            color = Color(0x40FFFFFF),
            style = Stroke(width = wallThickness)
        )

        // Glass Base
        drawLine(
            color = Color(0x55FFFFFF),
            start = Offset(bottomLeftX + 4.dp.toPx(), glassBottomY - 2.dp.toPx()),
            end = Offset(bottomRightX - 4.dp.toPx(), glassBottomY - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx()
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

                Text(
                    text = "Health standards recommend 8 glasses (2,000 ml) daily, or approximately 35 ml per kilogram of body weight for active individuals.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = NothingTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

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

package com.example.buddygotchi

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.nothingDotGrid
import com.example.buddygotchi.ui.theme.BuddyGotchiTheme
import com.example.buddygotchi.ui.theme.Dseg7FontFamily
import com.example.buddygotchi.ui.theme.NothingRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        enableEdgeToEdge()

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
        val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY) ?: "TASK"
        val taskTimeMs = intent.getLongExtra(EXTRA_TASK_TIME, 0L)

        // Ensure alarm sound is ringing if not started already
        AlarmPlayer.startAlarm(this, taskId)

        setContent {
            BuddyGotchiTheme {
                AlarmScreen(
                    category = categoryLabel,
                    taskTimeMs = taskTimeMs,
                    onDismiss = {
                        dismissAlarm(taskId)
                    },
                    onSnooze = {
                        snoozeAlarm(taskId)
                    }
                )
            }
        }
    }

    private fun dismissAlarm(taskId: String) {
        AlarmPlayer.stopAlarm()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskId.hashCode())
        finish()
    }

    private fun snoozeAlarm(taskId: String) {
        AlarmPlayer.stopAlarm()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskId.hashCode())
        TaskManager.snoozeTask(this, taskId, 5)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmPlayer.stopAlarm()
    }

    companion object {
        const val EXTRA_TASK_ID = "com.example.buddygotchi.EXTRA_TASK_ID"
        const val EXTRA_CATEGORY = "com.example.buddygotchi.EXTRA_CATEGORY"
        const val EXTRA_TASK_TIME = "com.example.buddygotchi.EXTRA_TASK_TIME"
        const val EXTRA_OFFSET_MINUTES = "com.example.buddygotchi.EXTRA_OFFSET_MINUTES"
    }
}

@Composable
fun AlarmScreen(
    category: String,
    taskTimeMs: Long,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    // Current live time loop
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedCurrentTime = timeFormat.format(currentTime)
    val formattedScheduledTime = if (taskTimeMs > 0) timeFormat.format(Date(taskTimeMs)) else ""

    // Flashing red indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nothingDotGrid()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NothingRed.copy(alpha = alphaAnim))
                )
                Text(
                    text = "BUDDYGOTCHI • ALARM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = NothingRed
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = category.uppercase(Locale.getDefault()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Center Clock & Alert Info
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = formattedCurrentTime,
                fontFamily = Dseg7FontFamily,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (formattedScheduledTime.isNotEmpty()) {
                Text(
                    text = "SCHEDULED FOR $formattedScheduledTime",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = "TIME TO COMPLETE YOUR TASK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Bottom Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SNOOZE (+5m) Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSnooze() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SNOOZE (+5 MIN)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // DISMISS Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NothingRed)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DISMISS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            }
        }
    }
}

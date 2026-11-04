package com.example.buddygotchi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlertNotificationManager {
    const val CHANNEL_ID = "buddygotchi_task_alerts"
    private const val CHANNEL_NAME = "BuddyGotchi Task Alerts"

    fun showTaskNotification(
        context: Context,
        taskId: String = "test_alert",
        taskTitle: String = "Reminder Task",
        taskCategory: String = "REMINDER",
        taskTime: Long = System.currentTimeMillis(),
        offsetMinutes: Int = 0
    ) {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        // 1. Phone vibration
        triggerVibration(appContext)

        // 2. Open App Intent
        val openIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            taskId.hashCode(),
            openIntent,
            flags
        )

        // Dismiss action
        val dismissIntent = Intent(appContext, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DISMISS_ALARM
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            appContext,
            taskId.hashCode() + 1,
            dismissIntent,
            flags
        )

        val timeStr = if (taskTime > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(taskTime))
        } else ""

        val subtitle = when {
            offsetMinutes > 0 -> "Upcoming at $timeStr (in $offsetMinutes min) • $taskCategory"
            timeStr.isNotBlank() -> "Due now at $timeStr • $taskCategory"
            else -> "Due now • $taskCategory"
        }

        // Buddy thumbnail large icon
        AccountAndInventoryManager.ensureInitialized(appContext)
        val petId = AccountAndInventoryManager.equippedPetIdState.value
        val buddyBitmap = Buddy3DManager.getBuddyScreenshotBitmap(appContext, petId)

        val vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(taskTitle.ifBlank { "$taskCategory Task Due" })
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setVibrate(vibrationPattern)
            .setColor(Color.parseColor("#D71921")) // NothingRed
            .addAction(0, "DISMISS", dismissPendingIntent)

        if (buddyBitmap != null) {
            builder.setLargeIcon(buddyBitmap)
        }

        try {
            NotificationManagerCompat.from(appContext).notify(taskId.hashCode(), builder.build())
        } catch (_: SecurityException) {}
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "High priority task due alerts with sound and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = Color.parseColor("#D71921")
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val pattern = longArrayOf(0, 400, 200, 400, 200, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }
}

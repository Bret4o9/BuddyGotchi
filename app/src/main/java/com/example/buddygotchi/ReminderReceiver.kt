package com.example.buddygotchi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule active reminders after reboot
                val tasks = TaskManager.getTasks(context)
                val now = System.currentTimeMillis()
                tasks.filter { !it.isCompleted && it.reminderTime.time > now }.forEach { task ->
                    ReminderScheduler.scheduleReminder(context, task)
                }
            }

            ACTION_DISMISS_ALARM -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                AlarmPlayer.stopAlarm()
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(taskId.hashCode())

                // If task is recurring, advance by 24h and reschedule
                val tasks = TaskManager.getTasks(context)
                val task = tasks.find { it.id == taskId }
                if (task != null && task.isRecurring) {
                    val nextReminder = Date(task.reminderTime.time + 24 * 60 * 60 * 1000L)
                    val nextTaskTime = Date(task.taskTime.time + 24 * 60 * 60 * 1000L)
                    val recurringTask = task.copy(
                        reminderTime = nextReminder,
                        taskTime = nextTaskTime,
                        isCompleted = false
                    )
                    val updated = tasks.map { if (it.id == taskId) recurringTask else it }
                    TaskManager.saveTasks(context, updated)
                    ReminderScheduler.scheduleReminder(context, recurringTask)
                }
            }

            ACTION_SNOOZE_ALARM -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                AlarmPlayer.stopAlarm()
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(taskId.hashCode())
                TaskManager.snoozeTask(context, taskId, 5)
            }

            else -> {
                // Trigger Alarm
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY) ?: "REMINDER"
                val taskTimeMs = intent.getLongExtra(EXTRA_TASK_TIME, 0L)
                val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)
                val taskDescription = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
                val isRecurring = intent.getBooleanExtra(EXTRA_IS_RECURRING, false)

                // 1. Play looping alarm sound & vibration immediately
                AlarmPlayer.startAlarm(context, taskId)

                // 2. Prepare Channel
                val channelId = "buddygotchi_alarms_v2"
                createAlarmChannel(context, channelId)

                // 3. Full-Screen Intent for AlarmActivity
                val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmActivity.EXTRA_TASK_ID, taskId)
                    putExtra(AlarmActivity.EXTRA_CATEGORY, categoryLabel)
                    putExtra(AlarmActivity.EXTRA_TASK_TIME, taskTimeMs)
                    putExtra(AlarmActivity.EXTRA_OFFSET_MINUTES, offsetMinutes)
                    putExtra(AlarmActivity.EXTRA_DESCRIPTION, taskDescription)
                    putExtra(AlarmActivity.EXTRA_IS_RECURRING, isRecurring)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    taskId.hashCode(),
                    fullScreenIntent,
                    flags
                )

                // 4. Action: DISMISS
                val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_DISMISS_ALARM
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode() + 10,
                    dismissIntent,
                    flags
                )

                // 5. Action: SNOOZE (+5m)
                val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_SNOOZE_ALARM
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_CATEGORY, categoryLabel)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode() + 20,
                    snoozeIntent,
                    flags
                )

                val formattedTaskTime = if (taskTimeMs > 0) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(taskTimeMs))
                } else ""

                val message = if (offsetMinutes > 0) {
                    "Upcoming at $formattedTaskTime (in $offsetMinutes min)"
                } else {
                    "Due now at $formattedTaskTime"
                }

                val notificationTitle = if (taskDescription.isNotBlank()) {
                    taskDescription
                } else {
                    "BuddyGotchi • $categoryLabel"
                }

                val notificationSubtitle = if (taskDescription.isNotBlank()) {
                    "$categoryLabel • $message"
                } else {
                    message
                }

                // 6. Build High-Priority Alarm Notification with Full-Screen Alert
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationSubtitle)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(fullScreenPendingIntent)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .addAction(0, "DISMISS", dismissPendingIntent)
                    .addAction(0, "SNOOZE (+5M)", snoozePendingIntent)
                    .build()

                notification.flags = notification.flags or Notification.FLAG_INSISTENT

                try {
                    NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
                } catch (_: SecurityException) {
                    // POST_NOTIFICATIONS permission check
                }
            }
        }
    }

    private fun createAlarmChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BuddyGotchi Alarms"
            val descriptionText = "Full-screen alarm alerts for BuddyGotchi tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(false) // Sound and vibration handled by AlarmPlayer
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_REMINDER_ALARM = "com.example.buddygotchi.REMINDER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.buddygotchi.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.buddygotchi.SNOOZE_ALARM"

        const val EXTRA_TASK_ID = "com.example.buddygotchi.EXTRA_TASK_ID"
        const val EXTRA_CATEGORY = "com.example.buddygotchi.EXTRA_CATEGORY"
        const val EXTRA_TASK_TIME = "com.example.buddygotchi.EXTRA_TASK_TIME"
        const val EXTRA_OFFSET_MINUTES = "com.example.buddygotchi.EXTRA_OFFSET_MINUTES"
        const val EXTRA_DESCRIPTION = "com.example.buddygotchi.EXTRA_DESCRIPTION"
        const val EXTRA_IS_RECURRING = "com.example.buddygotchi.EXTRA_IS_RECURRING"
    }
}

package com.example.buddygotchi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Android Home Screen Widget Provider.
 * Displays the animated BuddyGotchi companion on the left,
 * and the user's next upcoming task on the right with live countdown.
 */
class BuddyTaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PET_BUDDY) {
            // Give 5 XP to Buddy when petted from home screen
            TaskManager.addBuddyXp(context, 5)

            // Optional light haptic buzz
            try {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } catch (_: Exception) {}

            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_PET_BUDDY = "com.example.buddygotchi.ACTION_PET_BUDDY"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_buddy_task)

            // --- 1. BUDDY STATS (Left Column) ---
            val xp = TaskManager.getBuddyXp(context)
            val level = (xp / 50) + 1
            val progressInLevel = xp % 50

            views.setTextViewText(R.id.widget_buddy_level, "LVL $level BUDDY")
            views.setProgressBar(R.id.widget_buddy_xp_progress, 50, progressInLevel, false)
            views.setTextViewText(R.id.widget_buddy_xp_text, "$progressInLevel / 50 XP")

            // --- 2. NEXT TASK (Right Column) ---
            val tasks = TaskManager.getTasks(context)
            val now = System.currentTimeMillis()
            val activeTasks = tasks.filter { !it.isCompleted }
            val nextTask = activeTasks.filter { it.taskTime.time >= now }.minByOrNull { it.taskTime.time }
                ?: activeTasks.minByOrNull { it.taskTime.time }

            if (nextTask != null) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val diffMs = nextTask.taskTime.time - now
                val diffMins = (diffMs / (60 * 1000L)).toInt()

                val countdownText = when {
                    diffMins < -60 -> "Overdue"
                    diffMins < 0 -> "${-diffMins}m ago"
                    diffMins == 0 -> "Now"
                    diffMins < 60 -> "in ${diffMins}m"
                    diffMins < 1440 -> "in ${diffMins / 60}h"
                    else -> "in ${diffMins / 1440}d"
                }

                views.setTextViewText(R.id.widget_task_header, "NEXT TASK")
                views.setTextViewText(R.id.widget_task_category, nextTask.category.label.uppercase(Locale.getDefault()))
                views.setTextViewText(R.id.widget_task_time, timeFormat.format(nextTask.taskTime))
                views.setTextViewText(R.id.widget_task_countdown, countdownText)

                val desc = if (nextTask.description.isNotBlank()) nextTask.description else "No description"
                views.setTextViewText(R.id.widget_task_description, desc)
                views.setTextViewText(R.id.widget_task_footer, "Tap to open in BuddyGotchi →")
            } else {
                // Empty state: All tasks completed
                views.setTextViewText(R.id.widget_task_header, "STATUS")
                views.setTextViewText(R.id.widget_task_category, "DONE ✓")
                views.setTextViewText(R.id.widget_task_time, "ALL DONE")
                views.setTextViewText(R.id.widget_task_countdown, "")
                views.setTextViewText(R.id.widget_task_description, "No pending tasks today. Great job!")
                views.setTextViewText(R.id.widget_task_footer, "Tap to schedule new task +")
            }

            // --- 3. CLICK INTENTS ---
            // Tapping the task launches the app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_task_container, appPendingIntent)

            // Tapping the buddy pets the buddy and gives +5 XP
            val petIntent = Intent(context, BuddyTaskWidgetProvider::class.java).apply {
                action = ACTION_PET_BUDDY
            }
            val petPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                petIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_buddy_container, petPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, BuddyTaskWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }
}

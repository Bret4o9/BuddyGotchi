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
import com.example.buddygotchi.squad.GroupEvent
import com.example.buddygotchi.squad.ParticipantStatus
import com.example.buddygotchi.squad.SquadManager
import java.text.SimpleDateFormat
import java.util.Date
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

            // --- 2. NEXT AGENDA ITEM: TASK OR SQUAD EVENT (Right Column) ---
            val now = System.currentTimeMillis()
            val tasks = TaskManager.getTasks(context)
            val activeTasks = tasks.filter { !it.isCompleted }

            val profile = SquadManager.getProfile(context)
            val squadEvents = SquadManager.getEvents(context)
            val activeSquadEvents = squadEvents.filter { event ->
                if (event.isCancelled) return@filter false
                val userPart = event.participants.find { it.buddyCode.equals(profile.buddyCode, ignoreCase = true) }
                if (userPart != null && userPart.status == ParticipantStatus.DECLINED) return@filter false
                true
            }

            // Internal agenda item wrapper
            data class AgendaItem(
                val timeMs: Long,
                val isSquad: Boolean,
                val task: ReminderTask? = null,
                val squadEvent: GroupEvent? = null
            )

            val agendaItems = mutableListOf<AgendaItem>()
            activeTasks.forEach { task ->
                agendaItems.add(AgendaItem(timeMs = task.taskTime.time, isSquad = false, task = task))
            }
            activeSquadEvents.forEach { event ->
                agendaItems.add(AgendaItem(timeMs = event.eventTime, isSquad = true, squadEvent = event))
            }

            val futureItems = agendaItems.filter { it.timeMs >= now }
            val nextItem = futureItems.minByOrNull { it.timeMs }
                ?: agendaItems.minByOrNull { it.timeMs }

            if (nextItem != null) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val diffMs = nextItem.timeMs - now
                val diffMins = (diffMs / (60 * 1000L)).toInt()

                val countdownText = when {
                    diffMins < -60 -> "Overdue"
                    diffMins < 0 -> "${-diffMins}m ago"
                    diffMins == 0 -> "Now"
                    diffMins < 60 -> "in ${diffMins}m"
                    diffMins < 1440 -> "in ${diffMins / 60}h"
                    else -> "in ${diffMins / 1440}d"
                }

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                if (nextItem.isSquad && nextItem.squadEvent != null) {
                    val event = nextItem.squadEvent
                    val otherTasksCount = activeTasks.count { it.taskTime.time >= now }
                    val headerExtra = if (otherTasksCount > 0) " (+${otherTasksCount} TASK)" else ""
                    views.setTextViewText(R.id.widget_task_header, "● SQUAD EVENT$headerExtra")

                    val isUserHost = event.hostBuddyCode.equals(profile.buddyCode, ignoreCase = true)
                    val userStatus = event.participants.find { it.buddyCode.equals(profile.buddyCode, ignoreCase = true) }?.status
                    val categoryLabel = when {
                        isUserHost -> "HOST: YOU"
                        userStatus == ParticipantStatus.PENDING -> "RSVP PENDING"
                        else -> "SQUAD // ${event.hostDisplayName.uppercase(Locale.getDefault())}"
                    }
                    views.setTextViewText(R.id.widget_task_category, categoryLabel)
                    views.setTextViewText(R.id.widget_task_time, timeFormat.format(Date(event.eventTime)))
                    views.setTextViewText(R.id.widget_task_countdown, countdownText)

                    val locationClean = if (event.location.isNotBlank()) {
                        " • 📍 " + event.location.replace("https://maps.google.com/?q=", "")
                    } else ""
                    val acceptedCount = event.participants.count { it.status == ParticipantStatus.ACCEPTED }
                    val desc = "${event.title.uppercase(Locale.getDefault())}$locationClean [${acceptedCount} ATTENDING]"
                    views.setTextViewText(R.id.widget_task_description, desc)
                    views.setTextViewText(R.id.widget_task_footer, "Tap to open Squad Hub →")

                    appIntent.putExtra("target_page", "SQUAD")
                    appIntent.putExtra("event_id", event.id)
                } else if (nextItem.task != null) {
                    val task = nextItem.task
                    val otherSquadCount = activeSquadEvents.count { it.eventTime >= now }
                    val headerExtra = if (otherSquadCount > 0) " (+${otherSquadCount} SQUAD)" else ""
                    views.setTextViewText(R.id.widget_task_header, "NEXT TASK$headerExtra")

                    views.setTextViewText(R.id.widget_task_category, task.category.label.uppercase(Locale.getDefault()))
                    views.setTextViewText(R.id.widget_task_time, timeFormat.format(task.taskTime))
                    views.setTextViewText(R.id.widget_task_countdown, countdownText)

                    val desc = if (task.description.isNotBlank()) task.description else "No description"
                    views.setTextViewText(R.id.widget_task_description, desc)
                    views.setTextViewText(R.id.widget_task_footer, "Tap to open in BuddyGotchi →")

                    appIntent.putExtra("target_page", "MAIN")
                }

                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_task_container, appPendingIntent)
            } else {
                // Empty state: All tasks and squad events completed / none scheduled
                views.setTextViewText(R.id.widget_task_header, "STATUS")
                views.setTextViewText(R.id.widget_task_category, "ALL CLEAR ✓")
                views.setTextViewText(R.id.widget_task_time, "ALL DONE")
                views.setTextViewText(R.id.widget_task_countdown, "")
                views.setTextViewText(R.id.widget_task_description, "No pending tasks or squad events today. Great job!")
                views.setTextViewText(R.id.widget_task_footer, "Tap to schedule new task +")

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
            }

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

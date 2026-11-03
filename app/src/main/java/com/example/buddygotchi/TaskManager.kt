package com.example.buddygotchi

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TaskManager {

    private const val PREFS_NAME = "buddygotchi_prefs"
    private const val KEY_TASKS = "saved_tasks"
    private const val KEY_BUDDY_XP = "buddy_xp"

    // Configurable Petting XP settings (Default: 1 XP per pet, max 5 XP per day)
    const val DEFAULT_XP_PER_PET = 1
    const val DEFAULT_MAX_DAILY_PET_XP = 5
    private const val KEY_XP_PER_PET = "xp_per_pet_config"
    private const val KEY_MAX_DAILY_PET_XP = "max_daily_pet_xp_config"
    private const val KEY_DAILY_PET_DATE = "daily_pet_date_str"
    private const val KEY_DAILY_PET_XP_EARNED = "daily_pet_xp_earned"

    val buddyXpState = mutableIntStateOf(0)
    val dailyPetXpEarnedState = mutableIntStateOf(0)
    val maxDailyPetXpState = mutableIntStateOf(DEFAULT_MAX_DAILY_PET_XP)

    private fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getTasks(context: Context): List<ReminderTask> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        val list = mutableListOf<ReminderTask>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val taskTimeMs = obj.getLong("taskTime")
                val offsetMinutes = obj.getInt("offsetMinutes")
                val reminderTimeMs = obj.getLong("reminderTime")
                val categoryName = obj.optString("category", TaskCategory.WORK.name)
                val category = try {
                    TaskCategory.valueOf(categoryName)
                } catch (_: Exception) {
                    TaskCategory.WORK
                }
                val isCompleted = obj.optBoolean("isCompleted", false)
                val description = obj.optString("description", "")
                val isRecurring = obj.optBoolean("isRecurring", false)

                list.add(
                    ReminderTask(
                        id = id,
                        taskTime = Date(taskTimeMs),
                        offsetMinutes = offsetMinutes,
                        reminderTime = Date(reminderTimeMs),
                        category = category,
                        isCompleted = isCompleted,
                        description = description,
                        isRecurring = isRecurring
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTasks(context: Context, tasks: List<ReminderTask>) {
        val jsonArray = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("taskTime", task.taskTime.time)
                put("offsetMinutes", task.offsetMinutes)
                put("reminderTime", task.reminderTime.time)
                put("category", task.category.name)
                put("isCompleted", task.isCompleted)
                put("description", task.description)
                put("isRecurring", task.isRecurring)
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
        BuddyTaskWidgetProvider.updateAllWidgets(context)
    }

    fun addTask(context: Context, task: ReminderTask): List<ReminderTask> {
        val current = getTasks(context).toMutableList()
        current.add(0, task)
        saveTasks(context, current)
        ReminderScheduler.scheduleReminder(context, task)
        addBuddyXp(context, 10)
        return current
    }

    fun toggleTask(context: Context, taskId: String): List<ReminderTask> {
        val current = getTasks(context).map {
            if (it.id == taskId) {
                val updated = it.copy(isCompleted = !it.isCompleted)
                if (updated.isCompleted) {
                    ReminderScheduler.cancelReminder(context, taskId)
                } else {
                    ReminderScheduler.scheduleReminder(context, updated)
                }
                updated
            } else it
        }
        saveTasks(context, current)
        return current
    }

    fun getBuddyXp(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val xp = prefs.getInt(KEY_BUDDY_XP, 0)
        buddyXpState.intValue = xp
        dailyPetXpEarnedState.intValue = getDailyPetXpEarned(context)
        maxDailyPetXpState.intValue = getMaxDailyPetXp(context)
        return xp
    }

    fun getXpPerPet(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_XP_PER_PET, DEFAULT_XP_PER_PET).coerceAtLeast(1)
    }

    fun setXpPerPet(context: Context, xpPerPet: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_XP_PER_PET, xpPerPet.coerceAtLeast(1)).apply()
    }

    fun getMaxDailyPetXp(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cap = prefs.getInt(KEY_MAX_DAILY_PET_XP, DEFAULT_MAX_DAILY_PET_XP).coerceAtLeast(0)
        maxDailyPetXpState.intValue = cap
        return cap
    }

    fun setMaxDailyPetXp(context: Context, maxDailyXp: Int) {
        val safeCap = maxDailyXp.coerceAtLeast(0)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MAX_DAILY_PET_XP, safeCap).apply()
        maxDailyPetXpState.intValue = safeCap
        BuddyTaskWidgetProvider.updateAllWidgets(context)
    }

    fun getDailyPetXpEarned(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayDateKey()
        val savedDate = prefs.getString(KEY_DAILY_PET_DATE, null)
        return if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DAILY_PET_DATE, today)
                .putInt(KEY_DAILY_PET_XP_EARNED, 0)
                .apply()
            dailyPetXpEarnedState.intValue = 0
            0
        } else {
            val earned = prefs.getInt(KEY_DAILY_PET_XP_EARNED, 0).coerceAtLeast(0)
            dailyPetXpEarnedState.intValue = earned
            earned
        }
    }

    /**
     * Pets the Buddy and grants up to [getXpPerPet] (default 1 XP) per pet,
     * capped at [getMaxDailyPetXp] (default 5 XP) per calendar day.
     * Returns the actual XP granted (0 if daily cap reached).
     */
    fun petBuddy(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayDateKey()
        val earnedToday = getDailyPetXpEarned(context)
        val maxDaily = getMaxDailyPetXp(context)
        val xpPerPet = getXpPerPet(context)

        val remaining = (maxDaily - earnedToday).coerceAtLeast(0)
        if (remaining <= 0) {
            dailyPetXpEarnedState.intValue = earnedToday
            BuddyTaskWidgetProvider.updateAllWidgets(context)
            return 0
        }

        val granted = minOf(xpPerPet, remaining)
        val newDailyTotal = earnedToday + granted
        prefs.edit()
            .putString(KEY_DAILY_PET_DATE, today)
            .putInt(KEY_DAILY_PET_XP_EARNED, newDailyTotal)
            .apply()
        dailyPetXpEarnedState.intValue = newDailyTotal

        addBuddyXp(context, granted)
        return granted
    }

    fun addBuddyXp(context: Context, xpAmount: Int): Int {
        val current = (getBuddyXp(context) + xpAmount).coerceAtLeast(0)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BUDDY_XP, current).apply()
        buddyXpState.intValue = current
        BuddyTaskWidgetProvider.updateAllWidgets(context)
        try {
            AccountAndInventoryManager.syncToCloudIfEnabled(context)
        } catch (_: Exception) {}
        return current
    }

    fun setBuddyXp(context: Context, xp: Int) {
        val safeXp = xp.coerceAtLeast(0)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BUDDY_XP, safeXp).apply()
        buddyXpState.intValue = safeXp
        BuddyTaskWidgetProvider.updateAllWidgets(context)
    }

    fun clearDoneTasks(context: Context): List<ReminderTask> {
        val current = getTasks(context)
        val remaining = current.filter { !it.isCompleted }
        saveTasks(context, remaining)
        return remaining
    }

    fun snoozeTask(context: Context, taskId: String, minutes: Int = 5): ReminderTask? {
        val tasks = getTasks(context).toMutableList()
        val index = tasks.indexOfFirst { it.id == taskId }
        val now = System.currentTimeMillis()
        val newReminderTime = Date(now + minutes * 60 * 1000L)

        val snoozedTask = if (index != -1) {
            val original = tasks[index]
            val updated = original.copy(
                reminderTime = newReminderTime,
                taskTime = if (original.taskTime.time <= newReminderTime.time) {
                    Date(newReminderTime.time + (original.offsetMinutes * 60 * 1000L))
                } else {
                    original.taskTime
                },
                isCompleted = false
            )
            tasks[index] = updated
            saveTasks(context, tasks)
            updated
        } else {
            ReminderTask(
                id = taskId,
                taskTime = newReminderTime,
                offsetMinutes = 0,
                reminderTime = newReminderTime,
                category = TaskCategory.WORK,
                isCompleted = false
            )
        }

        ReminderScheduler.scheduleReminder(context, snoozedTask)
        return snoozedTask
    }
}

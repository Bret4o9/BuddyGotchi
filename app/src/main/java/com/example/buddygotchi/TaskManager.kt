package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object TaskManager {

    private const val PREFS_NAME = "buddygotchi_prefs"
    private const val KEY_TASKS = "saved_tasks"
    private const val KEY_BUDDY_XP = "buddy_xp"

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

                list.add(
                    ReminderTask(
                        id = id,
                        taskTime = Date(taskTimeMs),
                        offsetMinutes = offsetMinutes,
                        reminderTime = Date(reminderTimeMs),
                        category = category,
                        isCompleted = isCompleted
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
            }
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
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
        return prefs.getInt(KEY_BUDDY_XP, 0)
    }

    fun addBuddyXp(context: Context, xpAmount: Int): Int {
        val current = getBuddyXp(context) + xpAmount
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BUDDY_XP, current).apply()
        return current
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

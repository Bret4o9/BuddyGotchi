package com.example.buddygotchi

import java.util.Date
import java.util.UUID

enum class TaskCategory(val label: String) {
    WORK("WORK"),
    PERSONAL("PERSONAL"),
    OTHER("OTHER")
}

data class ReminderTask(
    val id: String = UUID.randomUUID().toString(),
    val taskTime: Date,
    val offsetMinutes: Int,
    val reminderTime: Date,
    val category: TaskCategory,
    val isCompleted: Boolean = false
)

package com.example.buddygotchi

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WaterTrackerState(
    val currentMl: Int = 0,
    val targetMl: Int = 2000,
    val userWeightKg: Int = 70,
    val glassSizeMl: Int = 250,
    val lastDate: String = ""
) {
    val glassesCount: Int get() = if (glassSizeMl > 0) currentMl / glassSizeMl else 0
    val targetGlasses: Int get() = if (glassSizeMl > 0) (targetMl + glassSizeMl - 1) / glassSizeMl else 8
    val percentage: Float get() = if (targetMl > 0) (currentMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 2f) else 0f
}

object WaterTrackerManager {
    private const val PREFS_NAME = "buddygotchi_water_prefs"
    private const val KEY_CURRENT_ML = "current_ml"
    private const val KEY_TARGET_ML = "target_ml"
    private const val KEY_WEIGHT_KG = "weight_kg"
    private const val KEY_GLASS_SIZE = "glass_size"
    private const val KEY_LAST_DATE = "last_date"

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getWaterState(context: Context): WaterTrackerState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val targetMl = prefs.getInt(KEY_TARGET_ML, 2000)
        val weightKg = prefs.getInt(KEY_WEIGHT_KG, 70)
        val glassSize = prefs.getInt(KEY_GLASS_SIZE, 250)

        val currentMl = if (savedDate == today) {
            prefs.getInt(KEY_CURRENT_ML, 0)
        } else {
            // New day: reset current intake
            prefs.edit().putInt(KEY_CURRENT_ML, 0).putString(KEY_LAST_DATE, today).apply()
            0
        }

        return WaterTrackerState(
            currentMl = currentMl,
            targetMl = targetMl,
            userWeightKg = weightKg,
            glassSizeMl = glassSize,
            lastDate = today
        )
    }

    fun addWater(context: Context, ml: Int): WaterTrackerState {
        val state = getWaterState(context)
        val newMl = (state.currentMl + ml).coerceAtLeast(0)
        saveState(context, state.copy(currentMl = newMl))
        return state.copy(currentMl = newMl)
    }

    fun removeWater(context: Context, ml: Int): WaterTrackerState {
        val state = getWaterState(context)
        val newMl = (state.currentMl - ml).coerceAtLeast(0)
        saveState(context, state.copy(currentMl = newMl))
        return state.copy(currentMl = newMl)
    }

    fun resetWater(context: Context): WaterTrackerState {
        val state = getWaterState(context)
        saveState(context, state.copy(currentMl = 0))
        return state.copy(currentMl = 0)
    }

    fun updateWeight(context: Context, weightKg: Int): WaterTrackerState {
        val state = getWaterState(context)
        // Standard formula: 35 ml of water per 1 kg of body weight
        val computedTarget = (weightKg * 35).coerceIn(1000, 5000)
        val updated = state.copy(userWeightKg = weightKg, targetMl = computedTarget)
        saveState(context, updated)
        return updated
    }

    fun updateTargetMl(context: Context, targetMl: Int): WaterTrackerState {
        val state = getWaterState(context)
        val updated = state.copy(targetMl = targetMl.coerceIn(500, 6000))
        saveState(context, updated)
        return updated
    }

    private fun saveState(context: Context, state: WaterTrackerState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_CURRENT_ML, state.currentMl)
            .putInt(KEY_TARGET_ML, state.targetMl)
            .putInt(KEY_WEIGHT_KG, state.userWeightKg)
            .putInt(KEY_GLASS_SIZE, state.glassSizeMl)
            .putString(KEY_LAST_DATE, getTodayDateString())
            .apply()
    }
}

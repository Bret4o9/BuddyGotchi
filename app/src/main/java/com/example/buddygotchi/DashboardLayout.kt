package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class DashboardWidgetId(
    val title: String,
    val canRemove: Boolean
) {
    CLOCK("Clock", canRemove = false),
    TODAY("Today", canRemove = true),
    NEXT("Next", canRemove = true),
    BUDDY("Buddy", canRemove = true)
}

enum class WidgetSize {
    CUBE,   // 1x1 cube (88dp x 88dp) - MIN SIZE
    HALF,   // 2x1 half row (~170dp x 120dp)
    SLIM,   // 4x1 full width slim banner (~360dp x 88dp)
    WIDE,   // 4x2 full width (~360dp x 180dp)
    MAX,    // 4x3 full width (~360dp x 276dp) - MAX SIZE
    TALL    // 1x4 vertical strip (88dp x 276dp)
}

data class DashboardWidgetItem(
    val widgetId: DashboardWidgetId,
    val size: WidgetSize = when (widgetId) {
        DashboardWidgetId.CLOCK -> WidgetSize.MAX
        DashboardWidgetId.TODAY -> WidgetSize.WIDE
        DashboardWidgetId.NEXT -> WidgetSize.HALF
        DashboardWidgetId.BUDDY -> WidgetSize.HALF
    }
)

object DashboardLayoutManager {

    private const val PREFS_KEY_LAYOUT = "dashboard_widget_layout_v3"
    private const val PREFS_KEY_V2 = "dashboard_widget_layout_v2"
    private const val PREFS_KEY_LEGACY = "dashboard_widget_layout"

    fun getDefaultLayout(): List<DashboardWidgetItem> = listOf(
        DashboardWidgetItem(DashboardWidgetId.CLOCK, WidgetSize.MAX),
        DashboardWidgetItem(DashboardWidgetId.TODAY, WidgetSize.WIDE),
        DashboardWidgetItem(DashboardWidgetId.NEXT, WidgetSize.HALF),
        DashboardWidgetItem(DashboardWidgetId.BUDDY, WidgetSize.HALF)
    )

    fun loadLayout(context: Context): List<DashboardWidgetItem> {
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString(PREFS_KEY_LAYOUT, null)
            ?: prefs.getString(PREFS_KEY_V2, null)
            ?: prefs.getString(PREFS_KEY_LEGACY, null)
            ?: return getDefaultLayout()

        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<DashboardWidgetItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.has("size")) {
                    val idStr = obj.optString("widget", "")
                    val sizeStr = obj.optString("size", "WIDE")
                    val id = try { DashboardWidgetId.valueOf(idStr) } catch (_: Exception) { null }
                    val size = when (sizeStr) {
                        "CUBE" -> WidgetSize.CUBE
                        "HALF" -> WidgetSize.HALF
                        "SLIM" -> WidgetSize.SLIM
                        "TALL" -> WidgetSize.TALL
                        "WIDE" -> WidgetSize.WIDE
                        "MAX" -> WidgetSize.MAX
                        "FULL" -> WidgetSize.WIDE
                        else -> WidgetSize.WIDE
                    }
                    if (id != null) {
                        list.add(DashboardWidgetItem(id, if (id == DashboardWidgetId.CLOCK) WidgetSize.MAX else size))
                    }
                } else {
                    // Legacy migration: { "type": "full", "widget": "..." } or { "type": "split", "first": "...", "second": "..." }
                    val type = obj.optString("type")
                    if (type == "full") {
                        val id = DashboardWidgetId.valueOf(obj.getString("widget"))
                        list.add(DashboardWidgetItem(id, if (id == DashboardWidgetId.CLOCK) WidgetSize.MAX else WidgetSize.WIDE))
                    } else if (type == "split") {
                        val first = DashboardWidgetId.valueOf(obj.getString("first"))
                        val second = DashboardWidgetId.valueOf(obj.getString("second"))
                        list.add(DashboardWidgetItem(first, WidgetSize.HALF))
                        list.add(DashboardWidgetItem(second, WidgetSize.HALF))
                    }
                }
            }
            // Ensure CLOCK is always present and never duplicated
            if (!list.any { it.widgetId == DashboardWidgetId.CLOCK }) {
                list.add(0, DashboardWidgetItem(DashboardWidgetId.CLOCK, WidgetSize.MAX))
            }
            if (list.isEmpty()) getDefaultLayout() else list
        } catch (_: Exception) {
            getDefaultLayout()
        }
    }

    fun saveLayout(context: Context, layout: List<DashboardWidgetItem>) {
        val jsonArray = JSONArray()
        layout.forEach { item ->
            val obj = JSONObject()
            obj.put("widget", item.widgetId.name)
            obj.put("size", if (item.widgetId == DashboardWidgetId.CLOCK) WidgetSize.MAX.name else item.size.name)
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY_LAYOUT, jsonArray.toString()).apply()
    }

    fun moveItem(layout: List<DashboardWidgetItem>, fromIndex: Int, toIndex: Int): List<DashboardWidgetItem> {
        if (fromIndex < 0 || fromIndex > layout.lastIndex) return layout
        if (toIndex < 0 || toIndex > layout.lastIndex) return layout
        if (fromIndex == toIndex) return layout
        val mutable = layout.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun updateWidgetSize(layout: List<DashboardWidgetItem>, widgetId: DashboardWidgetId, newSize: WidgetSize): List<DashboardWidgetItem> {
        if (widgetId == DashboardWidgetId.CLOCK) return layout // Clock is fixed size 4x3
        return layout.map { item ->
            if (item.widgetId == widgetId) {
                item.copy(size = newSize)
            } else {
                item
            }
        }
    }

    fun removeWidget(layout: List<DashboardWidgetItem>, widgetId: DashboardWidgetId): List<DashboardWidgetItem> {
        if (widgetId == DashboardWidgetId.CLOCK) return layout
        return layout.filter { it.widgetId != widgetId }
    }

    fun addWidget(layout: List<DashboardWidgetItem>, widgetId: DashboardWidgetId): List<DashboardWidgetItem> {
        if (layout.any { it.widgetId == widgetId }) return layout
        val defaultSize = when (widgetId) {
            DashboardWidgetId.CLOCK -> WidgetSize.MAX
            DashboardWidgetId.TODAY -> WidgetSize.WIDE
            DashboardWidgetId.NEXT -> WidgetSize.HALF
            DashboardWidgetId.BUDDY -> WidgetSize.HALF
        }
        return layout + DashboardWidgetItem(widgetId, defaultSize)
    }

    fun getUnusedWidgets(layout: List<DashboardWidgetItem>): List<DashboardWidgetId> {
        return DashboardWidgetId.entries.filter { id ->
            id != DashboardWidgetId.CLOCK && !layout.any { it.widgetId == id }
        }
    }

    /**
     * Packs widgets into horizontal rows.
     * Each row has a capacity of 4 units:
     * - MAX = 4 units (spans whole row, 276.dp tall)
     * - WIDE = 4 units (spans whole row, 180.dp tall)
     * - HALF = 2 units (spans half row, 120.dp tall)
     * - CUBE = 1 unit (1x1 cube, 88.dp tall)
     */
    fun packIntoRows(items: List<DashboardWidgetItem>): List<List<DashboardWidgetItem>> {
        val rows = mutableListOf<List<DashboardWidgetItem>>()
        var currentRow = mutableListOf<DashboardWidgetItem>()
        var currentUnits = 0

        for (item in items) {
            val units = when (item.size) {
                WidgetSize.MAX, WidgetSize.WIDE, WidgetSize.SLIM -> 4
                WidgetSize.HALF -> 2
                WidgetSize.CUBE, WidgetSize.TALL -> 1
            }
            if (currentUnits + units > 4) {
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentUnits = 0
                }
            }
            currentRow.add(item)
            currentUnits += units
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        return rows
    }
}

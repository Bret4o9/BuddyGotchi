package com.example.buddygotchi

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

object DashboardGridDefaults {
    const val COLUMNS = 4
    val HORIZONTAL_PADDING: Dp = 16.dp
    val GAP: Dp = 14.dp
    val UNIT_HEIGHT: Dp = 88.dp // True modular grid: ALL height-1 widgets are exactly 88dp!

    fun getWidgetHeight(rows: Int): Dp = when (rows) {
        1 -> UNIT_HEIGHT // 88.dp
        2 -> UNIT_HEIGHT * 2 + GAP // 190.dp
        3 -> UNIT_HEIGHT * 3 + GAP * 2 // 292.dp
        4 -> UNIT_HEIGHT * 4 + GAP * 3 // 394.dp
        else -> UNIT_HEIGHT * 2 + GAP
    }

    fun getWidgetHeight(size: WidgetSize): Dp = getWidgetHeight(size.rows)

    fun getUnitWidth(contentWidth: Dp): Dp {
        val totalGaps = GAP * (COLUMNS - 1)
        return (contentWidth - totalGaps) / COLUMNS
    }

    fun getWidgetWidth(cols: Int, contentWidth: Dp): Dp {
        val unitW = getUnitWidth(contentWidth)
        return unitW * cols + GAP * (cols - 1)
    }
}

enum class DashboardWidgetId(
    val title: String,
    val canRemove: Boolean
) {
    CLOCK("Clock", canRemove = false),
    TODAY("Today", canRemove = true),
    NEXT("Next", canRemove = true),
    BUDDY("Buddy", canRemove = true)
}

enum class WidgetSize(val cols: Int, val rows: Int) {
    // 1-column widgets
    CUBE(1, 1),       // 1x1 cube (~80dp x 88dp) - MIN SIZE
    TALL_1X2(1, 2),   // 1x2 vertical strip (~80dp x 190dp)
    TALL_1X3(1, 3),   // 1x3 vertical strip (~80dp x 292dp)
    TALL(1, 4),       // 1x4 vertical strip (~80dp x 394dp)

    // 2-column widgets
    HALF(2, 1),       // 2x1 half row (~170dp x 88dp) - FLUSH with CUBE!
    HALF_2X2(2, 2),   // 2x2 half block (~170dp x 190dp)
    HALF_2X3(2, 3),   // 2x3 half tall (~170dp x 292dp)
    HALF_2X4(2, 4),   // 2x4 half tower (~170dp x 394dp)

    // 3-column widgets
    COL3_1(3, 1),     // 3x1 (3/4 width x 88dp) - FLUSH with CUBE & HALF!
    COL3_2(3, 2),     // 3x2 (3/4 width x 190dp)
    COL3_3(3, 3),     // 3x3 (3/4 width x 292dp)
    COL3_4(3, 4),     // 3x4 (3/4 width x 394dp)

    // 4-column (full width) widgets
    SLIM(4, 1),       // 4x1 full width slim banner (~360dp x 88dp) - FLUSH with row 1!
    WIDE(4, 2),       // 4x2 full width (~360dp x 190dp)
    MAX(4, 3),        // 4x3 full width (~360dp x 292dp)
    MAX_4X4(4, 4);    // 4x4 full width (~360dp x 394dp)

    val label: String get() = "${cols}x${rows}"

    companion object {
        fun from(cols: Int, rows: Int): WidgetSize {
            val c = cols.coerceIn(1, 4)
            val r = rows.coerceIn(1, 4)
            return entries.firstOrNull { it.cols == c && it.rows == r } ?: when (c) {
                1 -> when (r) { 1 -> CUBE; 2 -> TALL_1X2; 3 -> TALL_1X3; else -> TALL }
                2 -> when (r) { 1 -> HALF; 2 -> HALF_2X2; 3 -> HALF_2X3; else -> HALF_2X4 }
                3 -> when (r) { 1 -> COL3_1; 2 -> COL3_2; 3 -> COL3_3; else -> COL3_4 }
                else -> when (r) { 1 -> SLIM; 2 -> WIDE; 3 -> MAX; else -> MAX_4X4 }
            }
        }

        fun parse(str: String): WidgetSize {
            return try {
                valueOf(str)
            } catch (_: Exception) {
                when (str) {
                    "1x1" -> CUBE
                    "1x2" -> TALL_1X2
                    "1x3" -> TALL_1X3
                    "1x4" -> TALL
                    "2x1" -> HALF
                    "2x2" -> HALF_2X2
                    "2x3" -> HALF_2X3
                    "2x4" -> HALF_2X4
                    "3x1" -> COL3_1
                    "3x2" -> COL3_2
                    "3x3" -> COL3_3
                    "3x4" -> COL3_4
                    "4x1" -> SLIM
                    "4x2" -> WIDE
                    "4x3" -> MAX
                    "4x4" -> MAX_4X4
                    "FULL" -> WIDE
                    else -> WIDE
                }
            }
        }
    }
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
                    val size = WidgetSize.parse(sizeStr)
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
            val units = item.size.cols
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

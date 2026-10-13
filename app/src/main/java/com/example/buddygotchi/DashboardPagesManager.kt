package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class DashboardPageType {
    CUSTOM_GRID,
    MAIN,
    SQUAD
}

data class DashboardPageItem(
    val id: String,
    val title: String,
    val type: DashboardPageType,
    val canDelete: Boolean = true
)

data class DashboardPageTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val defaultLayout: List<DashboardWidgetItem>
)

object DashboardPageTemplates {
    val NOTES = DashboardPageTemplate(
        id = "notes",
        name = "NOTES",
        description = "Notes & Password vault in place",
        iconEmoji = "📝",
        defaultLayout = listOf(
            DashboardWidgetItem(DashboardWidgetId.NOTES, WidgetSize.MAX),
            DashboardWidgetItem(DashboardWidgetId.PASSWORDS, WidgetSize.WIDE)
        )
    )

    val WELLNESS = DashboardPageTemplate(
        id = "wellness",
        name = "WELLNESS",
        description = "Water tracker & Buddy Gotchi",
        iconEmoji = "💧",
        defaultLayout = listOf(
            DashboardWidgetItem(DashboardWidgetId.WATER, WidgetSize.MAX),
            DashboardWidgetItem(DashboardWidgetId.BUDDY, WidgetSize.HALF)
        )
    )

    val FOCUS = DashboardPageTemplate(
        id = "focus",
        name = "FOCUS",
        description = "Today's tasks & next priority",
        iconEmoji = "⚡",
        defaultLayout = listOf(
            DashboardWidgetItem(DashboardWidgetId.TODAY, WidgetSize.WIDE),
            DashboardWidgetItem(DashboardWidgetId.NEXT, WidgetSize.HALF),
            DashboardWidgetItem(DashboardWidgetId.BUDDY, WidgetSize.HALF)
        )
    )

    val BLANK = DashboardPageTemplate(
        id = "blank",
        name = "BLANK",
        description = "Clean empty modular grid",
        iconEmoji = "◻",
        defaultLayout = emptyList()
    )

    val TEMPLATES = listOf(NOTES, WELLNESS, FOCUS, BLANK)
}

object DashboardPagesManager {
    private const val PREFS_KEY_PAGES = "dashboard_pages_list_v2"
    private const val PREFS_PAGE_LAYOUT_PREFIX = "dashboard_page_layout_"

    val DEFAULT_PAGES = listOf(
        DashboardPageItem(id = "page_left", title = "LEFT", type = DashboardPageType.CUSTOM_GRID, canDelete = false),
        DashboardPageItem(id = "page_main", title = "MAIN", type = DashboardPageType.MAIN, canDelete = false),
        DashboardPageItem(id = "page_squad", title = "SQUAD", type = DashboardPageType.SQUAD, canDelete = false)
    )

    fun loadPages(context: Context): List<DashboardPageItem> {
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString(PREFS_KEY_PAGES, null) ?: return DEFAULT_PAGES
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<DashboardPageItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val typeStr = obj.optString("type", DashboardPageType.CUSTOM_GRID.name)
                val type = try { DashboardPageType.valueOf(typeStr) } catch (_: Exception) { DashboardPageType.CUSTOM_GRID }
                val canDelete = obj.optBoolean("canDelete", type == DashboardPageType.CUSTOM_GRID && id != "page_left")
                list.add(DashboardPageItem(id, title, type, canDelete))
            }
            if (list.isEmpty()) DEFAULT_PAGES else list
        } catch (_: Exception) {
            DEFAULT_PAGES
        }
    }

    fun savePages(context: Context, pages: List<DashboardPageItem>) {
        val jsonArray = JSONArray()
        pages.forEach { page ->
            val obj = JSONObject()
            obj.put("id", page.id)
            obj.put("title", page.title)
            obj.put("type", page.type.name)
            obj.put("canDelete", page.canDelete)
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY_PAGES, jsonArray.toString()).apply()
    }

    fun createPage(context: Context, template: DashboardPageTemplate, customTitle: String? = null): DashboardPageItem {
        val title = customTitle?.takeIf { it.isNotBlank() } ?: template.name
        val newId = "page_${System.currentTimeMillis()}"
        val newPage = DashboardPageItem(
            id = newId,
            title = title,
            type = DashboardPageType.CUSTOM_GRID,
            canDelete = true
        )
        // Persist template default layout
        saveCustomPageLayout(context, newId, template.defaultLayout)
        val current = loadPages(context).toMutableList()
        current.add(newPage)
        savePages(context, current)
        return newPage
    }

    fun movePage(context: Context, fromIndex: Int, toIndex: Int): List<DashboardPageItem> {
        val current = loadPages(context).toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            savePages(context, current)
        }
        return current
    }

    fun deletePage(context: Context, pageId: String): List<DashboardPageItem> {
        val current = loadPages(context).filter { it.id != pageId || !it.canDelete }
        savePages(context, current)
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("$PREFS_PAGE_LAYOUT_PREFIX$pageId").apply()
        return current
    }

    fun loadCustomPageLayout(context: Context, pageId: String): List<DashboardWidgetItem> {
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("$PREFS_PAGE_LAYOUT_PREFIX$pageId", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<DashboardWidgetItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val idStr = obj.optString("widget", "")
                val sizeStr = obj.optString("size", "WIDE")
                val id = try { DashboardWidgetId.valueOf(idStr) } catch (_: Exception) { null }
                val size = if (id == DashboardWidgetId.CLOCK) WidgetSize.MAX else WidgetSize.parse(sizeStr)
                if (id != null) {
                    list.add(DashboardWidgetItem(id, size))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomPageLayout(context: Context, pageId: String, layout: List<DashboardWidgetItem>) {
        val jsonArray = JSONArray()
        layout.forEach { item ->
            val obj = JSONObject()
            obj.put("widget", item.widgetId.name)
            obj.put("size", item.size.name)
            jsonArray.put(obj)
        }
        val prefs = context.getSharedPreferences("buddygotchi_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("$PREFS_PAGE_LAYOUT_PREFIX$pageId", jsonArray.toString()).apply()
    }

    fun loadAllCustomLayouts(context: Context, pages: List<DashboardPageItem>): Map<String, List<DashboardWidgetItem>> {
        val map = mutableMapOf<String, List<DashboardWidgetItem>>()
        pages.forEach { page ->
            if (page.id != "page_left" && page.id != "page_main" && page.type == DashboardPageType.CUSTOM_GRID) {
                map[page.id] = loadCustomPageLayout(context, page.id)
            }
        }
        return map
    }
}

package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class GroceryViewMode {
    LIST,
    GRID
}

data class GroceryItemEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

data class GroceryList(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val entries: List<GroceryItemEntry> = emptyList(),
    val viewMode: GroceryViewMode = GroceryViewMode.LIST,
    val updatedAt: Long = System.currentTimeMillis()
)

object GroceryManager {
    private const val PREFS_NAME = "buddygotchi_grocery_prefs"
    private const val KEY_LISTS = "saved_grocery_lists"

    /**
     * Stable sort that keeps all unchecked items at the top in their original order
     * and moves all checked items to the bottom of the list/grid.
     */
    fun sortEntries(entries: List<GroceryItemEntry>): List<GroceryItemEntry> {
        return entries.sortedBy { it.isChecked }
    }

    fun getLists(context: Context): List<GroceryList> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_LISTS, null)
        if (jsonString == null) {
            val starterLists = listOf(
                GroceryList(
                    id = "starter_grocery_1",
                    title = "WEEKLY GROCERIES",
                    entries = listOf(
                        GroceryItemEntry(id = "item_1", text = "Oat Milk", isChecked = false),
                        GroceryItemEntry(id = "item_2", text = "Avocados", isChecked = false),
                        GroceryItemEntry(id = "item_3", text = "Sourdough Bread", isChecked = false),
                        GroceryItemEntry(id = "item_4", text = "Espresso Beans", isChecked = false)
                    ),
                    viewMode = GroceryViewMode.LIST,
                    updatedAt = System.currentTimeMillis()
                )
            )
            saveLists(context, starterLists)
            return starterLists
        }

        val list = mutableListOf<GroceryList>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val entriesArray = obj.optJSONArray("entries") ?: JSONArray()
                val entries = mutableListOf<GroceryItemEntry>()
                for (j in 0 until entriesArray.length()) {
                    val entryObj = entriesArray.getJSONObject(j)
                    entries.add(
                        GroceryItemEntry(
                            id = entryObj.optString("id", UUID.randomUUID().toString()),
                            text = entryObj.optString("text", ""),
                            isChecked = entryObj.optBoolean("isChecked", false)
                        )
                    )
                }
                val modeStr = obj.optString("viewMode", GroceryViewMode.LIST.name)
                val mode = try {
                    GroceryViewMode.valueOf(modeStr)
                } catch (_: Exception) {
                    GroceryViewMode.LIST
                }
                list.add(
                    GroceryList(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        entries = sortEntries(entries),
                        viewMode = mode,
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun saveLists(context: Context, lists: List<GroceryList>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        lists.forEach { groceryList ->
            val entriesArray = JSONArray()
            sortEntries(groceryList.entries).forEach { entry ->
                val entryObj = JSONObject().apply {
                    put("id", entry.id)
                    put("text", entry.text)
                    put("isChecked", entry.isChecked)
                }
                entriesArray.put(entryObj)
            }
            val obj = JSONObject().apply {
                put("id", groceryList.id)
                put("title", groceryList.title)
                put("entries", entriesArray)
                put("viewMode", groceryList.viewMode.name)
                put("updatedAt", groceryList.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
    }

    fun upsertList(context: Context, groceryList: GroceryList): List<GroceryList> {
        val current = getLists(context).toMutableList()
        val sortedList = groceryList.copy(
            entries = sortEntries(groceryList.entries),
            updatedAt = System.currentTimeMillis()
        )
        val index = current.indexOfFirst { it.id == groceryList.id }
        if (index != -1) {
            current[index] = sortedList
        } else {
            current.add(0, sortedList)
        }
        saveLists(context, current)
        return current.sortedByDescending { it.updatedAt }
    }

    fun deleteList(context: Context, id: String): List<GroceryList> {
        val current = getLists(context).filter { it.id != id }
        saveLists(context, current)
        return current
    }
}

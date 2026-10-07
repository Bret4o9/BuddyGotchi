package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

object NotesManager {
    private const val PREFS_NAME = "buddygotchi_notes_prefs"
    private const val KEY_NOTES = "saved_notes"

    fun getNotes(context: Context): List<NoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_NOTES, null)
        if (jsonString == null) {
            // Provide a starter note
            val starterNotes = listOf(
                NoteItem(
                    id = "starter_1",
                    title = "QUICK THOUGHTS",
                    content = "Welcome to your Nothing Notes.\nTap to expand full screen and capture your ideas, focus lists, and daily thoughts.",
                    updatedAt = System.currentTimeMillis()
                )
            )
            saveNotes(context, starterNotes)
            return starterNotes
        }

        val list = mutableListOf<NoteItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun saveNotes(context: Context, notes: List<NoteItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("updatedAt", note.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun upsertNote(context: Context, note: NoteItem): List<NoteItem> {
        val current = getNotes(context).toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index != -1) {
            current[index] = note.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, note.copy(updatedAt = System.currentTimeMillis()))
        }
        saveNotes(context, current)
        return current.sortedByDescending { it.updatedAt }
    }

    fun deleteNote(context: Context, id: String): List<NoteItem> {
        val current = getNotes(context).filter { it.id != id }
        saveNotes(context, current)
        return current
    }
}

package com.example.buddygotchi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PasswordItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val password: String = "",
    val username: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

object PasswordsManager {
    private const val PREFS_NAME = "buddygotchi_passwords_prefs"
    private const val KEY_PASSWORDS = "saved_passwords"

    fun getPasswords(context: Context): List<PasswordItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_PASSWORDS, null)
        if (jsonString == null) {
            // Provide starter passwords
            val starterPasswords = listOf(
                PasswordItem(
                    id = "starter_pass_1",
                    title = "GITHUB",
                    password = "ghp_secureToken#2026",
                    username = "Bret4o9",
                    updatedAt = System.currentTimeMillis()
                ),
                PasswordItem(
                    id = "starter_pass_2",
                    title = "WI-FI // HOME",
                    password = "CyberNothing!8421",
                    username = "Nothing_5G",
                    updatedAt = System.currentTimeMillis() - 60000
                ),
                PasswordItem(
                    id = "starter_pass_3",
                    title = "PROTON MAIL",
                    password = "v$8K#mQ2xL8@pW4!",
                    username = "denis@proton.me",
                    updatedAt = System.currentTimeMillis() - 120000
                )
            )
            savePasswords(context, starterPasswords)
            return starterPasswords
        }

        val list = mutableListOf<PasswordItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PasswordItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        password = obj.optString("password", ""),
                        username = obj.optString("username", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun savePasswords(context: Context, passwords: List<PasswordItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        passwords.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("password", item.password)
                put("username", item.username)
                put("updatedAt", item.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PASSWORDS, array.toString()).apply()
    }

    fun upsertPassword(context: Context, item: PasswordItem): List<PasswordItem> {
        val current = getPasswords(context).toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index != -1) {
            current[index] = item.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, item.copy(updatedAt = System.currentTimeMillis()))
        }
        savePasswords(context, current)
        return current.sortedByDescending { it.updatedAt }
    }

    fun deletePassword(context: Context, id: String): List<PasswordItem> {
        val current = getPasswords(context).filter { it.id != id }
        savePasswords(context, current)
        return current
    }

    /**
     * Generates a strong random password matching Nothing industrial aesthetics.
     */
    fun generatePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = java.security.SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}

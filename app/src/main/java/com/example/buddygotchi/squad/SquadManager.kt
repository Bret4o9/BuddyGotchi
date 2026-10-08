package com.example.buddygotchi.squad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

object SquadManager {
    private const val PREFS_NAME = "buddygotchi_squad_prefs"
    private const val KEY_PROFILE = "user_profile_v1"
    private const val KEY_FRIENDS = "friends_list_v1"
    private const val KEY_EVENTS = "group_events_v1"

    fun getProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PROFILE, null)
        if (jsonStr != null) {
            return try {
                UserProfile.fromJson(JSONObject(jsonStr))
            } catch (_: Exception) {
                generateDefaultProfile(context)
            }
        }
        return generateDefaultProfile(context)
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE, profile.toJson().toString()).apply()
        try {
            FirebaseSquadSync.syncProfile(context)
        } catch (_: Exception) {}
    }

    private fun generateDefaultProfile(context: Context): UserProfile {
        val randomDigits = Random.nextInt(1000, 9999)
        val defaultProfile = UserProfile(
            buddyCode = "BG-$randomDigits",
            displayName = "DENIS",
            level = 14,
            avatarEmoji = "👾",
            statusText = "ONLINE // READY"
        )
        saveProfile(context, defaultProfile)
        return defaultProfile
    }

    fun getFriends(context: Context): List<SquadFriend> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_FRIENDS, null)
        if (jsonStr == null) {
            val defaults = listOf(
                SquadFriend("BG-4182", "ALEX", 12, isOnline = true),
                SquadFriend("BG-7731", "MAYA", 16, isOnline = true),
                SquadFriend("BG-9055", "KAI", 8, isOnline = false)
            )
            saveFriends(context, defaults)
            return defaults
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SquadFriend>()
            for (i in 0 until array.length()) {
                list.add(SquadFriend.fromJson(array.getJSONObject(i)))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveFriends(context: Context, friends: List<SquadFriend>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        friends.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_FRIENDS, array.toString()).apply()
    }

    fun addFriend(context: Context, buddyCode: String, displayName: String): SquadFriend {
        val existing = getFriends(context).toMutableList()
        val cleanedCode = buddyCode.trim().uppercase()
        val cleanedName = if (displayName.isNotBlank()) displayName.trim().uppercase() else "FRIEND"
        
        val found = existing.find { it.buddyCode == cleanedCode }
        if (found != null) {
            return found
        }
        val newFriend = SquadFriend(
            buddyCode = cleanedCode,
            displayName = cleanedName,
            level = Random.nextInt(5, 20),
            isOnline = true
        )
        existing.add(0, newFriend)
        saveFriends(context, existing)
        return newFriend
    }

    fun removeFriend(context: Context, buddyCode: String) {
        val existing = getFriends(context).filter { it.buddyCode != buddyCode }
        saveFriends(context, existing)
    }

    fun getEvents(context: Context): List<GroupEvent> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_EVENTS, null)
        if (jsonStr == null) {
            val profile = getProfile(context)
            // Seed sample event tomorrow at 18:00
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val defaultEvent = GroupEvent(
                id = "sample_event_1",
                title = "SQUAD WORKOUT",
                location = "CENTRAL FITNESS // ZONE 3",
                eventTime = cal.timeInMillis,
                reminderOffsetMinutes = 30,
                hostBuddyCode = "BG-7731",
                hostDisplayName = "MAYA",
                participants = listOf(
                    EventParticipant("BG-7731", "MAYA", ParticipantStatus.ACCEPTED, isHost = true),
                    EventParticipant(profile.buddyCode, profile.displayName, ParticipantStatus.ACCEPTED),
                    EventParticipant("BG-4182", "ALEX", ParticipantStatus.PENDING)
                )
            )
            val defaults = listOf(defaultEvent)
            saveEvents(context, defaults)
            GroupEventScheduler.scheduleEventReminder(context, defaultEvent)
            return defaults
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<GroupEvent>()
            for (i in 0 until array.length()) {
                list.add(GroupEvent.fromJson(array.getJSONObject(i)))
            }
            list.sortedBy { it.eventTime }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveEvents(context: Context, events: List<GroupEvent>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        events.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    fun createEvent(context: Context, event: GroupEvent): List<GroupEvent> {
        val events = getEvents(context).toMutableList()
        events.add(0, event)
        saveEvents(context, events)
        GroupEventScheduler.scheduleEventReminder(context, event)
        try {
            FirebaseSquadSync.publishEvent(event)
        } catch (_: Exception) {}
        return events.sortedBy { it.eventTime }
    }

    fun respondToEvent(context: Context, eventId: String, status: ParticipantStatus): List<GroupEvent> {
        val profile = getProfile(context)
        val events = getEvents(context).map { ev ->
            if (ev.id == eventId) {
                val updatedParts = ev.participants.map { part ->
                    if (part.buddyCode == profile.buddyCode) {
                        part.copy(status = status)
                    } else part
                }
                val updatedEv = ev.copy(participants = updatedParts)
                if (status == ParticipantStatus.ACCEPTED) {
                    GroupEventScheduler.scheduleEventReminder(context, updatedEv)
                } else if (status == ParticipantStatus.DECLINED) {
                    GroupEventScheduler.cancelEventReminder(context, updatedEv.id)
                }
                updatedEv
            } else ev
        }
        saveEvents(context, events)
        try {
            FirebaseSquadSync.updateParticipantStatus(eventId, profile.buddyCode, status)
        } catch (_: Exception) {}
        return events
    }

    fun deleteEvent(context: Context, eventId: String): List<GroupEvent> {
        GroupEventScheduler.cancelEventReminder(context, eventId)
        val events = getEvents(context).filter { it.id != eventId }
        saveEvents(context, events)
        try {
            FirebaseSquadSync.deleteEvent(eventId)
        } catch (_: Exception) {}
        return events
    }
}

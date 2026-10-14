package com.example.buddygotchi.squad

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserProfile(
    val uid: String = UUID.randomUUID().toString(),
    val buddyCode: String,
    val displayName: String,
    val level: Int = 14,
    val avatarEmoji: String = "👾",
    val statusText: String = "ONLINE // READY"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uid", uid)
        put("buddyCode", buddyCode)
        put("displayName", displayName)
        put("level", level)
        put("avatarEmoji", avatarEmoji)
        put("statusText", statusText)
    }

    companion object {
        fun fromJson(json: JSONObject): UserProfile = UserProfile(
            uid = json.optString("uid", UUID.randomUUID().toString()),
            buddyCode = json.optString("buddyCode", "BG-8421"),
            displayName = json.optString("displayName", "DENIS"),
            level = json.optInt("level", 14),
            avatarEmoji = json.optString("avatarEmoji", "👾"),
            statusText = json.optString("statusText", "ONLINE // READY")
        )
    }
}

data class SquadFriend(
    val buddyCode: String,
    val displayName: String,
    val level: Int,
    val isOnline: Boolean = true,
    val avatarEmoji: String = "👾",
    val buddyColorHex: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("buddyCode", buddyCode)
        put("displayName", displayName)
        put("level", level)
        put("isOnline", isOnline)
        put("avatarEmoji", avatarEmoji)
        if (buddyColorHex != null) put("buddyColorHex", buddyColorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): SquadFriend = SquadFriend(
            buddyCode = json.getString("buddyCode"),
            displayName = json.getString("displayName"),
            level = json.optInt("level", 1),
            isOnline = json.optBoolean("isOnline", true),
            avatarEmoji = json.optString("avatarEmoji", "👾"),
            buddyColorHex = if (json.has("buddyColorHex") && !json.isNull("buddyColorHex")) json.optString("buddyColorHex") else null
        )
    }
}

enum class ParticipantStatus {
    ACCEPTED,
    PENDING,
    DECLINED
}

data class EventParticipant(
    val buddyCode: String,
    val displayName: String,
    val status: ParticipantStatus = ParticipantStatus.PENDING,
    val isHost: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("buddyCode", buddyCode)
        put("displayName", displayName)
        put("status", status.name)
        put("isHost", isHost)
    }

    companion object {
        fun fromJson(json: JSONObject): EventParticipant = EventParticipant(
            buddyCode = json.getString("buddyCode"),
            displayName = json.getString("displayName"),
            status = try {
                ParticipantStatus.valueOf(json.optString("status", "PENDING"))
            } catch (_: Exception) {
                ParticipantStatus.PENDING
            },
            isHost = json.optBoolean("isHost", false)
        )
    }
}

data class GroupEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val eventTime: Long, // timestamp ms
    val reminderOffsetMinutes: Int = 15,
    val hostBuddyCode: String,
    val hostDisplayName: String,
    val participants: List<EventParticipant> = emptyList(),
    val isCancelled: Boolean = false
) {
    val reminderTriggerTime: Long
        get() = eventTime - (reminderOffsetMinutes * 60 * 1000L)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("location", location)
        if (latitude != null) put("latitude", latitude)
        if (longitude != null) put("longitude", longitude)
        put("eventTime", eventTime)
        put("reminderOffsetMinutes", reminderOffsetMinutes)
        put("hostBuddyCode", hostBuddyCode)
        put("hostDisplayName", hostDisplayName)
        val pArray = JSONArray()
        participants.forEach { pArray.put(it.toJson()) }
        put("participants", pArray)
        put("isCancelled", isCancelled)
    }

    companion object {
        fun fromJson(json: JSONObject): GroupEvent {
            val pList = mutableListOf<EventParticipant>()
            val pArray = json.optJSONArray("participants")
            if (pArray != null) {
                for (i in 0 until pArray.length()) {
                    val pObj = pArray.optJSONObject(i)
                    if (pObj != null) {
                        pList.add(EventParticipant.fromJson(pObj))
                    }
                }
            }
            val lat = if (json.has("latitude") && !json.isNull("latitude")) json.optDouble("latitude") else null
            val lng = if (json.has("longitude") && !json.isNull("longitude")) json.optDouble("longitude") else null
            return GroupEvent(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.getString("title"),
                location = json.optString("location", "LOCATION"),
                latitude = lat,
                longitude = lng,
                eventTime = json.getLong("eventTime"),
                reminderOffsetMinutes = json.optInt("reminderOffsetMinutes", 15),
                hostBuddyCode = json.getString("hostBuddyCode"),
                hostDisplayName = json.getString("hostDisplayName"),
                participants = pList,
                isCancelled = json.optBoolean("isCancelled", false)
            )
        }
    }
}

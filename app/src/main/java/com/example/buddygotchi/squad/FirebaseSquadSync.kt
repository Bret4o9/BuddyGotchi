package com.example.buddygotchi.squad

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

object FirebaseSquadSync {
    private const val TAG = "FirebaseSquadSync"
    private const val USERS_COLLECTION = "users"
    private const val EVENTS_COLLECTION = "events"

    private var eventsListener: ListenerRegistration? = null
    private var isInitialized = false

    fun initialize(context: Context, onEventsUpdated: ((List<GroupEvent>) -> Unit)? = null) {
        if (isInitialized) return
        isInitialized = true

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "Anonymous Firebase Auth successful: ${it.user?.uid}")
                    syncProfile(context)
                    startEventsListener(context, onEventsUpdated)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Anonymous Auth failed: ${e.message}")
                    // Still attempt profile sync and listener
                    syncProfile(context)
                    startEventsListener(context, onEventsUpdated)
                }
        } else {
            syncProfile(context)
            startEventsListener(context, onEventsUpdated)
        }
    }

    fun syncProfile(context: Context) {
        val profile = SquadManager.getProfile(context)
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "buddyCode" to profile.buddyCode,
            "displayName" to profile.displayName,
            "level" to profile.level,
            "avatarEmoji" to profile.avatarEmoji,
            "statusText" to profile.statusText,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection(USERS_COLLECTION)
            .document(profile.buddyCode)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Profile synced to cloud: ${profile.buddyCode}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to sync profile: ${e.message}")
            }
    }

    fun publishEvent(event: GroupEvent) {
        val db = FirebaseFirestore.getInstance()
        val participantsList = event.participants.map {
            hashMapOf(
                "buddyCode" to it.buddyCode,
                "displayName" to it.displayName,
                "status" to it.status.name,
                "isHost" to it.isHost
            )
        }

        val eventData = hashMapOf(
            "id" to event.id,
            "title" to event.title,
            "location" to event.location,
            "eventTime" to event.eventTime,
            "reminderOffsetMinutes" to event.reminderOffsetMinutes,
            "hostBuddyCode" to event.hostBuddyCode,
            "hostDisplayName" to event.hostDisplayName,
            "participants" to participantsList,
            "participantCodes" to event.participants.map { it.buddyCode },
            "isCancelled" to event.isCancelled,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection(EVENTS_COLLECTION)
            .document(event.id)
            .set(eventData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Event published to cloud: ${event.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to publish event: ${e.message}")
            }
    }

    fun updateParticipantStatus(eventId: String, buddyCode: String, status: ParticipantStatus) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection(EVENTS_COLLECTION).document(eventId)

        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener
            @Suppress("UNCHECKED_CAST")
            val rawList = snapshot.get("participants") as? List<Map<String, Any>> ?: return@addOnSuccessListener
            val updatedList = rawList.map { p ->
                if (p["buddyCode"] == buddyCode) {
                    val mod = p.toMutableMap()
                    mod["status"] = status.name
                    mod
                } else p
            }
            docRef.update("participants", updatedList)
                .addOnSuccessListener { Log.d(TAG, "RSVP updated in cloud for $buddyCode") }
                .addOnFailureListener { e -> Log.w(TAG, "Failed to update RSVP: ${e.message}") }
        }
    }

    fun deleteEvent(eventId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(EVENTS_COLLECTION).document(eventId).delete()
            .addOnSuccessListener { Log.d(TAG, "Event deleted from cloud: $eventId") }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to delete event from cloud: ${e.message}") }
    }

    fun startEventsListener(context: Context, onEventsUpdated: ((List<GroupEvent>) -> Unit)? = null) {
        eventsListener?.remove()
        val profile = SquadManager.getProfile(context)
        val db = FirebaseFirestore.getInstance()

        // Realtime listener for events where current user is participant or host
        eventsListener = db.collection(EVENTS_COLLECTION)
            .whereArrayContains("participantCodes", profile.buddyCode)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener

                val remoteEvents = mutableListOf<GroupEvent>()
                for (doc in snapshots.documents) {
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: continue
                        val location = doc.getString("location") ?: "LOCATION"
                        val eventTime = doc.getLong("eventTime") ?: continue
                        val offsetMinutes = doc.getLong("reminderOffsetMinutes")?.toInt() ?: 15
                        val hostCode = doc.getString("hostBuddyCode") ?: ""
                        val hostName = doc.getString("hostDisplayName") ?: ""
                        val isCancelled = doc.getBoolean("isCancelled") ?: false

                        @Suppress("UNCHECKED_CAST")
                        val rawParticipants = doc.get("participants") as? List<Map<String, Any>> ?: emptyList()
                        val participants = rawParticipants.map { p ->
                            EventParticipant(
                                buddyCode = p["buddyCode"] as? String ?: "",
                                displayName = p["displayName"] as? String ?: "",
                                status = try {
                                    ParticipantStatus.valueOf(p["status"] as? String ?: "PENDING")
                                } catch (_: Exception) {
                                    ParticipantStatus.PENDING
                                },
                                isHost = p["isHost"] as? Boolean ?: false
                            )
                        }

                        val parsedEvent = GroupEvent(
                            id = id,
                            title = title,
                            location = location,
                            eventTime = eventTime,
                            reminderOffsetMinutes = offsetMinutes,
                            hostBuddyCode = hostCode,
                            hostDisplayName = hostName,
                            participants = participants,
                            isCancelled = isCancelled
                        )
                        remoteEvents.add(parsedEvent)

                        // If user accepted this event, ensure alarm is scheduled
                        val userPart = parsedEvent.participants.find { it.buddyCode == profile.buddyCode }
                        if (userPart?.status == ParticipantStatus.ACCEPTED) {
                            GroupEventScheduler.scheduleEventReminder(context, parsedEvent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing cloud event", e)
                    }
                }

                if (remoteEvents.isNotEmpty()) {
                    // Merge with local events
                    val local = SquadManager.getEvents(context)
                    val mergedMap = local.associateBy { it.id }.toMutableMap()
                    remoteEvents.forEach { mergedMap[it.id] = it }
                    val mergedList = mergedMap.values.sortedBy { it.eventTime }
                    SquadManager.saveEvents(context, mergedList)
                    onEventsUpdated?.invoke(mergedList)
                }
            }
    }

    fun findUserByBuddyCode(
        buddyCode: String,
        onResult: (SquadFriend?) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection(USERS_COLLECTION).document(buddyCode.trim().uppercase())
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val code = doc.getString("buddyCode") ?: buddyCode
                    val name = doc.getString("displayName") ?: "FRIEND"
                    val level = doc.getLong("level")?.toInt() ?: 1
                    onResult(SquadFriend(buddyCode = code, displayName = name, level = level, isOnline = true))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}

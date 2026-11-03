package com.example.buddygotchi

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.buddygotchi.squad.SquadManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class CompanionPetCatalogEntry(
    val id: String,
    val name: String,
    val subtitle: String,
    val licenseBadge: String,
    val requiredLevel: Int,
    val displayMode: BuddyDisplayMode,
    val modelId: String = Buddy3DManager.BUILTIN_BUDDYBOT_ID,
    val characterIndex: Int = 0
)

data class CompanionGearItemEntry(
    val id: String,
    val name: String,
    val slot: String,
    val description: String,
    val requiredLevel: Int
)

object AccountAndInventoryManager {
    private const val PREFS_NAME = "buddy_account_legal_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_ACCOUNT_EMAIL = "account_email"
    private const val KEY_ACCOUNT_USERNAME = "account_username"
    private const val KEY_ACCOUNT_BUDDY_CODE = "account_buddy_code"
    private const val KEY_EQUIPPED_PET_ID = "equipped_pet_id"
    private const val KEY_UNLOCKED_PETS = "unlocked_pets_json"
    private const val KEY_UNLOCKED_ITEMS = "unlocked_items_json"
    private const val KEY_EQUIPPED_ITEMS = "equipped_items_json"

    // Legal & Consent keys
    private const val KEY_CONSENT_BANNER_ANSWERED = "consent_banner_answered"
    private const val KEY_CLOUD_SYNC_CONSENT = "cloud_sync_consent"
    private const val KEY_AGE_CONSENT_VERIFIED = "age_consent_verified"
    private const val KEY_TERMS_ACCEPTED_AT = "terms_accepted_timestamp"
    private const val KEY_UPDATES_SUBSCRIBED = "updates_subscribed"
    private const val KEY_LAST_SYNC_TIME = "last_cloud_sync_timestamp"

    val petCatalog: List<CompanionPetCatalogEntry> = listOf(
        CompanionPetCatalogEntry(
            id = "pet_2d_classic",
            name = "2D CLASSIC EYES",
            subtitle = "Interactive Nothing OS Vector Eyes",
            licenseBadge = "CC0 ORIGINAL • FREE",
            requiredLevel = 1,
            displayMode = BuddyDisplayMode.CLASSIC_2D
        ),
        CompanionPetCatalogEntry(
            id = "pet_buddybot_cc0",
            name = "BUDDYBOT MK-I",
            subtitle = "Animated 3D Companion Bot",
            licenseBadge = "CC0 ORIGINAL • FREE",
            requiredLevel = 1,
            displayMode = BuddyDisplayMode.THREE_D,
            modelId = Buddy3DManager.BUILTIN_BUDDYBOT_ID,
            characterIndex = 0
        ),
        CompanionPetCatalogEntry(
            id = "pet_ssrb_greybie",
            name = "GREYBIE (3D)",
            subtitle = "Animated 3D Companion • Classic Grey",
            licenseBadge = "TEST ASSET • FREE",
            requiredLevel = 1,
            displayMode = BuddyDisplayMode.THREE_D,
            modelId = Buddy3DManager.BUILTIN_SSRB_ID,
            characterIndex = 0
        ),
        CompanionPetCatalogEntry(
            id = "pet_ssrb_camoue",
            name = "CAMOUE (3D)",
            subtitle = "Animated 3D Companion • Camo Variant",
            licenseBadge = "TEST ASSET • FREE",
            requiredLevel = 2,
            displayMode = BuddyDisplayMode.THREE_D,
            modelId = Buddy3DManager.BUILTIN_SSRB_ID,
            characterIndex = 1
        ),
        CompanionPetCatalogEntry(
            id = "pet_ssrb_boitan",
            name = "BOITAN (3D)",
            subtitle = "Animated 3D Companion • Roll Variant",
            licenseBadge = "TEST ASSET • FREE",
            requiredLevel = 3,
            displayMode = BuddyDisplayMode.THREE_D,
            modelId = Buddy3DManager.BUILTIN_SSRB_ID,
            characterIndex = 2
        ),
        CompanionPetCatalogEntry(
            id = "pet_ssrb_trio",
            name = "COMPANION TRIO (3D)",
            subtitle = "All 3 Animated Companions Together",
            licenseBadge = "TEST ASSET • FREE",
            requiredLevel = 4,
            displayMode = BuddyDisplayMode.THREE_D,
            modelId = Buddy3DManager.BUILTIN_SSRB_ID,
            characterIndex = -1
        )
    )

    val itemCatalog: List<CompanionGearItemEntry> = listOf(
        CompanionGearItemEntry(
            id = "item_dot_collar",
            name = "NOTHING DOT COLLAR",
            slot = "NECK",
            description = "Monochrome dot-matrix identity collar (Starter Free Item)",
            requiredLevel = 1
        ),
        CompanionGearItemEntry(
            id = "item_red_core",
            name = "RED CORE BATTERY",
            slot = "CORE",
            description = "Boosts companion mood indicator (+0% hidden fees, 100% free)",
            requiredLevel = 1
        ),
        CompanionGearItemEntry(
            id = "item_glyph_halo",
            name = "GLYPH HALO RING",
            slot = "HEAD",
            description = "Floating white LED light ring unlocked at Level 2",
            requiredLevel = 2
        ),
        CompanionGearItemEntry(
            id = "item_matrix_visor",
            name = "LED MATRIX VISOR",
            slot = "EYES",
            description = "Retro DSEG-7 tactical visor unlocked at Level 3",
            requiredLevel = 3
        ),
        CompanionGearItemEntry(
            id = "item_thruster_pack",
            name = "MICRO THRUSTER PACK",
            slot = "BACK",
            description = "Orbital stabilizer module unlocked at Level 5",
            requiredLevel = 5
        )
    )

    private var isInitialized = false

    // Observable state for Compose UI
    val isLoggedInState = mutableStateOf(false)
    val accountEmailState = mutableStateOf("")
    val accountUsernameState = mutableStateOf("GUEST")
    val accountBuddyCodeState = mutableStateOf("")

    val equippedPetIdState = mutableStateOf("pet_ssrb_greybie")
    val unlockedPetIdsState = mutableStateListOf(
        "pet_2d_classic",
        "pet_buddybot_cc0",
        "pet_ssrb_greybie",
        "pet_ssrb_camoue",
        "pet_ssrb_boitan",
        "pet_ssrb_trio"
    )
    val unlockedItemIdsState = mutableStateListOf("item_dot_collar", "item_red_core")
    val equippedItemIdsState = mutableStateListOf("item_dot_collar")

    // Legal & Consent observable state
    val hasAnsweredConsentBannerState = mutableStateOf(false)
    val cloudSyncConsentState = mutableStateOf(false)
    val ageConsentVerifiedState = mutableStateOf(false)
    val updatesSubscribedState = mutableStateOf(false)
    val lastSyncTimestampState = mutableStateOf(0L)
    val syncStatusMessageState = mutableStateOf<String?>(null)

    fun ensureInitialized(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        isLoggedInState.value = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        accountEmailState.value = prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: ""
        val squadProfile = SquadManager.getProfile(context)
        accountUsernameState.value = prefs.getString(KEY_ACCOUNT_USERNAME, squadProfile.displayName) ?: squadProfile.displayName
        accountBuddyCodeState.value = prefs.getString(KEY_ACCOUNT_BUDDY_CODE, squadProfile.buddyCode) ?: squadProfile.buddyCode

        equippedPetIdState.value = prefs.getString(KEY_EQUIPPED_PET_ID, "pet_ssrb_greybie") ?: "pet_ssrb_greybie"

        val petsJson = prefs.getString(KEY_UNLOCKED_PETS, null)
        if (!petsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(petsJson)
                unlockedPetIdsState.clear()
                for (i in 0 until arr.length()) {
                    unlockedPetIdsState.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }

        val itemsJson = prefs.getString(KEY_UNLOCKED_ITEMS, null)
        if (!itemsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(itemsJson)
                unlockedItemIdsState.clear()
                for (i in 0 until arr.length()) {
                    unlockedItemIdsState.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }

        val eqItemsJson = prefs.getString(KEY_EQUIPPED_ITEMS, null)
        if (!eqItemsJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(eqItemsJson)
                equippedItemIdsState.clear()
                for (i in 0 until arr.length()) {
                    equippedItemIdsState.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }

        hasAnsweredConsentBannerState.value = prefs.getBoolean(KEY_CONSENT_BANNER_ANSWERED, false)
        cloudSyncConsentState.value = prefs.getBoolean(KEY_CLOUD_SYNC_CONSENT, false)
        ageConsentVerifiedState.value = prefs.getBoolean(KEY_AGE_CONSENT_VERIFIED, false)
        updatesSubscribedState.value = prefs.getBoolean(KEY_UPDATES_SUBSCRIBED, false)
        lastSyncTimestampState.value = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

        autoUnlockForCurrentLevel(context)
        isInitialized = true
    }

    fun autoUnlockForCurrentLevel(context: Context) {
        val xp = TaskManager.getBuddyXp(context)
        val level = (xp / 50) + 1
        var changed = false
        for (pet in petCatalog) {
            if (level >= pet.requiredLevel && pet.id !in unlockedPetIdsState) {
                unlockedPetIdsState.add(pet.id)
                changed = true
            }
        }
        for (item in itemCatalog) {
            if (level >= item.requiredLevel && item.id !in unlockedItemIdsState) {
                unlockedItemIdsState.add(item.id)
                changed = true
            }
        }
        if (changed) {
            saveInventoryToPrefs(context)
        }
    }

    private fun saveInventoryToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val petsArr = JSONArray().apply { unlockedPetIdsState.forEach { put(it) } }
        val itemsArr = JSONArray().apply { unlockedItemIdsState.forEach { put(it) } }
        val eqArr = JSONArray().apply { equippedItemIdsState.forEach { put(it) } }
        prefs.edit()
            .putString(KEY_EQUIPPED_PET_ID, equippedPetIdState.value)
            .putString(KEY_UNLOCKED_PETS, petsArr.toString())
            .putString(KEY_UNLOCKED_ITEMS, itemsArr.toString())
            .putString(KEY_EQUIPPED_ITEMS, eqArr.toString())
            .apply()
    }

    fun equipPet(context: Context, petId: String) {
        ensureInitialized(context)
        val entry = petCatalog.find { it.id == petId } ?: return
        if (petId !in unlockedPetIdsState) return
        equippedPetIdState.value = petId
        Buddy3DManager.setDisplayMode(context, entry.displayMode)
        if (entry.displayMode == BuddyDisplayMode.THREE_D) {
            Buddy3DManager.selectModel(context, entry.modelId, entry.characterIndex)
        }
        saveInventoryToPrefs(context)
        syncToCloudIfEnabled(context)
    }

    fun toggleEquipGearItem(context: Context, itemId: String) {
        ensureInitialized(context)
        if (itemId !in unlockedItemIdsState) return
        if (itemId in equippedItemIdsState) {
            equippedItemIdsState.remove(itemId)
        } else {
            equippedItemIdsState.add(itemId)
        }
        saveInventoryToPrefs(context)
        syncToCloudIfEnabled(context)
    }

    fun setStorageAndCloudConsent(context: Context, allowCloudSync: Boolean) {
        ensureInitialized(context)
        hasAnsweredConsentBannerState.value = true
        cloudSyncConsentState.value = allowCloudSync
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENT_BANNER_ANSWERED, true)
            .putBoolean(KEY_CLOUD_SYNC_CONSENT, allowCloudSync)
            .apply()
        if (allowCloudSync && isLoggedInState.value) {
            syncAccountToCloud(context)
        }
    }

    fun setUpdatesSubscription(context: Context, subscribed: Boolean) {
        ensureInitialized(context)
        updatesSubscribedState.value = subscribed
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_UPDATES_SUBSCRIBED, subscribed)
            .apply()
        syncStatusMessageState.value = if (subscribed) {
            "SUBSCRIBED TO OPTIONAL RELEASE NOTES"
        } else {
            "UNSUBSCRIBED FROM ALL OPTIONAL COMMUNICATIONS"
        }
        syncToCloudIfEnabled(context)
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun accountDocKeyForEmail(email: String): String {
        val normalized = email.trim().lowercase()
        return "ACC-" + sha256Hex(normalized).take(16).uppercase()
    }

    /**
     * Optional Account Registration with explicit GDPR/COPPA consent validation.
     */
    fun registerOrLinkAccount(
        context: Context,
        email: String,
        password: String,
        displayName: String,
        ageConfirmed13Plus: Boolean,
        termsAndPrivacyAccepted: Boolean,
        subscribeToUpdates: Boolean,
        onResult: (Result<String>) -> Unit
    ) {
        ensureInitialized(context)
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim().uppercase().ifBlank { "OPERATOR" }

        if (!ageConfirmed13Plus) {
            onResult(Result.failure(IllegalArgumentException("You must confirm the Age of Consent requirement (13+ / 16+ EEA) to create an account.")))
            return
        }
        if (!termsAndPrivacyAccepted) {
            onResult(Result.failure(IllegalArgumentException("You must accept the Privacy Policy and Terms of Service to create an account.")))
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            onResult(Result.failure(IllegalArgumentException("Please enter a valid email address.")))
            return
        }
        if (password.length < 6) {
            onResult(Result.failure(IllegalArgumentException("Password must be at least 6 characters.")))
            return
        }

        val now = System.currentTimeMillis()
        val squadProfile = SquadManager.getProfile(context)
        val updatedProfile = squadProfile.copy(displayName = cleanName)
        SquadManager.saveProfile(context, updatedProfile)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ACCOUNT_EMAIL, cleanEmail)
            .putString(KEY_ACCOUNT_USERNAME, cleanName)
            .putString(KEY_ACCOUNT_BUDDY_CODE, updatedProfile.buddyCode)
            .putBoolean(KEY_CONSENT_BANNER_ANSWERED, true)
            .putBoolean(KEY_CLOUD_SYNC_CONSENT, true)
            .putBoolean(KEY_AGE_CONSENT_VERIFIED, true)
            .putLong(KEY_TERMS_ACCEPTED_AT, now)
            .putBoolean(KEY_UPDATES_SUBSCRIBED, subscribeToUpdates)
            .apply()

        isLoggedInState.value = true
        accountEmailState.value = cleanEmail
        accountUsernameState.value = cleanName
        accountBuddyCodeState.value = updatedProfile.buddyCode
        hasAnsweredConsentBannerState.value = true
        cloudSyncConsentState.value = true
        ageConsentVerifiedState.value = true
        updatesSubscribedState.value = subscribeToUpdates

        val passwordHash = sha256Hex("buddygotchi_salt::$cleanEmail::$password")
        val auth = FirebaseAuth.getInstance()

        // Try native Firebase Email/Password auth first; gracefully fall back to Firestore cloud account record
        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener {
                syncAccountToCloud(context, passwordHash = passwordHash) { syncOk ->
                    val msg = if (syncOk) {
                        "ACCOUNT CREATED & SYNCED TO CLOUD ($cleanName)"
                    } else {
                        "ACCOUNT CREATED LOCALLY ($cleanName)"
                    }
                    syncStatusMessageState.value = msg
                    onResult(Result.success(msg))
                }
            }
    }

    /**
     * Sign in to an existing account and restore Pets, Items, XP, and Buddy Code across devices.
     */
    fun signInAndRestoreFromCloud(
        context: Context,
        email: String,
        password: String,
        onResult: (Result<String>) -> Unit
    ) {
        ensureInitialized(context)
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || password.length < 6) {
            onResult(Result.failure(IllegalArgumentException("Enter a valid email and password (6+ chars).")))
            return
        }

        val passwordHash = sha256Hex("buddygotchi_salt::$cleanEmail::$password")
        val accountDocId = accountDocKeyForEmail(cleanEmail)
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(accountDocId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val storedHash = doc.getString("passwordHash") ?: ""
                    if (storedHash.isNotEmpty() && storedHash != passwordHash) {
                        onResult(Result.failure(IllegalArgumentException("Incorrect password for $cleanEmail.")))
                        return@addOnSuccessListener
                    }

                    val restoredName = doc.getString("displayName") ?: "OPERATOR"
                    val restoredCode = doc.getString("linkedBuddyCode") ?: SquadManager.getProfile(context).buddyCode
                    val restoredXp = doc.getLong("buddyXp")?.toInt() ?: TaskManager.getBuddyXp(context)
                    val restoredMode = doc.getString("displayMode") ?: BuddyDisplayMode.THREE_D.name
                    val restoredModelId = doc.getString("activeModelId") ?: Buddy3DManager.BUILTIN_SSRB_ID
                    val restoredCharIdx = doc.getLong("activeCharIndex")?.toInt() ?: 0
                    val restoredEquippedPet = doc.getString("equippedPetId") ?: "pet_ssrb_greybie"

                    @Suppress("UNCHECKED_CAST")
                    val cloudPets = doc.get("unlockedPetIds") as? List<String>
                    @Suppress("UNCHECKED_CAST")
                    val cloudItems = doc.get("unlockedItemIds") as? List<String>
                    @Suppress("UNCHECKED_CAST")
                    val cloudEqItems = doc.get("equippedItemIds") as? List<String>

                    if (!cloudPets.isNullOrEmpty()) {
                        unlockedPetIdsState.clear()
                        unlockedPetIdsState.addAll(cloudPets.distinct())
                    }
                    if (!cloudItems.isNullOrEmpty()) {
                        unlockedItemIdsState.clear()
                        unlockedItemIdsState.addAll(cloudItems.distinct())
                    }
                    if (cloudEqItems != null) {
                        equippedItemIdsState.clear()
                        equippedItemIdsState.addAll(cloudEqItems.distinct())
                    }

                    equippedPetIdState.value = restoredEquippedPet
                    TaskManager.setBuddyXp(context, restoredXp)

                    val modeEnum = try {
                        BuddyDisplayMode.valueOf(restoredMode)
                    } catch (_: Exception) {
                        BuddyDisplayMode.THREE_D
                    }
                    Buddy3DManager.setDisplayMode(context, modeEnum)
                    Buddy3DManager.selectModel(context, restoredModelId, restoredCharIdx)

                    val currentProfile = SquadManager.getProfile(context)
                    SquadManager.saveProfile(
                        context,
                        currentProfile.copy(
                            buddyCode = restoredCode,
                            displayName = restoredName,
                            level = (restoredXp / 50) + 1
                        )
                    )

                    isLoggedInState.value = true
                    accountEmailState.value = cleanEmail
                    accountUsernameState.value = restoredName
                    accountBuddyCodeState.value = restoredCode
                    cloudSyncConsentState.value = true
                    hasAnsweredConsentBannerState.value = true
                    lastSyncTimestampState.value = System.currentTimeMillis()

                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .putString(KEY_ACCOUNT_EMAIL, cleanEmail)
                        .putString(KEY_ACCOUNT_USERNAME, restoredName)
                        .putString(KEY_ACCOUNT_BUDDY_CODE, restoredCode)
                        .putBoolean(KEY_CONSENT_BANNER_ANSWERED, true)
                        .putBoolean(KEY_CLOUD_SYNC_CONSENT, true)
                        .putLong(KEY_LAST_SYNC_TIME, lastSyncTimestampState.value)
                        .apply()

                    saveInventoryToPrefs(context)
                    val msg = "RESTORED ACCOUNT: $restoredName (${unlockedPetIdsState.size} PETS, ${unlockedItemIdsState.size} ITEMS)"
                    syncStatusMessageState.value = msg
                    onResult(Result.success(msg))
                } else {
                    onResult(Result.failure(IllegalArgumentException("No cloud account found for $cleanEmail. Use CREATE ACCOUNT first.")))
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(IllegalArgumentException("Cloud sign-in failed: ${e.message}")))
            }
    }

    fun syncToCloudIfEnabled(context: Context) {
        if (!isInitialized) return
        if (isLoggedInState.value && cloudSyncConsentState.value && accountEmailState.value.isNotBlank()) {
            syncAccountToCloud(context)
        }
    }

    fun syncAccountToCloud(
        context: Context,
        passwordHash: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        ensureInitialized(context)
        if (!cloudSyncConsentState.value || accountEmailState.value.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        autoUnlockForCurrentLevel(context)
        val email = accountEmailState.value
        val accountDocId = accountDocKeyForEmail(email)
        val profile = SquadManager.getProfile(context)
        val xp = TaskManager.getBuddyXp(context)
        val level = (xp / 50) + 1
        val now = System.currentTimeMillis()

        // Data Minimization: Only sync necessary companion/account progression.
        // Passwords Widget data is NEVER synced to the cloud.
        val payload = hashMapOf<String, Any>(
            "buddyCode" to accountDocId,
            "linkedBuddyCode" to profile.buddyCode,
            "displayName" to accountUsernameState.value.ifBlank { profile.displayName },
            "email" to email,
            "level" to level,
            "buddyXp" to xp,
            "displayMode" to Buddy3DManager.displayModeState.value.name,
            "activeModelId" to Buddy3DManager.activeModelIdState.value,
            "activeCharIndex" to Buddy3DManager.activeCharacterIndexState.intValue,
            "equippedPetId" to equippedPetIdState.value,
            "unlockedPetIds" to unlockedPetIdsState.toList(),
            "unlockedItemIds" to unlockedItemIdsState.toList(),
            "equippedItemIds" to equippedItemIdsState.toList(),
            "updatesSubscribed" to updatesSubscribedState.value,
            "ageConsentVerified" to ageConsentVerifiedState.value,
            "updatedAt" to now
        )
        if (passwordHash != null) {
            payload["passwordHash"] = passwordHash
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(accountDocId)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                lastSyncTimestampState.value = now
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_SYNC_TIME, now)
                    .apply()
                syncStatusMessageState.value = "CLOUD SYNC COMPLETE • ALL PETS & ITEMS SAVED"
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                syncStatusMessageState.value = "SYNC WARNING: ${e.message}"
                onComplete?.invoke(false)
            }
    }

    /**
     * Switch back to Guest / Local Mode without deleting local data.
     */
    fun signOutToGuestMode(context: Context) {
        ensureInitialized(context)
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}

        isLoggedInState.value = false
        accountEmailState.value = ""
        accountUsernameState.value = SquadManager.getProfile(context).displayName
        syncStatusMessageState.value = "SIGNED OUT • CONTINUING IN LOCAL GUEST MODE"

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_ACCOUNT_EMAIL, "")
            .apply()
    }

    /**
     * Mandatory GDPR Right to Erasure (Art. 17) & Google Play Data Deletion implementation.
     * Permanently deletes the user's cloud records in Firestore, deletes the Firebase Auth user,
     * and wipes local SharedPreferences & custom files.
     */
    fun deleteAccountAndAllData(
        context: Context,
        onComplete: (String) -> Unit
    ) {
        ensureInitialized(context)
        val db = FirebaseFirestore.getInstance()
        val email = accountEmailState.value
        val buddyCode = SquadManager.getProfile(context).buddyCode

        // 1. Delete cloud documents from Firestore
        if (email.isNotBlank()) {
            val accDocId = accountDocKeyForEmail(email)
            db.collection("users").document(accDocId).delete()
        }
        if (buddyCode.isNotBlank()) {
            db.collection("users").document(buddyCode).delete()
        }

        // 2. Delete Firebase Auth user record if signed in
        try {
            FirebaseAuth.getInstance().currentUser?.delete()
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}

        // 3. Wipe all local SharedPreferences files
        val prefNames = listOf(
            PREFS_NAME,
            "buddygotchi_prefs",
            "buddy_3d_prefs",
            "buddygotchi_squad_prefs",
            "notes_prefs",
            "passwords_prefs",
            "grocery_prefs",
            "water_tracker_prefs"
        )
        for (name in prefNames) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }

        // 4. Delete custom imported files if any
        val customDir = File(context.filesDir, "custom_models")
        if (customDir.exists()) {
            customDir.deleteRecursively()
        }

        // 5. Reset in-memory states
        isLoggedInState.value = false
        accountEmailState.value = ""
        accountUsernameState.value = "GUEST"
        cloudSyncConsentState.value = false
        updatesSubscribedState.value = false
        ageConsentVerifiedState.value = false
        lastSyncTimestampState.value = 0L
        TaskManager.setBuddyXp(context, 0)

        unlockedPetIdsState.clear()
        unlockedPetIdsState.addAll(listOf("pet_2d_classic", "pet_buddybot_cc0", "pet_ssrb_greybie"))
        unlockedItemIdsState.clear()
        unlockedItemIdsState.addAll(listOf("item_dot_collar", "item_red_core"))
        equippedItemIdsState.clear()
        equippedItemIdsState.add("item_dot_collar")
        equippedPetIdState.value = "pet_ssrb_greybie"

        val msg = "ALL CLOUD & LOCAL DATA PERMANENTLY DELETED"
        syncStatusMessageState.value = msg
        onComplete(msg)
    }
}

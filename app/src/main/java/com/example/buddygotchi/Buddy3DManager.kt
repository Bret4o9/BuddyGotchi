package com.example.buddygotchi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

enum class BuddyDisplayMode {
    CLASSIC_2D,
    THREE_D
}

data class Buddy3DModelEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val modelUrl: String,
    val format: String,
    val texturesBaseUrl: String,
    val isBuiltIn: Boolean = false,
    val characterNames: List<String> = emptyList()
)

object Buddy3DManager {
    private const val PREFS_NAME = "buddy_3d_prefs"
    private const val KEY_DISPLAY_MODE = "display_mode"
    private const val KEY_ACTIVE_MODEL_ID = "active_model_id"
    private const val KEY_ACTIVE_CHAR_INDEX = "active_char_index"
    private const val KEY_CUSTOM_MODELS = "custom_models_json"

    const val BUILTIN_SSRB_ID = "ssrbs_hololive"
    const val BUILTIN_BUDDYBOT_ID = "buddybot_cc0"

    val builtInBuddyBotModel = Buddy3DModelEntry(
        id = BUILTIN_BUDDYBOT_ID,
        title = "BUDDYBOT MK-I (FREE CC0)",
        subtitle = "GLTF • 100% Original Royalty-Free Asset",
        modelUrl = "https://buddy3d.local/assets/buddy3d/models/buddybot-cc0/buddybot.gltf",
        format = "gltf",
        texturesBaseUrl = "https://buddy3d.local/assets/buddy3d/models/buddybot-cc0/",
        isBuiltIn = true,
        characterNames = listOf("BuddyBot")
    )

    val builtInSsrbsModel = Buddy3DModelEntry(
        id = BUILTIN_SSRB_ID,
        title = "SSRB HOLOLIVE (TEST)",
        subtitle = "FBX • Greybie, Camoue & Boitan (Fan Test)",
        modelUrl = "https://buddy3d.local/assets/buddy3d/models/ssrbs-hololive/SSRB_Poi_PoseUp_01.fbx",
        format = "fbx",
        texturesBaseUrl = "https://buddy3d.local/assets/buddy3d/models/ssrbs-hololive/textures/",
        isBuiltIn = true,
        characterNames = listOf("Greybie", "Camoue", "Boitan")
    )

    /**
     * Built-in 3D character models shipped inside the app APK.
     *
     * HOW TO ADD A NEW BUILT-IN 3D CHARACTER:
     * 1. Create a folder in: app/src/main/assets/buddy3d/models/<your-folder>/
     * 2. Place your .fbx, .glb, .gltf, or .obj file inside that folder.
     * 3. (If textures are separate images) place them in: app/src/main/assets/buddy3d/models/<your-folder>/textures/
     * 4. Add a new Buddy3DModelEntry(...) to [builtInModels] below.
     */
    val builtInModels: List<Buddy3DModelEntry> = listOf(
        builtInSsrbsModel,
        builtInBuddyBotModel
    )

    private var isInitialized = false

    val displayModeState = mutableStateOf(BuddyDisplayMode.THREE_D)
    val activeModelIdState = mutableStateOf(BUILTIN_SSRB_ID)
    val activeCharacterIndexState = mutableIntStateOf(0) // 0=Greybie, 1=Camoue, 2=Boitan, -1=Trio
    val detectedCharactersState = mutableStateListOf("Greybie", "Camoue", "Boitan")
    val customModelsState = mutableStateListOf<Buddy3DModelEntry>()

    // True while the user is actively touching/dragging on the 3D Buddy viewport
    val isInteractingWith3DState = mutableStateOf(false)

    // Controlled by DashboardScreen & Lifecycle to freeze WebGL rendering when off-screen or minimized
    val isRenderingActiveState = mutableStateOf(true)

    fun ensureInitialized(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeStr = prefs.getString(KEY_DISPLAY_MODE, BuddyDisplayMode.THREE_D.name)
        displayModeState.value = try {
            BuddyDisplayMode.valueOf(modeStr ?: BuddyDisplayMode.THREE_D.name)
        } catch (_: Exception) {
            BuddyDisplayMode.THREE_D
        }

        activeModelIdState.value = prefs.getString(KEY_ACTIVE_MODEL_ID, BUILTIN_SSRB_ID) ?: BUILTIN_SSRB_ID
        activeCharacterIndexState.intValue = prefs.getInt(KEY_ACTIVE_CHAR_INDEX, 0)

        customModelsState.clear()
        val rawJson = prefs.getString(KEY_CUSTOM_MODELS, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val charsArr = obj.optJSONArray("characterNames")
                    val chars = mutableListOf<String>()
                    if (charsArr != null) {
                        for (j in 0 until charsArr.length()) {
                            chars.add(charsArr.getString(j))
                        }
                    }
                    customModelsState.add(
                        Buddy3DModelEntry(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            subtitle = obj.optString("subtitle", "Custom 3D Model"),
                            modelUrl = obj.getString("modelUrl"),
                            format = obj.getString("format"),
                            texturesBaseUrl = obj.optString("texturesBaseUrl", ""),
                            isBuiltIn = false,
                            characterNames = chars
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
        isInitialized = true
    }

    fun getAllModels(context: Context): List<Buddy3DModelEntry> {
        ensureInitialized(context)
        return builtInModels + customModelsState
    }

    fun getActiveModel(context: Context): Buddy3DModelEntry {
        ensureInitialized(context)
        val all = getAllModels(context)
        return all.find { it.id == activeModelIdState.value } ?: builtInSsrbsModel
    }

    fun setDisplayMode(context: Context, mode: BuddyDisplayMode) {
        ensureInitialized(context)
        displayModeState.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_MODE, mode.name)
            .apply()
        BuddyWidgetFrameSequenceManager.onEquippedBuddyChanged(context)
    }

    fun toggleDisplayMode(context: Context) {
        ensureInitialized(context)
        val next = if (displayModeState.value == BuddyDisplayMode.CLASSIC_2D) {
            BuddyDisplayMode.THREE_D
        } else {
            BuddyDisplayMode.CLASSIC_2D
        }
        setDisplayMode(context, next)
    }

    fun selectModel(context: Context, modelId: String, characterIndex: Int = 0) {
        ensureInitialized(context)
        activeModelIdState.value = modelId
        activeCharacterIndexState.intValue = characterIndex
        val model = getAllModels(context).find { it.id == modelId } ?: builtInSsrbsModel
        if (model.characterNames.isNotEmpty()) {
            detectedCharactersState.clear()
            detectedCharactersState.addAll(model.characterNames)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_MODEL_ID, modelId)
            .putInt(KEY_ACTIVE_CHAR_INDEX, characterIndex)
            .apply()
        BuddyWidgetFrameSequenceManager.onEquippedBuddyChanged(context)
    }

    fun selectCharacterIndex(context: Context, index: Int) {
        ensureInitialized(context)
        activeCharacterIndexState.intValue = index
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ACTIVE_CHAR_INDEX, index)
            .apply()
        BuddyWidgetFrameSequenceManager.onEquippedBuddyChanged(context)
    }

    fun cycleCharacter(context: Context) {
        ensureInitialized(context)
        val count = detectedCharactersState.size
        if (count <= 1) return
        val current = activeCharacterIndexState.intValue
        val next = if (current + 1 >= count) -1 else current + 1
        selectCharacterIndex(context, next)
    }

    fun updateDetectedCharacters(names: List<String>, activeIndex: Int) {
        if (names.isNotEmpty()) {
            detectedCharactersState.clear()
            detectedCharactersState.addAll(names)
        }
        activeCharacterIndexState.intValue = activeIndex
    }

    fun getActiveCharacterLabel(): String {
        val chars = detectedCharactersState
        val idx = activeCharacterIndexState.intValue
        return when {
            chars.size <= 1 -> chars.firstOrNull()?.uppercase() ?: "3D BUDDY"
            idx == -1 -> "ALL ${chars.size} (TRIO)"
            idx in chars.indices -> chars[idx].uppercase()
            else -> chars.first().uppercase()
        }
    }

    fun getBuddyNickname(context: Context, petId: String? = null): String {
        AccountAndInventoryManager.ensureInitialized(context)
        return AccountAndInventoryManager.getPetNickname(petId ?: AccountAndInventoryManager.equippedPetIdState.value)
    }

    fun setBuddyNickname(context: Context, nickname: String, petId: String? = null) {
        AccountAndInventoryManager.setPetNickname(context, nickname, petId ?: AccountAndInventoryManager.equippedPetIdState.value)
    }

    private fun saveCustomModels(context: Context) {
        val arr = JSONArray()
        for (m in customModelsState) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("title", m.title)
            obj.put("subtitle", m.subtitle)
            obj.put("modelUrl", m.modelUrl)
            obj.put("format", m.format)
            obj.put("texturesBaseUrl", m.texturesBaseUrl)
            val charsArr = JSONArray()
            m.characterNames.forEach { charsArr.put(it) }
            obj.put("characterNames", charsArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_MODELS, arr.toString())
            .apply()
    }

    fun deleteCustomModel(context: Context, modelId: String) {
        ensureInitialized(context)
        customModelsState.removeAll { it.id == modelId }
        val dir = File(context.filesDir, "custom_models/$modelId")
        if (dir.exists()) dir.deleteRecursively()
        saveCustomModels(context)
        if (activeModelIdState.value == modelId) {
            selectModel(context, BUILTIN_SSRB_ID, 0)
        }
    }

    /**
     * Imports a single 3D model (.fbx, .glb, .gltf, .obj) or a .zip archive containing a 3D model + textures.
     */
    fun importModelFile(context: Context, uri: Uri): Result<Buddy3DModelEntry> {
        ensureInitialized(context)
        return try {
            val displayName = resolveDisplayName(context, uri) ?: "model.glb"
            val ext = displayName.substringAfterLast('.', "").lowercase()
            val id = "custom_" + UUID.randomUUID().toString().take(8)
            val destRoot = File(context.filesDir, "custom_models/$id")
            val texDir = File(destRoot, "textures")
            destRoot.mkdirs()
            texDir.mkdirs()
            val thumbDir = File(destRoot, "Thumbnail")
            thumbDir.mkdirs()

            val supported3DExts = setOf("fbx", "glb", "gltf", "obj")
            val supportedTexExts = setOf("png", "jpg", "jpeg", "webp", "bmp")

            var primaryModelFile: File? = null
            var primaryFormat = ""

            if (ext == "zip") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val entryName = File(entry.name).name
                                val entryExt = entryName.substringAfterLast('.', "").lowercase()
                                val isThumb = entry.name.contains("Thumbnail", ignoreCase = true) ||
                                    entryName.startsWith("thumb", ignoreCase = true)

                                if (isThumb && entryExt in supportedTexExts) {
                                    val outThumb = File(thumbDir, entryName)
                                    FileOutputStream(outThumb).use { out -> zis.copyTo(out) }
                                    // Also maintain thumbnail.png as default entry if not already set
                                    val stdThumb = File(thumbDir, "thumbnail.png")
                                    if (!stdThumb.exists()) {
                                        FileOutputStream(stdThumb).use { out -> outThumb.inputStream().use { it.copyTo(out) } }
                                    }
                                } else if (entryExt in supported3DExts) {
                                    val outFile = File(destRoot, entryName)
                                    FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                                    if (primaryModelFile == null) {
                                        primaryModelFile = outFile
                                        primaryFormat = entryExt
                                    }
                                } else if (entryExt in supportedTexExts) {
                                    val outTex = File(texDir, entryName)
                                    FileOutputStream(outTex).use { out -> zis.copyTo(out) }
                                } else if (entryExt == "bin" || entryExt == "mtl") {
                                    val outAux = File(destRoot, entryName)
                                    FileOutputStream(outAux).use { out -> zis.copyTo(out) }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            } else if (ext in supported3DExts) {
                val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val outFile = File(destRoot, safeName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { out -> input.copyTo(out) }
                }
                primaryModelFile = outFile
                primaryFormat = ext
            } else {
                destRoot.deleteRecursively()
                return Result.failure(IllegalArgumentException("Unsupported file type '.$ext'. Supported: .fbx, .glb, .gltf, .obj, .zip"))
            }

            val modelFile = primaryModelFile
            if (modelFile == null || !modelFile.exists()) {
                destRoot.deleteRecursively()
                return Result.failure(IllegalArgumentException("No .fbx, .glb, .gltf, or .obj model found in file."))
            }

            val cleanTitle = displayName.substringBeforeLast('.')
                .replace(Regex("[_-]+"), " ")
                .trim()
                .uppercase()
                .ifBlank { "CUSTOM 3D BUDDY" }

            val entry = Buddy3DModelEntry(
                id = id,
                title = cleanTitle,
                subtitle = "${primaryFormat.uppercase()} • Imported Model",
                modelUrl = "https://buddy3d.local/custom/$id/${modelFile.name}",
                format = primaryFormat,
                texturesBaseUrl = "https://buddy3d.local/custom/$id/textures/",
                isBuiltIn = false
            )

            customModelsState.add(entry)
            saveCustomModels(context)
            selectModel(context, entry.id, 0)
            setDisplayMode(context, BuddyDisplayMode.THREE_D)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    /**
     * Loads the static screenshot icon for the given companion pet from standardized Thumbnail/ folders.
     * Checks:
     * 1. Model-specific Thumbnail/ folders (e.g. assets/buddy3d/models/<folder>/Thumbnail/)
     * 2. Program buddies/Thumbnail/ folder (assets/buddies/Thumbnail/)
     * 3. User-generated content (UGC) storage (filesDir/custom_models/<id>/Thumbnail/)
     */
    fun getBuddyScreenshotBitmap(context: Context, petId: String): Bitmap? {
        val candidateAssetPaths = when (petId) {
            "pet_2d_classic" -> listOf(
                "buddy3d/models/2d-classic/Thumbnail/thumbnail.png",
                "buddy3d/models/2d-classic/Thumbnail/$petId.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            "pet_buddybot_cc0" -> listOf(
                "buddy3d/models/buddybot-cc0/Thumbnail/thumbnail.png",
                "buddy3d/models/buddybot-cc0/Thumbnail/$petId.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            "pet_ssrb_greybie" -> listOf(
                "buddy3d/models/ssrbs-hololive/Thumbnail/pet_ssrb_greybie.png",
                "buddy3d/models/ssrbs-hololive/Thumbnail/thumbnail.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            "pet_ssrb_camoue" -> listOf(
                "buddy3d/models/ssrbs-hololive/Thumbnail/pet_ssrb_camoue.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            "pet_ssrb_boitan" -> listOf(
                "buddy3d/models/ssrbs-hololive/Thumbnail/pet_ssrb_boitan.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            "pet_ssrb_trio" -> listOf(
                "buddy3d/models/ssrbs-hololive/Thumbnail/pet_ssrb_trio.png",
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
            else -> listOf(
                "buddies/Thumbnail/$petId.png",
                "buddies/$petId.png"
            )
        }

        // 1. Check bundled program assets
        for (path in candidateAssetPaths) {
            try {
                context.assets.open(path).use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        // 2. Check user-generated content (UGC) storage for custom models
        val ugcCandidates = listOf(
            File(context.filesDir, "custom_models/$petId/Thumbnail/thumbnail.png"),
            File(context.filesDir, "custom_models/$petId/Thumbnail/$petId.png"),
            File(context.filesDir, "custom_models/$petId/Thumbnail/thumb.png"),
            File(context.filesDir, "buddies/Thumbnail/$petId.png"),
            File(context.filesDir, "buddies/$petId.png")
        )
        for (f in ugcCandidates) {
            try {
                if (f.exists()) {
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        return null
    }
}

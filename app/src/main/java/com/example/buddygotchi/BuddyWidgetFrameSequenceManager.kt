package com.example.buddygotchi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.Base64
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates and caches a temporary 24 FPS idle animation image sequence in [Context.getCacheDir]
 * for the currently equipped Buddy companion so the Android Home Screen AppWidget can loop
 * the character's full idle animation smoothly via a RemoteViews [android.widget.ViewFlipper]
 * without running a battery-draining live 3D engine on the home screen.
 */
object BuddyWidgetFrameSequenceManager {
    const val DEFAULT_FRAME_COUNT = 28
    const val MAX_FRAME_COUNT = 42
    private const val FRAME_WIDTH = 128
    private const val FRAME_HEIGHT = 84
    private const val CACHE_DIR_NAME = "widget_buddy_idle_seq"
    private const val META_FILE_NAME = "sequence_meta.txt"

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Returns a unique key identifying the currently equipped Buddy visual configuration.
     */
    fun getCurrentBuddyVisualKey(context: Context): String {
        Buddy3DManager.ensureInitialized(context)
        AccountAndInventoryManager.ensureInitialized(context)
        val mode = Buddy3DManager.displayModeState.value
        val modelId = Buddy3DManager.activeModelIdState.value
        val charIdx = Buddy3DManager.activeCharacterIndexState.intValue
        val equippedItems = AccountAndInventoryManager.equippedItemIdsState.sorted().joinToString(",")
        return "${mode.name}::$modelId::$charIdx::$equippedItems"
    }

    /**
     * Returns the human-readable name of the currently equipped Buddy for the Home Screen widget label.
     */
    fun getEquippedBuddyShortName(context: Context): String {
        Buddy3DManager.ensureInitialized(context)
        AccountAndInventoryManager.ensureInitialized(context)
        val mode = Buddy3DManager.displayModeState.value
        if (mode == BuddyDisplayMode.CLASSIC_2D) {
            return "2D EYES"
        }
        val modelId = Buddy3DManager.activeModelIdState.value
        val charIdx = Buddy3DManager.activeCharacterIndexState.intValue
        return when (modelId) {
            Buddy3DManager.BUILTIN_BUDDYBOT_ID -> "BUDDYBOT"
            Buddy3DManager.BUILTIN_SSRB_ID -> when (charIdx) {
                0 -> "GREYBIE"
                1 -> "CAMOUE"
                2 -> "BOITAN"
                -1 -> "TRIO"
                else -> "GREYBIE"
            }
            else -> Buddy3DManager.getActiveCharacterLabel().take(10)
        }
    }

    /**
     * Called when the live 3D WebGL viewer (`viewer.html`) finishes capturing a 24 FPS
     * idle animation sequence of the active 3D model. Saves all frames as temporary PNGs
     * in [Context.getCacheDir] and refreshes all Home Screen widgets.
     */
    fun saveCapturedWebGLFrames(context: Context, modelKey: String, framesJson: String) {
        try {
            val arr = JSONArray(framesJson)
            val count = arr.length().coerceAtMost(MAX_FRAME_COUNT)
            if (count < 8) return
            val dir = getCacheDir(context)

            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("frame_") && file.name.endsWith(".png")) {
                    file.delete()
                }
            }

            var savedCount = 0
            for (i in 0 until count) {
                val dataUrl = arr.getString(i)
                val base64Part = dataUrl.substringAfter("base64,", "")
                if (base64Part.isNotBlank()) {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (decoded != null) {
                        val scaled = if (decoded.width == FRAME_WIDTH && decoded.height == FRAME_HEIGHT) {
                            decoded
                        } else {
                            Bitmap.createScaledBitmap(decoded, FRAME_WIDTH, FRAME_HEIGHT, true)
                        }
                        val outFile = File(dir, "frame_$savedCount.png")
                        FileOutputStream(outFile).use { out ->
                            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        savedCount++
                    }
                }
            }

            if (savedCount >= 8) {
                val currentKey = getCurrentBuddyVisualKey(context)
                File(dir, META_FILE_NAME).writeText("webgl::$savedCount::$currentKey")
                BuddyTaskWidgetProvider.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            android.util.Log.w("BuddyWidgetSeq", "Failed to save WebGL frames: ${e.message}")
        }
    }

    /**
     * Called whenever the user equips a different Pet, switches 2D/3D mode, or changes characters.
     * Immediately regenerates the temporary idle frame sequence and updates the Home Screen widget.
     */
    fun onEquippedBuddyChanged(context: Context) {
        try {
            val dir = getCacheDir(context)
            val metaFile = File(dir, META_FILE_NAME)
            if (metaFile.exists()) {
                metaFile.delete()
            }
            getOrGenerateIdleFrames(context)
            BuddyTaskWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    /**
     * Loads the temporary 24 FPS idle animation frames from cache, or generates them into temporary
     * PNG files if the equipped Buddy changed or cache was cleared.
     */
    fun getOrGenerateIdleFrames(context: Context): List<Bitmap> {
        val dir = getCacheDir(context)
        val metaFile = File(dir, META_FILE_NAME)
        val expectedKey = getCurrentBuddyVisualKey(context)

        val savedMeta = if (metaFile.exists()) metaFile.readText().trim() else ""
        val parts = savedMeta.split("::", limit = 3)
        if (parts.size == 3 && (parts[0] == "webgl" || parts[0] == "studio") && parts[2] == expectedKey) {
            val cachedCount = parts[1].toIntOrNull()?.coerceIn(8, MAX_FRAME_COUNT) ?: 0
            if (cachedCount >= 8 && (0 until cachedCount).all { File(dir, "frame_$it.png").exists() }) {
                val loaded = mutableListOf<Bitmap>()
                for (i in 0 until cachedCount) {
                    val bmp = BitmapFactory.decodeFile(File(dir, "frame_$i.png").absolutePath)
                    if (bmp != null) loaded.add(bmp)
                }
                if (loaded.size == cachedCount) {
                    return loaded
                }
            }
        }

        // Generate the 24 FPS fallback idle animation sequence for the currently equipped Buddy and save to temp cache
        val generated = generateStudioIdleFrames(context, DEFAULT_FRAME_COUNT)
        for (i in generated.indices) {
            val outFile = File(dir, "frame_$i.png")
            FileOutputStream(outFile).use { out ->
                generated[i].compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        metaFile.writeText("studio::${generated.size}::$expectedKey")
        return generated
    }

    private fun generateStudioIdleFrames(context: Context, frameCount: Int): List<Bitmap> {
        Buddy3DManager.ensureInitialized(context)
        AccountAndInventoryManager.ensureInitialized(context)

        val modelId = Buddy3DManager.activeModelIdState.value
        val charIdx = Buddy3DManager.activeCharacterIndexState.intValue
        val equippedItems = AccountAndInventoryManager.equippedItemIdsState.toSet()

        val frames = mutableListOf<Bitmap>()
        for (frameIdx in 0 until frameCount) {
            val phase = (frameIdx.toDouble() / frameCount.toDouble()) * 2.0 * PI
            val bmp = Bitmap.createBitmap(FRAME_WIDTH, FRAME_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            when (modelId) {
                Buddy3DManager.BUILTIN_BUDDYBOT_ID -> {
                    drawBuddyBotIdleFrame(canvas, frameIdx, phase, equippedItems)
                }
                else -> {
                    when (charIdx) {
                        1 -> drawSsrbsCharacterIdleFrame(
                            canvas = canvas,
                            frameIdx = frameIdx,
                            phase = phase,
                            bodyTopColor = Color.rgb(118, 128, 104),
                            bodyBottomColor = Color.rgb(74, 82, 64),
                            accentColor = Color.rgb(168, 184, 140),
                            variant = 1,
                            equippedItems = equippedItems
                        )
                        2 -> drawSsrbsCharacterIdleFrame(
                            canvas = canvas,
                            frameIdx = frameIdx,
                            phase = phase,
                            bodyTopColor = Color.rgb(218, 206, 184),
                            bodyBottomColor = Color.rgb(162, 148, 126),
                            accentColor = Color.rgb(215, 25, 33),
                            variant = 2,
                            equippedItems = equippedItems
                        )
                        -1 -> drawSsrbsTrioIdleFrame(canvas, frameIdx, phase, equippedItems)
                        else -> drawSsrbsCharacterIdleFrame(
                            canvas = canvas,
                            frameIdx = frameIdx,
                            phase = phase,
                            bodyTopColor = Color.rgb(210, 212, 218),
                            bodyBottomColor = Color.rgb(138, 142, 152),
                            accentColor = Color.rgb(215, 25, 33),
                            variant = 0,
                            equippedItems = equippedItems
                        )
                    }
                }
            }
            frames.add(bmp)
        }
        return frames
    }

    /**
     * Renders 1 frame of the 3D SSRB Companion (Greybie / Camoue / Boitan) idle breathing + bounce + blink loop.
     */
    private fun drawSsrbsCharacterIdleFrame(
        canvas: Canvas,
        frameIdx: Int,
        phase: Double,
        bodyTopColor: Int,
        bodyBottomColor: Int,
        accentColor: Int,
        variant: Int,
        equippedItems: Set<String>
    ) {
        val w = FRAME_WIDTH.toFloat()
        val h = FRAME_HEIGHT.toFloat()
        val cx = w * 0.5f
        val bobY = (sin(phase) * 5.0).toFloat()
        val squashX = 1.0f + (cos(phase) * 0.035).toFloat()
        val squashY = 1.0f - (cos(phase) * 0.035).toFloat()
        val swayDeg = (sin(phase) * 3.2).toFloat()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Ground shadow (scales inversely with jump height)
        val shadowScale = 1.0f - (sin(phase) * 0.08).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(90, 0, 0, 0)
        canvas.drawOval(
            RectF(
                cx - 34f * shadowScale,
                h - 18f,
                cx + 34f * shadowScale,
                h - 6f
            ),
            paint
        )

        canvas.save()
        canvas.translate(cx, h * 0.56f + bobY)
        canvas.rotate(swayDeg)
        canvas.scale(squashX, squashY)

        // Optional Back Wings Gear
        if ("item_plasma_wings" in equippedItems || "item_thruster_pack" in equippedItems) {
            paint.color = Color.argb(200, 215, 25, 33)
            val wingFlap = (sin(phase * 2) * 4f).toFloat()
            canvas.drawRoundRect(RectF(-44f, -12f + wingFlap, -26f, 10f + wingFlap), 6f, 6f, paint)
            canvas.drawRoundRect(RectF(26f, -12f + wingFlap, 44f, 10f + wingFlap), 6f, 6f, paint)
        }

        // 2. Top Fuse / Antenna Stem & Glowing Tip
        val stemPath = Path().apply {
            moveTo(0f, -28f)
            quadTo(6f + (sin(phase) * 3f).toFloat(), -40f, 2f + (cos(phase) * 4f).toFloat(), -47f)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4.5f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(40, 40, 44)
        canvas.drawPath(stemPath, paint)

        val tipX = 2f + (cos(phase) * 4f).toFloat()
        val tipY = -47f
        paint.style = Paint.Style.FILL
        paint.color = accentColor
        canvas.drawCircle(tipX, tipY, 5.5f, paint)
        paint.color = Color.argb(110, 255, 90, 95)
        canvas.drawCircle(tipX, tipY, 8.5f, paint)

        // 3. Round 3D Toon Body with Cel-Shaded Gradient & Dark Anime Outline
        val bodyRect = RectF(-32f, -30f, 32f, 28f)
        paint.shader = LinearGradient(
            0f, -30f, 0f, 28f,
            bodyTopColor, bodyBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(bodyRect, paint)
        paint.shader = null

        // Anime outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.2f
        paint.color = Color.rgb(20, 20, 22)
        canvas.drawOval(bodyRect, paint)

        // Specular 3D highlight on upper-left brow
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(75, 255, 255, 255)
        canvas.drawOval(RectF(-20f, -23f, -4f, -12f), paint)

        // Camo / Variant markings
        if (variant == 1) {
            paint.color = Color.argb(95, 52, 60, 42)
            canvas.drawOval(RectF(-24f, -6f, -10f, 8f), paint)
            canvas.drawOval(RectF(10f, -14f, 23f, -2f), paint)
        } else if (variant == 2) {
            paint.color = Color.argb(85, 215, 25, 33)
            canvas.drawOval(RectF(-18f, 8f, -8f, 15f), paint)
            canvas.drawOval(RectF(8f, 8f, 18f, 15f), paint)
        }

        // 4. Expressive Eyes (Blink on frame 4)
        val isBlinking = (frameIdx == 4)
        paint.color = Color.rgb(22, 22, 24)
        if (isBlinking) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3.0f
            canvas.drawLine(-16f, -2f, -6f, -2f, paint)
            canvas.drawLine(6f, -2f, 16f, -2f, paint)
        } else {
            paint.style = Paint.Style.FILL
            val lookX = (sin(phase * 0.5) * 1.8).toFloat()
            canvas.drawOval(RectF(-15f + lookX, -9f, -7f + lookX, 3f), paint)
            canvas.drawOval(RectF(7f + lookX, -9f, 15f + lookX, 3f), paint)

            // Eye shine dots
            paint.color = Color.WHITE
            canvas.drawCircle(-12.5f + lookX, -5.5f, 2.0f, paint)
            canvas.drawCircle(9.5f + lookX, -5.5f, 2.0f, paint)
        }

        // Optional Visor Gear
        if ("item_matrix_visor" in equippedItems) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(190, 215, 25, 33)
            canvas.drawRoundRect(RectF(-20f, -10f, 20f, 2f), 4f, 4f, paint)
        }

        // Optional Neck Collar Gear
        if ("item_dot_collar" in equippedItems) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3.5f
            paint.color = Color.rgb(215, 25, 33)
            canvas.drawArc(RectF(-26f, 12f, 26f, 27f), 15f, 150f, false, paint)
        }

        // Optional Head Halo / Crown Gear
        if ("item_glyph_halo" in equippedItems || "item_holo_crown" in equippedItems) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = if ("item_holo_crown" in equippedItems) Color.rgb(215, 25, 33) else Color.WHITE
            canvas.drawOval(RectF(-18f, -42f, 18f, -34f), paint)
        }

        canvas.restore()
    }

    /**
     * Renders 1 frame of the 3D Companion Trio (Greybie + Camoue + Boitan together) idle animation.
     */
    private fun drawSsrbsTrioIdleFrame(
        canvas: Canvas,
        frameIdx: Int,
        phase: Double,
        equippedItems: Set<String>
    ) {
        canvas.save()
        canvas.translate(-34f, 4f)
        canvas.scale(0.62f, 0.62f, FRAME_WIDTH * 0.5f, FRAME_HEIGHT * 0.5f)
        drawSsrbsCharacterIdleFrame(
            canvas, frameIdx, phase + 0.7,
            Color.rgb(118, 128, 104), Color.rgb(74, 82, 64), Color.rgb(168, 184, 140),
            1, emptySet()
        )
        canvas.restore()

        canvas.save()
        canvas.translate(34f, 4f)
        canvas.scale(0.62f, 0.62f, FRAME_WIDTH * 0.5f, FRAME_HEIGHT * 0.5f)
        drawSsrbsCharacterIdleFrame(
            canvas, frameIdx, phase + 1.4,
            Color.rgb(218, 206, 184), Color.rgb(162, 148, 126), Color.rgb(215, 25, 33),
            2, emptySet()
        )
        canvas.restore()

        canvas.save()
        canvas.scale(0.74f, 0.74f, FRAME_WIDTH * 0.5f, FRAME_HEIGHT * 0.55f)
        drawSsrbsCharacterIdleFrame(
            canvas, frameIdx, phase,
            Color.rgb(210, 212, 218), Color.rgb(138, 142, 152), Color.rgb(215, 25, 33),
            0, equippedItems
        )
        canvas.restore()
    }

    /**
     * Renders 1 frame of the 3D BUDDYBOT MK-I (CC0 Robot Companion) idle hover + antenna pulse animation.
     */
    private fun drawBuddyBotIdleFrame(
        canvas: Canvas,
        frameIdx: Int,
        phase: Double,
        equippedItems: Set<String>
    ) {
        val w = FRAME_WIDTH.toFloat()
        val h = FRAME_HEIGHT.toFloat()
        val cx = w * 0.5f
        val hoverY = (sin(phase) * 5.5).toFloat()
        val tiltDeg = (cos(phase) * 2.5).toFloat()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Hover shadow
        val shadowScale = 1.0f - (sin(phase) * 0.1).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(85, 0, 0, 0)
        canvas.drawOval(
            RectF(cx - 30f * shadowScale, h - 16f, cx + 30f * shadowScale, h - 6f),
            paint
        )

        canvas.save()
        canvas.translate(cx, h * 0.52f + hoverY)
        canvas.rotate(tiltDeg)

        // Antenna
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.color = Color.rgb(180, 184, 192)
        canvas.drawLine(0f, -26f, 0f, -42f, paint)

        // Pulsing Nothing Red Antenna Beacon
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(215, 25, 33)
        canvas.drawCircle(0f, -44f, 5.0f, paint)
        if (frameIdx % 2 == 0) {
            paint.color = Color.argb(110, 215, 25, 33)
            canvas.drawCircle(0f, -44f, 8.5f, paint)
        }

        // Bot Head / Chassis Cube
        val bodyRect = RectF(-30f, -26f, 30f, 22f)
        paint.shader = LinearGradient(
            0f, -26f, 0f, 22f,
            Color.rgb(232, 234, 240),
            Color.rgb(154, 158, 168),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(bodyRect, 12f, 12f, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.8f
        paint.color = Color.rgb(30, 32, 36)
        canvas.drawRoundRect(bodyRect, 12f, 12f, paint)

        // Dark Visor Screen
        val visorRect = RectF(-22f, -16f, 22f, 8f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(18, 20, 24)
        canvas.drawRoundRect(visorRect, 6f, 6f, paint)

        // LED Matrix Eyes inside Visor (Blink on frame 4)
        val isBlinking = (frameIdx == 4)
        paint.color = if ("item_red_core" in equippedItems) Color.rgb(215, 25, 33) else Color.WHITE
        if (isBlinking) {
            paint.strokeWidth = 2.6f
            canvas.drawLine(-14f, -4f, -5f, -4f, paint)
            canvas.drawLine(5f, -4f, 14f, -4f, paint)
        } else {
            val eyeShift = (sin(phase) * 1.5).toFloat()
            canvas.drawRoundRect(RectF(-14f + eyeShift, -9f, -6f + eyeShift, -1f), 2f, 2f, paint)
            canvas.drawRoundRect(RectF(6f + eyeShift, -9f, 14f + eyeShift, -1f), 2f, 2f, paint)
        }

        // Red Core Chest Badge
        paint.color = Color.rgb(215, 25, 33)
        canvas.drawCircle(0f, 15f, 3.5f, paint)

        canvas.restore()
    }
}

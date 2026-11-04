package com.example.buddygotchi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.buddygotchi.ui.theme.NothingRed
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import kotlin.math.hypot

/**
 * Feature toggle for the local 3D / ZIP file import button in Buddy3DStudioDialog.
 * Kept false so users cannot import characters locally from the UI, while preserving
 * the full import implementation in code for developer/future use.
 */
private const val SHOW_LOCAL_IMPORT_UI = false

private class Buddy3DJsBridge(
    private val context: Context,
    private val onCharactersUpdated: (List<String>, Int) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onBuddyPetted() {
        mainHandler.post {
            TaskManager.petBuddy(context)
        }
    }

    @JavascriptInterface
    fun onIdleSequenceCaptured(modelKey: String, framesJson: String) {
        mainHandler.post {
            BuddyWidgetFrameSequenceManager.saveCapturedWebGLFrames(context, modelKey, framesJson)
        }
    }

    @JavascriptInterface
    fun onCharactersDetected(namesJson: String, activeIndex: Int) {
        mainHandler.post {
            try {
                val arr = JSONArray(namesJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                Buddy3DManager.updateDetectedCharacters(list, activeIndex)
                onCharactersUpdated(list, activeIndex)
            } catch (_: Exception) {
            }
        }
    }
}

private fun guessMimeType(path: String): String {
    val ext = path.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "html" -> "text/html"
        "js" -> "application/javascript"
        "css" -> "text/css"
        "json", "gltf" -> "application/json"
        "glb" -> "model/gltf-binary"
        "fbx", "bin" -> "application/octet-stream"
        "obj", "mtl" -> "text/plain"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}

private fun interceptBuddy3DRequest(context: Context, request: WebResourceRequest): WebResourceResponse? {
    val uri = request.url ?: return null
    if (uri.host != "buddy3d.local") return null

    val rawPath = URLDecoder.decode(uri.path ?: "", "UTF-8").trimStart('/')
    val headers = mapOf(
        "Access-Control-Allow-Origin" to "*",
        "Access-Control-Allow-Methods" to "GET, OPTIONS",
        "Cache-Control" to "public, max-age=604800"
    )

    try {
        if (rawPath.startsWith("assets/")) {
            val assetPath = rawPath.removePrefix("assets/")
            val stream = context.assets.open(assetPath)
            return WebResourceResponse(
                guessMimeType(assetPath),
                "UTF-8",
                200,
                "OK",
                headers,
                stream
            )
        } else if (rawPath.startsWith("custom/")) {
            val relPath = rawPath.removePrefix("custom/")
            val file = File(context.filesDir, "custom_models/$relPath")
            if (file.exists() && file.isFile) {
                return WebResourceResponse(
                    guessMimeType(file.name),
                    "UTF-8",
                    200,
                    "OK",
                    headers,
                    FileInputStream(file)
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("Buddy3DView", "Failed to serve $rawPath: ${e.message}")
    }

    return WebResourceResponse(
        "text/plain",
        "UTF-8",
        404,
        "Not Found",
        headers,
        ByteArrayInputStream("Not found: $rawPath".toByteArray())
    )
}

@Composable
fun getBuddy3DViewportSize(size: WidgetSize): Pair<Dp, Dp> {
    return when {
        size.cols == 1 && size.rows == 1 -> 58.dp to 42.dp
        size.cols == 1 && size.rows == 2 -> 72.dp to 86.dp
        size.cols == 1 -> 74.dp to 118.dp
        size.cols == 2 && size.rows == 1 -> 98.dp to 48.dp
        size.cols == 2 && size.rows == 2 -> 130.dp to 94.dp
        size.rows == 1 -> 92.dp to 56.dp
        size.rows == 2 -> 125.dp to 112.dp
        else -> 220.dp to 118.dp
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun Buddy3DView(
    size: WidgetSize,
    getTouchPosition: () -> Offset? = { null },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tapSlopPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(Unit) {
        Buddy3DManager.ensureInitialized(context)
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    var loadedModelUrl by remember { mutableStateOf<String?>(null) }

    val isRenderingActive by Buddy3DManager.isRenderingActiveState
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, webViewRef, isPageReady) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (isRenderingActive) {
                        webViewRef?.evaluateJavascript("if(window.setRenderingActive) window.setRenderingActive(true);", null)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    webViewRef?.evaluateJavascript("if(window.setRenderingActive) window.setRenderingActive(false);", null)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Buddy3DManager.isInteractingWith3DState.value = false
            webViewRef?.evaluateJavascript("if(window.setRenderingActive) window.setRenderingActive(false);", null)
        }
    }

    val activeModelId by Buddy3DManager.activeModelIdState
    val activeCharIndex by Buddy3DManager.activeCharacterIndexState
    val isInteractingWith3D by Buddy3DManager.isInteractingWith3DState
    val activeModel = remember(activeModelId, Buddy3DManager.customModelsState.size) {
        Buddy3DManager.getActiveModel(context)
    }

    val (viewportW, viewportH) = getBuddy3DViewportSize(size)

    LaunchedEffect(isPageReady, isRenderingActive) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPageReady) return@LaunchedEffect
        wv.evaluateJavascript(
            "if(window.setRenderingActive) window.setRenderingActive($isRenderingActive);",
            null
        )
    }

    var centerInRoot by remember { mutableStateOf<Offset?>(null) }
    val currentTouchGetter by rememberUpdatedState(getTouchPosition)

    // Forward dashboard touch tracking to 3D Buddy gaze via snapshotFlow without recomposing Buddy3DView
    LaunchedEffect(isPageReady, isInteractingWith3D, isRenderingActive) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPageReady || isInteractingWith3D || !isRenderingActive) return@LaunchedEffect
        var lastEvalTime = 0L
        snapshotFlow { currentTouchGetter() }.collect { activeTouch ->
            val center = centerInRoot
            if (activeTouch != null && center != null) {
                val now = SystemClock.uptimeMillis()
                if (now - lastEvalTime >= 32L) {
                    lastEvalTime = now
                    val normX = ((activeTouch.x - center.x) / 350f).coerceIn(-1f, 1f)
                    val normY = ((activeTouch.y - center.y) / 350f).coerceIn(-1f, 1f)
                    wv.evaluateJavascript("if(window.setLookTarget) window.setLookTarget($normX, $normY);", null)
                }
            } else {
                lastEvalTime = 0L
                wv.evaluateJavascript("if(window.setLookTarget) window.setLookTarget(0, 0);", null)
            }
        }
    }

    // Trigger model load or character index change when state updates
    LaunchedEffect(isPageReady, activeModel.modelUrl, activeCharIndex) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPageReady) return@LaunchedEffect
        if (loadedModelUrl != activeModel.modelUrl) {
            loadedModelUrl = activeModel.modelUrl
            val js = "if(window.loadBuddyModel) window.loadBuddyModel('${activeModel.modelUrl}', '${activeModel.format}', '${activeModel.texturesBaseUrl}', $activeCharIndex);"
            wv.evaluateJavascript(js, null)
        } else {
            val js = "if(window.selectCharacterIndex) window.selectCharacterIndex($activeCharIndex);"
            wv.evaluateJavascript(js, null)
        }
    }

    Box(
        modifier = modifier
            .size(viewportW, viewportH)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = "Interactive 3D Buddy Companion Viewport. Drag to rotate 360 degrees, tap to pet." }
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                centerInRoot = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
            }
            // Capture and consume all pointer events in Compose so parent HorizontalPager,
            // verticalScroll, and long-press edit mode never steal 3D orbit drags.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    Buddy3DManager.isInteractingWith3DState.value = true

                    val downTime = SystemClock.uptimeMillis()
                    var lastPos = down.position
                    var totalDist = 0f

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (!change.pressed) {
                                change.consume()
                                break
                            }

                            val delta = change.position - lastPos
                            lastPos = change.position
                            totalDist += hypot(delta.x, delta.y)
                            change.consume()

                            if (delta.x != 0f || delta.y != 0f) {
                                // Convert pixel delta to density-independent orbit units for consistent feel across screens
                                val dxDp = delta.x / density.density
                                val dyDp = delta.y / density.density
                                webViewRef?.evaluateJavascript(
                                    "if(window.onNativeDrag) window.onNativeDrag($dxDp, $dyDp);",
                                    null
                                )
                            }
                        }
                    } finally {
                        Buddy3DManager.isInteractingWith3DState.value = false
                        webViewRef?.evaluateJavascript(
                            "if(window.onNativeRelease) window.onNativeRelease();",
                            null
                        )
                        val elapsed = SystemClock.uptimeMillis() - downTime
                        if (totalDist < tapSlopPx && elapsed < 350L) {
                            webViewRef?.evaluateJavascript("if(window.petBuddy) window.petBuddy();", null)
                            TaskManager.petBuddy(context)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewRef = this
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                    }

                    // Touch events are handled by the enclosing Compose pointerInput modifier
                    setOnTouchListener { _, _ -> true }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            android.util.Log.d("Buddy3DWeb", "${cm?.message()} [L${cm?.lineNumber()}]")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (request != null) {
                                val intercepted = interceptBuddy3DRequest(ctx, request)
                                if (intercepted != null) return intercepted
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageReady = true
                        }
                    }

                    addJavascriptInterface(
                        Buddy3DJsBridge(ctx) { _, _ -> },
                        "Buddy3DBridge"
                    )

                    loadUrl("https://buddy3d.local/assets/buddy3d/viewer.html")
                }
            }
        )
    }
}

@Composable
fun Buddy3DStudioDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Buddy3DManager.ensureInitialized(context)
        AccountAndInventoryManager.ensureInitialized(context)
    }

    val displayMode by Buddy3DManager.displayModeState
    val activeModelId by Buddy3DManager.activeModelIdState
    val activeCharIndex by Buddy3DManager.activeCharacterIndexState
    val equippedPetId by AccountAndInventoryManager.equippedPetIdState

    // Filter pets based on active mode (2D Buddy shows 2D pets only; 3D Buddy shows 3D pets only)
    val filteredPets = remember(displayMode) {
        AccountAndInventoryManager.petCatalog.filter { it.displayMode == displayMode }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BUDDY STUDIO",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "SELECT 2D / 3D COMPANION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF8E8E8E)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", fontSize = 12.sp, color = Color.White)
                    }
                }

                // 1. 2D vs 3D Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1C))
                        .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val is2D = displayMode == BuddyDisplayMode.CLASSIC_2D
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (is2D) NothingRed else Color.Transparent)
                            .clickable {
                                Buddy3DManager.setDisplayMode(context, BuddyDisplayMode.CLASSIC_2D)
                                AccountAndInventoryManager.equipPet(context, "pet_2d_classic")
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = " 2D Buddy ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (is2D) Color.White else Color(0xFF9E9E9E)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!is2D) NothingRed else Color.Transparent)
                            .clickable {
                                Buddy3DManager.setDisplayMode(context, BuddyDisplayMode.THREE_D)
                                if (equippedPetId == "pet_2d_classic") {
                                    AccountAndInventoryManager.equipPet(context, "pet_ssrb_greybie")
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3D Buddy",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!is2D) Color.White else Color(0xFF9E9E9E)
                        )
                    }
                }

                // 2. Live 3D Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181818))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayMode == BuddyDisplayMode.CLASSIC_2D) {
                        BuddyEyes(size = WidgetSize.MAX)
                    } else {
                        Buddy3DView(
                            size = WidgetSize.MAX,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = if (displayMode == BuddyDisplayMode.THREE_D) "DRAG TO ROTATE • TAP TO PET" else "CLASSIC 2D MODE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        color = Color(0xFF777777),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }

                // 3. Buddy Selection Grid (3x3 visible, scrollable up and down)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "BUDDY CATALOG",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPets) { pet ->
                            val isSelected = when (pet.displayMode) {
                                BuddyDisplayMode.CLASSIC_2D -> displayMode == BuddyDisplayMode.CLASSIC_2D
                                BuddyDisplayMode.THREE_D -> {
                                    displayMode == BuddyDisplayMode.THREE_D &&
                                        activeModelId == pet.modelId &&
                                        activeCharIndex == pet.characterIndex
                                }
                            }

                            val screenshot = remember(pet.id) {
                                Buddy3DManager.getBuddyScreenshotBitmap(context, pet.id)
                            }

                            val buddyDisplayName = remember(pet.id) {
                                when (pet.id) {
                                    "pet_2d_classic" -> "2D Classic"
                                    "pet_buddybot_cc0" -> "BuddyBot"
                                    "pet_ssrb_greybie" -> "Greybie"
                                    "pet_ssrb_camoue" -> "Camoue"
                                    "pet_ssrb_boitan" -> "Boitan"
                                    "pet_ssrb_trio" -> "Trio"
                                    else -> pet.name.replace(" (3D)", "").trim()
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NothingRed.copy(alpha = 0.16f) else Color(0xFF191919))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) NothingRed else Color(0xFF2C2C2C),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        AccountAndInventoryManager.equipPet(context, pet.id)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (screenshot != null) {
                                    Image(
                                        bitmap = screenshot.asImageBitmap(),
                                        contentDescription = buddyDisplayName,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF222222)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = buddyDisplayName.take(2).uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = buddyDisplayName,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
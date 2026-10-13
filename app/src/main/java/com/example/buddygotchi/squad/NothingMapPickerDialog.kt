package com.example.buddygotchi.squad

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class GeocodedPlace(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

object NominatimGeocoder {
    suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "BuddyGotchiApp/1.0 (Android; NothingOS)")
            }
            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val addr = json.optJSONObject("address")
                if (addr != null) {
                    val placeName = addr.optString("amenity").ifEmpty {
                        addr.optString("building").ifEmpty {
                            addr.optString("shop").ifEmpty {
                                addr.optString("leisure").ifEmpty {
                                    addr.optString("road")
                                }
                            }
                        }
                    }
                    val city = addr.optString("city").ifEmpty {
                        addr.optString("town").ifEmpty {
                            addr.optString("village").ifEmpty {
                                addr.optString("suburb")
                            }
                        }
                    }
                    if (placeName.isNotBlank() && city.isNotBlank()) {
                        "$placeName, $city".uppercase()
                    } else if (placeName.isNotBlank()) {
                        placeName.uppercase()
                    } else {
                        val displayName = json.optString("display_name", "")
                        displayName.split(",").take(2).joinToString(", ").trim().uppercase()
                    }
                } else {
                    json.optString("display_name", "").split(",").take(2).joinToString(", ").trim().uppercase()
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchPlaces(query: String): List<GeocodedPlace> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&addressdetails=1&limit=5")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "BuddyGotchiApp/1.0 (Android; NothingOS)")
            }
            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                val results = mutableListOf<GeocodedPlace>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val displayName = obj.getString("display_name")
                    val addr = obj.optJSONObject("address")
                    val name = addr?.optString("amenity")?.ifEmpty { null }
                        ?: addr?.optString("shop")?.ifEmpty { null }
                        ?: addr?.optString("road")?.ifEmpty { null }
                        ?: displayName.split(",").firstOrNull()?.trim()
                        ?: "LOCATION"
                    val sub = displayName.split(",").drop(1).take(2).joinToString(", ").trim()
                    results.add(
                        GeocodedPlace(
                            title = name.uppercase(),
                            subtitle = if (sub.isNotBlank()) sub.uppercase() else "VENUE",
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
                results
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private class MapJsBridge(
    private val onIdle: (Double, Double, Int) -> Unit
) {
    @JavascriptInterface
    fun onMapIdle(lat: Double, lng: Double, zoom: Int) {
        onIdle(lat, lng, zoom)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NothingMapPickerDialog(
    initialLat: Double? = null,
    initialLng: Double? = null,
    initialLocationName: String = "",
    onDismiss: () -> Unit,
    onLocationSelected: (locationName: String, latitude: Double, longitude: Double) -> Unit
) {
    // Default to Sofia (42.6977, 23.3219) if null
    var selectedLat by remember { mutableDoubleStateOf(initialLat ?: 42.6977) }
    var selectedLng by remember { mutableDoubleStateOf(initialLng ?: 23.3219) }
    var locationNameInput by remember {
        mutableStateOf(if (initialLocationName.isNotBlank()) initialLocationName else "CENTRAL LOCATION")
    }

    var isReverseGeocoding by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodedPlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val initialZoom = 15

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Column {
                            Text(
                                text = "LOCATION PICKER // MAP",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = NothingWhite
                            )
                            Text(
                                text = "CARTO DARK TILES • OPENSTREETMAP",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = NothingTextTertiary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            color = NothingTextSecondary
                        )
                    }
                }

                // 2. Search Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()
                            if (GoogleMapsLinkResolver.isResolvable(query)) {
                                isSearching = true
                                searchJob = coroutineScope.launch {
                                    val resolved = GoogleMapsLinkResolver.resolve(query)
                                    if (resolved != null) {
                                        selectedLat = resolved.latitude
                                        selectedLng = resolved.longitude
                                        locationNameInput = resolved.title
                                        webViewRef?.evaluateJavascript(
                                            "setCenter(${resolved.latitude}, ${resolved.longitude}, 17);",
                                            null
                                        )
                                        searchResults = listOf(
                                            GeocodedPlace(
                                                title = resolved.title,
                                                subtitle = "📍 GOOGLE MAPS PIN • ${resolved.subtitle}",
                                                latitude = resolved.latitude,
                                                longitude = resolved.longitude
                                            )
                                        )
                                    }
                                    isSearching = false
                                }
                            } else if (query.trim().length >= 2) {
                                isSearching = true
                                searchJob = coroutineScope.launch {
                                    delay(400) // Debounce
                                    val results = NominatimGeocoder.searchPlaces(query.trim())
                                    searchResults = results
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                                isSearching = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161616))
                            .border(1.dp, if (searchQuery.isNotBlank()) NothingRed.copy(alpha = 0.6f) else Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = 0.8.sp
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "🔍 SEARCH OR PASTE MAPS LINK...",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            color = NothingTextTertiary.copy(alpha = 0.5f),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF282828))
                                            .clickable {
                                                searchQuery = ""
                                                searchResults = emptyList()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕",
                                            fontSize = 9.sp,
                                            color = NothingTextSecondary
                                        )
                                    }
                                } else {
                                    // Quick paste button from clipboard
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF242424))
                                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                            .clickable {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    searchQuery = clip
                                                    if (GoogleMapsLinkResolver.isResolvable(clip)) {
                                                        isSearching = true
                                                        searchJob = coroutineScope.launch {
                                                            val resolved = GoogleMapsLinkResolver.resolve(clip)
                                                            if (resolved != null) {
                                                                selectedLat = resolved.latitude
                                                                selectedLng = resolved.longitude
                                                                locationNameInput = resolved.title
                                                                webViewRef?.evaluateJavascript(
                                                                    "setCenter(${resolved.latitude}, ${resolved.longitude}, 17);",
                                                                    null
                                                                )
                                                                searchResults = listOf(
                                                                    GeocodedPlace(
                                                                        title = resolved.title,
                                                                        subtitle = "📍 GOOGLE MAPS PIN • ${resolved.subtitle}",
                                                                        latitude = resolved.latitude,
                                                                        longitude = resolved.longitude
                                                                    )
                                                                )
                                                            }
                                                            isSearching = false
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "📋 PASTE",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingWhite
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (isSearching) {
                        Text(
                            text = "● RESOLVING LOCATION...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    // Search Results Dropdown
                    if (searchResults.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF333333)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(searchResults) { place ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLat = place.latitude
                                                selectedLng = place.longitude
                                                locationNameInput = place.title
                                                searchResults = emptyList()
                                                searchQuery = ""
                                                focusManager.clearFocus()
                                                webViewRef?.evaluateJavascript(
                                                    "setCenter(${place.latitude}, ${place.longitude}, 16);",
                                                    null
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(NothingRed)
                                            )
                                            Text(
                                                text = place.title,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingWhite
                                            )
                                        }
                                        Text(
                                            text = place.subtitle,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.5.sp,
                                            color = NothingTextTertiary,
                                            maxLines = 1
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF242424), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                // 3. Interactive Map Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(16.dp))
                        .background(Color(0xFF101010))
                ) {
                    // Leaflet WebView
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                android.util.Log.e("LeafletMap", "WebView factory executed!")
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile; BuddyGotchi/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }
                                setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onConsoleMessage(cm: android.webkit.ConsoleMessage?): Boolean {
                                        android.util.Log.e("LeafletMap", "Console: ${cm?.message()} [line ${cm?.lineNumber()}]")
                                        return true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        android.util.Log.e("LeafletMap", "onPageFinished: $url")
                                        view?.postDelayed({
                                            view.evaluateJavascript("if (window.map) { map.invalidateSize(); initMap($selectedLat, $selectedLng, $initialZoom); }", null)
                                        }, 150)
                                    }
                                    override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                        super.onReceivedError(view, request, error)
                                        android.util.Log.e("LeafletMap", "onReceivedError: ${error?.description} for ${request?.url}")
                                    }
                                }
                                addJavascriptInterface(
                                    MapJsBridge { lat, lng, _ ->
                                        selectedLat = lat
                                        selectedLng = lng
                                        // Reverse geocode when map stops moving
                                        reverseGeocodeJob?.cancel()
                                        reverseGeocodeJob = coroutineScope.launch {
                                            isReverseGeocoding = true
                                            val resolved = NominatimGeocoder.reverseGeocode(lat, lng)
                                            if (!resolved.isNullOrBlank()) {
                                                locationNameInput = resolved
                                            }
                                            isReverseGeocoding = false
                                        }
                                    },
                                    "AndroidBridge"
                                )

                                loadUrl("file:///android_asset/leaflet/map.html")
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                        }
                    )

                    // Center Crosshair Reticle (Nothing OS Style)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Badge above pin
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xE6141414))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PIN LOCATION",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingRed,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            // Reticle Canvas
                            Canvas(modifier = Modifier.size(36.dp)) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.width / 2 - 4.dp.toPx()

                                // Outer ring
                                drawCircle(
                                    color = NothingRed.copy(alpha = 0.8f),
                                    radius = radius,
                                    center = center,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                )

                                // Center red dot
                                drawCircle(
                                    color = NothingRed,
                                    radius = 3.5.dp.toPx(),
                                    center = center
                                )

                                // 4 Tick lines (Nothing crosshair)
                                val tickLength = 5.dp.toPx()
                                // Top
                                drawLine(NothingRed, Offset(center.x, center.y - radius - tickLength), Offset(center.x, center.y - radius), 1.5.dp.toPx())
                                // Bottom
                                drawLine(NothingRed, Offset(center.x, center.y + radius), Offset(center.x, center.y + radius + tickLength), 1.5.dp.toPx())
                                // Left
                                drawLine(NothingRed, Offset(center.x - radius - tickLength, center.y), Offset(center.x - radius, center.y), 1.5.dp.toPx())
                                // Right
                                drawLine(NothingRed, Offset(center.x + radius, center.y), Offset(center.x + radius + tickLength, center.y), 1.5.dp.toPx())
                            }
                        }
                    }

                    // Floating Map Controls (Zoom + Recenter)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Zoom in
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC1A1A1A))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable {
                                    webViewRef?.evaluateJavascript("zoomIn();", null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        // Zoom out
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC1A1A1A))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable {
                                    webViewRef?.evaluateJavascript("zoomOut();", null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        // Recenter to Sofia
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC1A1A1A))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedLat = 42.6977
                                    selectedLng = 23.3219
                                    webViewRef?.evaluateJavascript("setCenter(42.6977, 23.3219, 15);", null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⌖",
                                fontSize = 14.sp,
                                color = NothingRed
                            )
                        }
                    }
                }

                // 4. Resolved Location Details & Input
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161616),
                    border = BorderStroke(1.dp, Color(0xFF282828)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOCATION LABEL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isReverseGeocoding) NothingRed else Color(0xFF00FF66))
                                )
                                Text(
                                    text = if (isReverseGeocoding) "RESOLVING..." else "GPS LOCKED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp,
                                    color = NothingTextTertiary
                                )
                            }
                        }

                        BasicTextField(
                            value = locationNameInput,
                            onValueChange = { locationNameInput = it.uppercase() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                letterSpacing = 0.8.sp
                            ),
                            cursorBrush = SolidColor(NothingRed),
                            singleLine = true
                        )

                        // Coordinates strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "LAT: %.4f • LNG: %.4f",
                                    selectedLat,
                                    selectedLng
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingRed,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "TAP TO EDIT LABEL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.5.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }
                }

                // 5. Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Confirm
                    Box(
                        modifier = Modifier
                            .weight(1.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingRed)
                            .clickable {
                                val finalName = if (locationNameInput.isNotBlank()) {
                                    locationNameInput.trim()
                                } else {
                                    String.format(Locale.US, "PIN (%.4f, %.4f)", selectedLat, selectedLng)
                                }
                                onLocationSelected(finalName, selectedLat, selectedLng)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CONFIRM LOCATION [✓]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }
    }
}

package com.example.buddygotchi.squad

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

data class ResolvedLocation(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val sourceUrl: String? = null
)

object GoogleMapsLinkResolver {
    private const val TAG = "MapsResolver"

    private val MAPS_URL_REGEX = Regex("""(?i)(?:https?://)?[a-zA-Z0-9.-]*(?:maps\.app\.goo\.gl|goo\.gl/maps|google\.[a-z.]+/maps|maps\.google\.[a-z.]+|maps\.apple\.com|openstreetmap\.org)[^\s<>"{}|\\^`]*""")
    private val GENERIC_URL_REGEX = Regex("""https?://[^\s<>"{}|\\^`]+""")
    private val COORD_REGEX = Regex("""^\s*(-?\d{1,2}(?:\.\d+)?)[,\s;]+(-?\d{1,3}(?:\.\d+)?)\s*$""")

    /**
     * Quickly checks whether the input string contains a web URL or raw GPS coordinates.
     */
    fun isResolvable(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        if (COORD_REGEX.matches(trimmed)) return true
        if (MAPS_URL_REGEX.containsMatchIn(trimmed)) return true
        if (GENERIC_URL_REGEX.containsMatchIn(trimmed)) return true
        return false
    }

    /**
     * Resolves a Google Maps link, Apple Maps link, OpenStreetMap link, or raw GPS coordinates.
     * Extracts venue name and exact latitude/longitude.
     */
    suspend fun resolve(input: String): ResolvedLocation? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()

        // 1. Check raw coordinates first (e.g. "42.71816, 23.24588")
        val coordMatch = COORD_REGEX.find(trimmed)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lng = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                val reverseName = NominatimGeocoder.reverseGeocode(lat, lng) ?: "PINNED COORDINATES"
                return@withContext ResolvedLocation(
                    title = reverseName.uppercase(),
                    subtitle = String.format(Locale.US, "LAT: %.4f • LNG: %.4f", lat, lng),
                    latitude = lat,
                    longitude = lng
                )
            }
        }

        // 2. Extract URL from text (e.g. user shared "Place Name https://maps.app.goo.gl/...")
        val urlMatch = MAPS_URL_REGEX.find(trimmed) ?: GENERIC_URL_REGEX.find(trimmed) ?: return@withContext null
        var initialUrl = urlMatch.value
        val textPrefix = trimmed.substringBefore(initialUrl).trim()

        if (!initialUrl.startsWith("http://", ignoreCase = true) && !initialUrl.startsWith("https://", ignoreCase = true)) {
            initialUrl = "https://$initialUrl"
        }

        try {
            // Follow redirects to get final destination URL
            val finalUrl = followRedirects(initialUrl)
            Log.d(TAG, "Resolved '$initialUrl' -> '$finalUrl'")

            // Parse coordinates
            val coords = extractCoordinates(finalUrl) ?: extractCoordinatesFromQuery(finalUrl)
            if (coords == null) {
                Log.w(TAG, "No coordinates could be extracted from: $finalUrl")
                return@withContext null
            }
            val (lat, lng) = coords

            // Parse title
            val rawTitle = extractPlaceTitle(finalUrl, textPrefix)
            val title = if (!rawTitle.isNullOrBlank()) {
                rawTitle.uppercase()
            } else {
                NominatimGeocoder.reverseGeocode(lat, lng) ?: "GOOGLE MAPS PIN"
            }

            ResolvedLocation(
                title = title,
                subtitle = String.format(Locale.US, "LAT: %.4f • LNG: %.4f", lat, lng),
                latitude = lat,
                longitude = lng,
                sourceUrl = initialUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving maps link '$initialUrl'", e)
            null
        }
    }

    private fun followRedirects(initialUrl: String, maxHops: Int = 6): String {
        var currentUrl = initialUrl
        for (i in 0 until maxHops) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; BuddyGotchi/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(URL(currentUrl), location).toString()
                    }
                } else {
                    break
                }
            } else {
                conn.disconnect()
                break
            }
        }
        return currentUrl
    }

    private fun extractCoordinates(url: String): Pair<Double, Double>? {
        // Pattern 1: Exact place pin in Google Maps data parameters: !3d<lat> and !4d<lng>
        val latMatch = Regex("""!3d(-?\d+(?:\.\d+)?)""").find(url)
        val lngMatch = Regex("""!4d(-?\d+(?:\.\d+)?)""").find(url)
        if (latMatch != null && lngMatch != null) {
            val lat = latMatch.groupValues[1].toDoubleOrNull()
            val lng = lngMatch.groupValues[1].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }

        // Pattern 2: Viewport center @<lat>,<lng>
        val viewMatch = Regex("""@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""").find(url)
        if (viewMatch != null) {
            val lat = viewMatch.groupValues[1].toDoubleOrNull()
            val lng = viewMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }

        // Pattern 3: query/ll params: ll=<lat>,<lng> or q=<lat>,<lng>
        val queryCoordMatch = Regex("""[?&](?:ll|q|query)=(?:loc:)?(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""").find(url)
        if (queryCoordMatch != null) {
            val lat = queryCoordMatch.groupValues[1].toDoubleOrNull()
            val lng = queryCoordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }

        // Pattern 4: OpenStreetMap #map=zoom/lat/lng
        val osmMatch = Regex("""#map=\d+/(-?\d+(?:\.\d+)?)/(-?\d+(?:\.\d+)?)""").find(url)
        if (osmMatch != null) {
            val lat = osmMatch.groupValues[1].toDoubleOrNull()
            val lng = osmMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }

        return null
    }

    private suspend fun extractCoordinatesFromQuery(url: String): Pair<Double, Double>? {
        val qMatch = Regex("""[?&](?:q|query)=([^&]+)""").find(url) ?: return null
        val rawQuery = qMatch.groupValues[1]
        val decoded = try {
            URLDecoder.decode(rawQuery.replace("+", " "), "UTF-8")
        } catch (_: Exception) {
            rawQuery.replace("+", " ")
        }
        val places = NominatimGeocoder.searchPlaces(decoded)
        return places.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
    }

    private fun extractPlaceTitle(url: String, textPrefix: String): String? {
        // Priority 1: Prefix text shared from Google Maps (e.g. "Двата Лъва Люлин\nhttps://...")
        if (textPrefix.isNotBlank()) {
            val cleaned = textPrefix.lines()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }
            if (!cleaned.isNullOrBlank()) {
                return cleaned
            }
        }

        // Priority 2: Extract /place/<TITLE>/ from Google Maps URL
        val placeMatch = Regex("""/place/([^/@?]+)""").find(url)
        if (placeMatch != null) {
            val raw = placeMatch.groupValues[1]
            try {
                val decoded = URLDecoder.decode(raw.replace("+", " "), "UTF-8").trim()
                if (decoded.isNotBlank()) return decoded
            } catch (_: Exception) {
                val cleaned = raw.replace("+", " ").trim()
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // Priority 3: Extract from query parameter q=
        val qMatch = Regex("""[?&](?:q|query)=([^&]+)""").find(url)
        if (qMatch != null) {
            val raw = qMatch.groupValues[1]
            try {
                val decoded = URLDecoder.decode(raw.replace("+", " "), "UTF-8").trim()
                // If it's not raw coordinates, use it as title
                if (decoded.isNotBlank() && !decoded.contains(Regex("""^-?\d+(?:\.\d+)?"""))) {
                    return decoded
                }
            } catch (_: Exception) {}
        }

        return null
    }
}

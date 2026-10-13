package com.example.buddygotchi.squad

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Cross-platform map navigation helper.
 * Ready for future iOS conversion:
 * - On Android, launches the user's preferred navigation app (Google Maps, Waze, etc.)
 *   with fallback to the Google Maps web viewer.
 * - On iOS (when ported to Kotlin Multiplatform or Swift), this same interface maps directly to
 *   UIApplication.shared.open(URL(string: "http://maps.apple.com/?ll=lat,lng&q=encodedLabel")!)
 */
object SquadMapNavigationHelper {

    fun openNavigation(context: Context, latitude: Double, longitude: Double, label: String = "SQUAD EVENT") {
        try {
            val encodedLabel = Uri.encode(label)
            // geo:lat,lng?q=lat,lng(Label) is the standard Android geo Intent
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            // Fallback to browser Google Maps
            try {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open map navigation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openSearch(context: Context, locationQuery: String) {
        val encodedQuery = Uri.encode(locationQuery)
        try {
            val geoUri = Uri.parse("geo:0,0?q=$encodedQuery")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            try {
                val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open map navigation", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

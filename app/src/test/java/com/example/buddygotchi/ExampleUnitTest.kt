package com.example.buddygotchi

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
import kotlinx.coroutines.runBlocking
import com.example.buddygotchi.squad.GoogleMapsLinkResolver

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testGoogleMapsShortlinkResolution() = runBlocking {
        val link = "https://maps.app.goo.gl/nMhDCXCCTki4U8HX8"
        assertTrue(GoogleMapsLinkResolver.isResolvable(link))
        val resolved = GoogleMapsLinkResolver.resolve(link)
        println("RESOLVED TITLE: " + resolved!!.title)
        assertEquals("ДВАТА ЛЪВА ЛЮЛИН", resolved.title)
    }
}
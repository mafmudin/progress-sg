package udinsi.dev.progress_svg

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [FoProgressConfig]. No Context / Robolectric needed: the class touches no
 * Android runtime and `Color.*` are compile-time int constants that inline into test bytecode.
 */
class FoProgressConfigTest {

    @Test
    fun defaults() {
        val c = FoProgressConfig()
        assertEquals("", c.message)
        assertEquals(20f, c.textSize, 0f)
        assertEquals(Color.WHITE, c.textColor)
        assertEquals(Color.TRANSPARENT, c.backgroundColor)
        assertTrue(c.cancelable)
        assertFalse(c.cancelOnTouchOutside)
        assertNull(c.source)
    }

    @Test
    fun svgSetsSource() {
        val c = FoProgressConfig().apply { svg("loading.svg") }
        assertEquals(FoProgressConfig.Source.Svg("loading.svg"), c.source)
    }

    @Test
    fun gifSetsSource() {
        val c = FoProgressConfig().apply { gif(123) }
        assertEquals(FoProgressConfig.Source.Gif(123), c.source)
    }

    @Test
    fun lastSourceWins() {
        val c = FoProgressConfig().apply { svg("a.svg"); gif(123) }
        assertEquals(FoProgressConfig.Source.Gif(123), c.source)
    }

    @Test(expected = IllegalStateException::class)
    fun validateThrowsWhenNoSource() {
        FoProgressConfig().validate()
    }

    @Test
    fun validatePassesWithSource() {
        FoProgressConfig().apply { svg("a.svg") }.validate() // must not throw
    }
}

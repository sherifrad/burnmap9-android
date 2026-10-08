package com.drsherif.ruleofnines

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drsherif.ruleofnines.analysis.PixelCoverageAnalyzer
import com.drsherif.ruleofnines.model.BodySurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelCoverageAnalyzerTest {
    private val head = BodySurface.FRONT_HEAD
    private val trunk = BodySurface.FRONT_TRUNK
    private val palette = mapOf(head.argb to head, trunk.argb to trunk)
    private fun bitmap(colors: IntArray) = Bitmap.createBitmap(colors, colors.size, 1, Bitmap.Config.ARGB_8888)
    private fun map() = bitmap(intArrayOf(head.argb, head.argb, trunk.argb, trunk.argb, 0, 0))

    @Test fun emptyFullHalfAndBackground() = runBlocking {
        val analyzer = PixelCoverageAnalyzer()
        val map = map()
        val blank = bitmap(IntArray(6))
        assertTrue(analyzer.analyze(map, blank, palette).all { it.fraction == 0.0 })
        val full = bitmap(IntArray(6) { Color.RED })
        assertTrue(analyzer.analyze(map, full, palette).all { it.fraction == 1.0 && it.totalPixels == 2 })
        val half = bitmap(intArrayOf(Color.RED, 0, Color.RED, 0, Color.RED, Color.RED))
        assertTrue(analyzer.analyze(map, half, palette).all { it.fraction == 0.5 })
        val outside = bitmap(intArrayOf(0, 0, 0, 0, Color.RED, Color.RED))
        assertTrue(analyzer.analyze(map, outside, palette).all { it.paintedPixels == 0 })
    }

    @Test fun alphaBoundaryAndOnlyPureRedCounts() = runBlocking {
        val paint = bitmap(intArrayOf(
            Color.argb(127, 255, 0, 0), Color.argb(128, 255, 0, 0),
            Color.rgb(255, 1, 0), Color.BLUE, Color.RED, Color.RED,
        ))
        val result = PixelCoverageAnalyzer().analyze(map(), paint, palette)
        assertEquals(1, result.single { it.surface == head }.paintedPixels)
        assertEquals(0, result.single { it.surface == trunk }.paintedPixels)
    }

    @Test fun duplicatePaintCannotExceedRegion() = runBlocking {
        val paint = Bitmap.createBitmap(6, 1, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paint)
        val brush = android.graphics.Paint().apply { color = Color.RED }
        repeat(10) { canvas.drawRect(0f, 0f, 6f, 1f, brush) }
        assertTrue(PixelCoverageAnalyzer().analyze(map(), paint, palette).all { it.fraction == 1.0 })
    }

    @Test(expected = IllegalArgumentException::class) fun mismatchedDimensionsRejected() = runBlocking {
        PixelCoverageAnalyzer().analyze(map(), bitmap(intArrayOf(Color.RED)), palette)
        Unit
    }

    @Test(expected = IllegalArgumentException::class) fun unknownMapColorRejected() = runBlocking {
        PixelCoverageAnalyzer().analyze(bitmap(intArrayOf(Color.CYAN, head.argb)), bitmap(IntArray(2)), palette)
        Unit
    }

    @Test(expected = IllegalArgumentException::class) fun absentRegionRejected() = runBlocking {
        PixelCoverageAnalyzer().analyze(bitmap(intArrayOf(head.argb, head.argb)), bitmap(IntArray(2)), palette)
        Unit
    }

    @Test fun mutableMapsAreNotCachedAsImmutableAssets() = runBlocking {
        val analyzer = PixelCoverageAnalyzer()
        val map = map().copy(Bitmap.Config.ARGB_8888, true)
        val paint = bitmap(IntArray(6))
        analyzer.analyze(map, paint, palette)
        map.setPixel(0, 0, Color.CYAN)
        try {
            analyzer.analyze(map, paint, palette)
            throw AssertionError("Modified invalid map was silently cached")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun cancellationIsNotReportedAsSuccess() = runBlocking {
        var published = false
        val job = launch {
            PixelCoverageAnalyzer().analyze(map(), bitmap(IntArray(6)), palette)
            published = true
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(!published)
    }
}

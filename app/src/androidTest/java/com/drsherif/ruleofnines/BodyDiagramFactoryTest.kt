package com.drsherif.ruleofnines

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drsherif.ruleofnines.analysis.PixelCoverageAnalyzer
import com.drsherif.ruleofnines.graphics.BitmapTransform
import com.drsherif.ruleofnines.graphics.BodyDiagramFactory
import com.drsherif.ruleofnines.graphics.PaintController
import com.drsherif.ruleofnines.model.BodySurface
import com.drsherif.ruleofnines.model.BodyView
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyDiagramFactoryTest {
    @Test fun baseAndHiddenMapHaveIdenticalSupportAndAllRegionsExist() = runBlocking {
        BodyView.entries.forEach { view ->
            val diagram = BodyDiagramFactory.create(view)
            assertFalse(diagram.hiddenMap.isMutable)
            val base = IntArray(512 * 1024)
            val map = IntArray(base.size)
            diagram.baseBitmap.getPixels(base, 0, 512, 0, 0, 512, 1024)
            diagram.hiddenMap.getPixels(map, 0, 512, 0, 0, 512, 1024)
            assertTrue(base.indices.all { Color.alpha(base[it]) == Color.alpha(map[it]) })
            val blank = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888)
            val result = PixelCoverageAnalyzer().analyze(diagram.hiddenMap, blank, diagram.palette)
            assertEquals(BodySurface.forView(view).toSet(), result.map { it.surface }.toSet())
            assertTrue(result.all { it.totalPixels > 0 })
        }
    }

    @Test fun correctPatientOrientationAndPerineumPlacement() {
        val front = BodyDiagramFactory.create(BodyView.FRONT).hiddenMap
        val back = BodyDiagramFactory.create(BodyView.BACK).hiddenMap
        assertEquals(BodySurface.FRONT_RIGHT_ARM.argb, front.getPixel(128, 320))
        assertEquals(BodySurface.FRONT_LEFT_ARM.argb, front.getPixel(384, 320))
        assertEquals(BodySurface.BACK_LEFT_ARM.argb, back.getPixel(128, 320))
        assertEquals(BodySurface.BACK_RIGHT_ARM.argb, back.getPixel(384, 320))
        assertEquals(BodySurface.FRONT_RIGHT_LEG.argb, front.getPixel(211, 750))
        assertEquals(BodySurface.BACK_LEFT_LEG.argb, back.getPixel(211, 750))
        assertEquals(BodySurface.PERINEUM.argb, front.getPixel(256, 560))
        assertTrue(back.getPixel(256, 560) != BodySurface.PERINEUM.argb)
        assertEquals(BodySurface.FRONT_HEAD.argb, front.getPixel(256, 160))
    }

    @Test fun maskingDoesNotCountBaseAndClipsOutsidePaint() = runBlocking {
        val diagram = BodyDiagramFactory.create(BodyView.FRONT)
        val raw = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888)
        val blankMasked = PixelCoverageAnalyzer.maskedPaint(raw, diagram.baseBitmap)
        assertEquals(0, blankMasked.getPixel(256, 300))
        assertTrue(PixelCoverageAnalyzer().analyze(diagram.hiddenMap, blankMasked, diagram.palette).all { it.paintedPixels == 0 })
        raw.eraseColor(Color.RED)
        val masked = PixelCoverageAnalyzer.maskedPaint(raw, diagram.baseBitmap)
        assertEquals(0, masked.getPixel(0, 0))
        assertEquals(Color.RED, masked.getPixel(256, 300))
        assertTrue(PixelCoverageAnalyzer().analyze(diagram.hiddenMap, masked, diagram.palette).all { it.fraction == 1.0 })
    }

    @Test fun fitTransformRoundTripsAtDifferentSizesAndLetterboxes() {
        listOf(400f to 600f, 800f to 300f).forEach { (width, height) ->
            val transform = BitmapTransform.fit(width, height)
            val point = Offset(211f, 750f)
            val result = transform.toBitmap(transform.toViewport(point))
            assertEquals(point.x, result.x, 0.001f)
            assertEquals(point.y, result.y, 0.001f)
            assertTrue(transform.offset.x > 0f || transform.offset.y > 0f)
        }
    }

    @Test fun snapshotsAreIndependentAndStrokesDoNotConnect() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val controller = PaintController()
            controller.begin(BodyView.FRONT, Offset(210f, 300f), 24f)
            controller.end()
            val snapshot = controller.snapshot()
            controller.begin(BodyView.FRONT, Offset(300f, 300f), 24f)
            controller.end()
            assertEquals(0, snapshot.bitmaps.getValue(BodyView.FRONT).getPixel(300, 300))
            val latest = controller.snapshot()
            assertEquals(0, latest.bitmaps.getValue(BodyView.FRONT).getPixel(256, 300))
            assertEquals(Color.RED, latest.bitmaps.getValue(BodyView.FRONT).getPixel(300, 300))
            assertEquals(0, latest.bitmaps.getValue(BodyView.BACK).getPixel(210, 300))
            snapshot.release(); latest.release()
        }
    }
}

package com.drsherif.ruleofnines.analysis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.drsherif.ruleofnines.model.BodySurface
import com.drsherif.ruleofnines.model.SurfaceCoverage
import java.util.IdentityHashMap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PixelCoverageAnalyzer(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private data class RegionIndex(
        val palette: Map<Int, BodySurface>,
        val surfaces: List<BodySurface>,
        val labels: IntArray,
        val totals: IntArray,
    )
    private val cache = IdentityHashMap<Bitmap, RegionIndex>()
    private val cacheMutex = Mutex()

    suspend fun analyze(
        hiddenColorMapBitmap: Bitmap,
        userPaintedBitmap: Bitmap,
        palette: Map<Int, BodySurface>,
    ): List<SurfaceCoverage> = withContext(dispatcher) {
        validateBitmap(hiddenColorMapBitmap)
        validateBitmap(userPaintedBitmap)
        require(hiddenColorMapBitmap.width == userPaintedBitmap.width &&
            hiddenColorMapBitmap.height == userPaintedBitmap.height) { "Paint/map dimensions differ" }
        require(palette.isNotEmpty() && palette.values.toSet().size == palette.size) { "Invalid region palette" }
        require(palette.keys.all { Color.alpha(it) == 255 }) { "Map colors must be opaque" }

        val index = if (hiddenColorMapBitmap.isMutable) {
            buildIndex(hiddenColorMapBitmap, palette)
        } else cacheMutex.withLock {
            cache[hiddenColorMapBitmap]?.takeIf { it.palette == palette } ?: run {
                buildIndex(hiddenColorMapBitmap, palette).also { cache[hiddenColorMapBitmap] = it }
            }
        }
        val width = userPaintedBitmap.width
        val painted = IntArray(width * userPaintedBitmap.height)
        userPaintedBitmap.getPixels(painted, 0, width, 0, 0, width, userPaintedBitmap.height)
        val counts = IntArray(index.surfaces.size)
        for (y in 0 until userPaintedBitmap.height) {
            coroutineContext.ensureActive()
            for (x in 0 until width) {
                val position = y * width + x
                val label = index.labels[position]
                val pixel = painted[position]
                if (label >= 0 && Color.alpha(pixel) >= 128 &&
                    (pixel and 0x00FFFFFF) == 0x00FF0000) counts[label]++
            }
        }
        index.surfaces.mapIndexed { i, surface -> SurfaceCoverage(surface, index.totals[i], counts[i]) }
    }

    private suspend fun buildIndex(bitmap: Bitmap, palette: Map<Int, BodySurface>): RegionIndex {
        val surfaces = palette.values.toList()
        val labelsByColor = palette.mapValues { surfaces.indexOf(it.value) }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val labels = IntArray(pixels.size) { -1 }
        val totals = IntArray(surfaces.size)
        for (y in 0 until bitmap.height) {
            coroutineContext.ensureActive()
            for (x in 0 until bitmap.width) {
                val position = y * bitmap.width + x
                val color = pixels[position]
                if (Color.alpha(color) == 0) continue
                val label = requireNotNull(labelsByColor[color]) { "Unknown or antialiased color in region map" }
                labels[position] = label
                totals[label]++
            }
        }
        require(totals.all { it > 0 }) { "A required region has zero pixels" }
        return RegionIndex(palette.toMap(), surfaces, labels, totals)
    }

    private fun validateBitmap(bitmap: Bitmap) {
        require(!bitmap.isRecycled && bitmap.config == Bitmap.Config.ARGB_8888) {
            "A readable software ARGB_8888 bitmap is required"
        }
    }

    companion object {
        /** Worker-local paint-only overlay. The silhouette never becomes analysis paint. */
        fun maskedPaint(rawPaint: Bitmap, silhouette: Bitmap): Bitmap {
            require(rawPaint.width == silhouette.width && rawPaint.height == silhouette.height)
            val output = Bitmap.createBitmap(rawPaint.width, rawPaint.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawBitmap(silhouette, 0f, 0f, null)
            val paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
            canvas.drawBitmap(rawPaint, 0f, 0f, paint)
            return output
        }
    }
}

package com.drsherif.ruleofnines.graphics

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import com.drsherif.ruleofnines.model.BodyView

data class PaintSnapshot(val revision: Long, val bitmaps: Map<BodyView, Bitmap>) {
    fun release() = bitmaps.values.forEach { it.recycle() }
}

data class BitmapTransform(val scale: Float, val offset: Offset) {
    fun toBitmap(point: Offset): Offset = (point - offset) / scale
    fun toViewport(point: Offset): Offset = point * scale + offset
    companion object {
        fun fit(width: Float, height: Float): BitmapTransform {
            require(width > 0 && height > 0)
            val scale = minOf(width / BodyDiagramFactory.WIDTH, height / BodyDiagramFactory.HEIGHT)
            return BitmapTransform(scale, Offset(
                (width - BodyDiagramFactory.WIDTH * scale) / 2,
                (height - BodyDiagramFactory.HEIGHT * scale) / 2,
            ))
        }
    }
}

/** Main-thread-owned buffers. Background jobs only receive independent immutable snapshots. */
@Stable
class PaintController(private val onChanged: (Long) -> Unit = {}) {
    // Non-state diagnostic used by the instrumentation recomposition check.
    internal val canvasCompositionCount = java.util.concurrent.atomic.AtomicInteger()
    private class Buffer {
        val bitmap = Bitmap.createBitmap(BodyDiagramFactory.WIDTH, BodyDiagramFactory.HEIGHT, Bitmap.Config.ARGB_8888)
        val image = bitmap.asImageBitmap()
        val canvas = Canvas(image)
    }
    private val buffers = BodyView.entries.associateWith { Buffer() }
    var revision by mutableLongStateOf(0L)
        private set
    private var activeView: BodyView? = null
    private var lastPoint: Offset? = null
    private var activePath = Path()
    private var activeWidth = 24f

    fun image(view: BodyView): ImageBitmap = buffers.getValue(view).image

    fun begin(view: BodyView, point: Offset, width: Float) {
        assertMain()
        require(width.isFinite() && width in 8f..64f)
        activeView = view
        activeWidth = width
        lastPoint = point
        activePath = Path().apply { moveTo(point.x, point.y) }
        buffers.getValue(view).canvas.drawCircle(point, width / 2f, redPaint(fill = true))
        changed()
    }

    fun drag(point: Offset) {
        assertMain()
        val view = activeView ?: return
        val previous = lastPoint ?: return
        activePath.lineTo(point.x, point.y)
        val segment = Path().apply { moveTo(previous.x, previous.y); lineTo(point.x, point.y) }
        buffers.getValue(view).canvas.drawPath(segment, redPaint(fill = false))
        lastPoint = point
        changed()
    }

    fun end() {
        assertMain()
        activeView = null
        lastPoint = null
        activePath = Path()
    }

    fun clearAll() {
        assertMain()
        end()
        buffers.values.forEach { it.bitmap.eraseColor(android.graphics.Color.TRANSPARENT) }
        changed()
    }

    fun snapshot(): PaintSnapshot {
        assertMain()
        return PaintSnapshot(revision, buffers.mapValues { (_, buffer) ->
            checkNotNull(buffer.bitmap.copy(Bitmap.Config.ARGB_8888, false)) { "Cannot snapshot paint" }
        })
    }

    private fun redPaint(fill: Boolean) = Paint().apply {
        color = Color.Red
        isAntiAlias = true
        style = if (fill) PaintingStyle.Fill else PaintingStyle.Stroke
        strokeWidth = activeWidth
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
    }

    private fun changed() {
        revision++
        onChanged(revision)
    }

    private fun assertMain() = check(Looper.myLooper() == Looper.getMainLooper()) {
        "Paint buffers must only be accessed on the main thread"
    }
}

package com.drsherif.ruleofnines.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.drsherif.ruleofnines.model.BodyRegion
import com.drsherif.ruleofnines.model.BodySurface
import com.drsherif.ruleofnines.model.BodyView

class BodyDiagram internal constructor(
    val view: BodyView,
    val baseBitmap: Bitmap,
    val hiddenMap: Bitmap,
) {
    val image: ImageBitmap = baseBitmap.asImageBitmap()
    val palette = BodySurface.palette(view)
}

/** Schematic projections. Clinical weights do not depend on the relative sizes of these paths. */
object BodyDiagramFactory {
    const val WIDTH = 512
    const val HEIGHT = 1024

    fun create(view: BodyView): BodyDiagram {
        val paths = regionPaths(view)
        val hidden = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val mapCanvas = Canvas(hidden)
        val fill = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
        paths.forEach { (surface, path) ->
            fill.color = surface.argb
            mapCanvas.drawPath(path, fill)
        }

        // Base support is copied from the map, so decorative strokes cannot grow the mask.
        val pixels = IntArray(WIDTH * HEIGHT)
        hidden.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        val counts = mutableMapOf<BodySurface, Int>()
        val palette = BodySurface.palette(view)
        pixels.forEachIndexed { index, pixel ->
            if (Color.alpha(pixel) != 0) {
                val surface = requireNotNull(palette[pixel]) { "Unknown map color" }
                counts[surface] = (counts[surface] ?: 0) + 1
                pixels[index] = when (surface.region) {
                    BodyRegion.FRONT_TRUNK, BodyRegion.BACK_TRUNK -> Color.rgb(221, 233, 234)
                    BodyRegion.PERINEUM -> Color.rgb(190, 213, 217)
                    else -> Color.rgb(235, 242, 241)
                }
            }
        }
        require(counts.keys == palette.values.toSet()) { "A required body surface is missing" }
        val base = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        }
        val canvas = Canvas(base)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(87, 116, 124); strokeWidth = 2.5f; style = Paint.Style.STROKE
        }
        val layer = canvas.saveLayer(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), null)
        paths.forEach { (_, path) -> canvas.drawPath(path, line) }
        val maskPaint = Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(hidden, 0f, 0f, maskPaint)
        canvas.restoreToCount(layer)
        val immutableBase = checkNotNull(base.copy(Bitmap.Config.ARGB_8888, false))
        val immutableMap = checkNotNull(hidden.copy(Bitmap.Config.ARGB_8888, false))
        base.recycle()
        hidden.recycle()
        return BodyDiagram(view, immutableBase, immutableMap)
    }

    private fun regionPaths(view: BodyView): List<Pair<BodySurface, Path>> {
        fun surface(region: BodyRegion) = BodySurface.forView(view).single { it.region == region }
        val head = Path().apply {
            addOval(RectF(211f, 32f, 301f, 148f), Path.Direction.CW)
            val neck = Path().apply { addRect(231f, 135f, 281f, 182f, Path.Direction.CW) }
            op(neck, Path.Op.UNION)
        }
        val trunk = Path().apply {
            moveTo(182f, 182f)
            cubicTo(208f, 171f, 221f, 166f, 231f, 166f)
            lineTo(281f, 166f)
            cubicTo(291f, 166f, 304f, 171f, 330f, 182f)
            lineTo(316f, 314f)
            cubicTo(306f, 380f, 306f, 413f, 314f, 449f)
            lineTo(324f, 525f); lineTo(300f, 560f); lineTo(268f, 575f)
            lineTo(256f, 548f); lineTo(244f, 575f); lineTo(212f, 560f)
            lineTo(188f, 525f); lineTo(198f, 449f)
            cubicTo(206f, 413f, 206f, 380f, 196f, 314f)
            close()
        }
        val viewerLeftArm = Path().apply {
            moveTo(182f, 182f)
            cubicTo(154f, 190f, 132f, 236f, 126f, 278f)
            lineTo(113f, 355f); lineTo(92f, 443f)
            cubicTo(78f, 458f, 70f, 490f, 78f, 504f)
            cubicTo(84f, 516f, 100f, 514f, 106f, 500f)
            lineTo(120f, 473f); lineTo(143f, 379f); lineTo(160f, 296f)
            lineTo(190f, 245f); close()
        }
        val viewerLeftLeg = Path().apply {
            moveTo(188f, 525f); lineTo(244f, 555f); lineTo(250f, 585f)
            lineTo(236f, 748f); lineTo(222f, 947f)
            cubicTo(222f, 963f, 216f, 974f, 210f, 982f)
            lineTo(153f, 982f)
            cubicTo(142f, 976f, 145f, 966f, 161f, 949f)
            lineTo(170f, 908f); lineTo(179f, 720f); close()
        }
        fun mirrored(path: Path) = Path(path).apply {
            transform(android.graphics.Matrix().apply { setScale(-1f, 1f); postTranslate(WIDTH.toFloat(), 0f) })
        }
        val leftArmRegion = if (view == BodyView.FRONT) BodyRegion.RIGHT_ARM else BodyRegion.LEFT_ARM
        val rightArmRegion = if (view == BodyView.FRONT) BodyRegion.LEFT_ARM else BodyRegion.RIGHT_ARM
        val leftLegRegion = if (view == BodyView.FRONT) BodyRegion.RIGHT_LEG else BodyRegion.LEFT_LEG
        val rightLegRegion = if (view == BodyView.FRONT) BodyRegion.LEFT_LEG else BodyRegion.RIGHT_LEG
        return buildList {
            add(surface(BodyRegion.HEAD_NECK) to head)
            add(surface(if (view == BodyView.FRONT) BodyRegion.FRONT_TRUNK else BodyRegion.BACK_TRUNK) to trunk)
            add(surface(leftArmRegion) to viewerLeftArm)
            add(surface(rightArmRegion) to mirrored(viewerLeftArm))
            add(surface(leftLegRegion) to viewerLeftLeg)
            add(surface(rightLegRegion) to mirrored(viewerLeftLeg))
            if (view == BodyView.FRONT) {
                add(BodySurface.PERINEUM to Path().apply {
                    moveTo(244f, 552f); lineTo(268f, 552f); lineTo(256f, 585f); close()
                })
            }
        }
    }
}

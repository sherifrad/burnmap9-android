package com.drsherif.ruleofnines.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.drsherif.ruleofnines.graphics.BitmapTransform
import com.drsherif.ruleofnines.graphics.BodyDiagram
import com.drsherif.ruleofnines.graphics.PaintController

@Composable
fun BodyPaintCanvas(
    diagram: BodyDiagram,
    controller: PaintController,
    brushWidth: Float,
    modifier: Modifier = Modifier,
) {
    SideEffect { controller.canvasCompositionCount.incrementAndGet() }
    Canvas(
        modifier = modifier
            .testTag("bodyCanvas")
            .semantics { contentDescription = "${diagram.view.title} adult body. Paint burned areas with a finger." }
            .pointerInput(diagram.view, controller, brushWidth) {
                fun transform() = BitmapTransform.fit(size.width.toFloat(), size.height.toFloat())
                detectDragGestures(
                    onDragStart = { controller.begin(diagram.view, transform().toBitmap(it), brushWidth) },
                    onDragEnd = { controller.end() },
                    onDragCancel = { controller.end() },
                    onDrag = { change, _ ->
                        change.consume()
                        controller.drag(transform().toBitmap(change.position))
                    },
                )
            }
            .pointerInput(diagram.view, controller, brushWidth) {
                detectTapGestures(onTap = {
                    val transform = BitmapTransform.fit(size.width.toFloat(), size.height.toFloat())
                    controller.begin(diagram.view, transform.toBitmap(it), brushWidth)
                    controller.end()
                })
            },
    ) {
        // This state read invalidates drawing, without recomposing the canvas on each touch.
        @Suppress("UNUSED_VARIABLE") val revision = controller.revision
        val transform = BitmapTransform.fit(size.width, size.height)
        withTransform({
            translate(transform.offset.x, transform.offset.y)
            scale(transform.scale, transform.scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
        }) {
            drawImage(diagram.image)
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, 0f, diagram.image.width.toFloat(), diagram.image.height.toFloat()), Paint())
            }
            drawImage(diagram.image)
            drawImage(controller.image(diagram.view), blendMode = BlendMode.SrcIn)
            drawIntoCanvas { it.restore() }
        }
    }
}

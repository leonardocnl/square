package br.com.square

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.eteks.sweethome3d.model.Home
import kotlin.math.pow

internal val background = Color(0xFFF4F4F0)
internal val ink = Color(0xFF222222)
internal val roomFill = Color(0xFFEBEBE7)

/**
 * Planta 2D. Diferença intencional em relação ao SH3D: roda = zoom no cursor;
 * pan com botão do meio ou espaço + arraste; F ou Ctrl+0 enquadram o projeto.
 */
@Composable
internal fun PlanCanvas(home: Home, view: SquarePlanView, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    var spaceDown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Canvas(
        modifier.fillMaxSize()
            .onSizeChanged { view.onSize(it.toSize()) }
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                when {
                    e.key == Key.Spacebar -> { spaceDown = e.type == KeyEventType.KeyDown; true }
                    e.type != KeyEventType.KeyDown -> false
                    e.key == Key.F || (e.isCtrlPressed && e.key == Key.Zero) -> { view.fit(); true }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        val change = e.changes.first()
                        when (e.type) {
                            PointerEventType.Press -> focus.requestFocus()
                            PointerEventType.Scroll -> {
                                // Roda para cima = scrollDelta.y negativo = aproxima.
                                view.zoomAt(ZOOM_STEP.pow(-change.scrollDelta.y), change.position)
                                change.consume()
                            }
                            PointerEventType.Move ->
                                if (e.buttons.isTertiaryPressed || (spaceDown && e.buttons.isPrimaryPressed)) {
                                    view.pan(change.positionChange())
                                    change.consume()
                                }
                        }
                    }
                }
            }
    ) {
        view.version // lido para redesenhar quando o modelo muda
        view.drawCount++

        fun pathOf(points: Array<FloatArray>): Path = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p[0], p[1]) else lineTo(p[0], p[1]) }
            close()
        }

        withTransform({
            translate(view.offset.x, view.offset.y)
            scale(view.getScale(), view.scale, Offset.Zero)
        }) {
            home.rooms.forEach { drawPath(pathOf(it.points), roomFill) }
            // getPoints() de Wall já devolve o contorno com as junções calculadas
            // pelo próprio SH3D.
            home.walls.forEach { drawPath(pathOf(it.points), ink) }
        }
    }
}

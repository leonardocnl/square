package br.com.square

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import com.eteks.sweethome3d.io.HomeFileRecorder
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Verificação visual padrão: renderiza o PlanCanvas sem janela, dirige a
// entrada (roda, botão do meio, teclas) e salva PNGs em app/build/screenshots.
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class PlanScreenshotTest {
    private fun assertNear(expected: Float, actual: Float, tol: Float = 0.5f) =
        assertTrue(abs(expected - actual) <= tol, "esperado $expected, veio $actual")

    @Test
    fun fitCameraCentersBounds() {
        val (scale, offset) = fitCamera(Rect(0f, 0f, 400f, 300f), Size(800f, 600f), margin = 0f)
        assertEquals(2f, scale)
        assertEquals(Offset.Zero, offset)
    }

    @Test
    fun offsetAfterZoomKeepsAnchorFixed() {
        val offset = Offset(30f, 70f)
        val anchor = Offset(200f, 100f)
        val model = (anchor - offset) / 0.5f
        val newOffset = offsetAfterZoom(offset, 0.5f, 1.5f, anchor)
        assertEquals(anchor, newOffset + model * 1.5f)
    }

    @Test
    fun zoomPanAndFitOnRealInput() {
        val home = HomeFileRecorder().readHome("samples/teste-simples.sh3d")
        val view = SquareProject(home).planView
        val scene = ImageComposeScene(800, 600, Density(1f)) { PlanCanvas(home, view) }
        val dir = File("app/build/screenshots").apply { mkdirs() }
        fun shot(name: String) {
            File(dir, "$name.png").writeBytes(scene.render().encodeToData()!!.bytes)
        }

        // 1. Enquadrado ao abrir
        scene.render()
        shot("1-fit")
        val fitScale = view.getScale()
        val fitOffset = view.offset
        assertTrue(fitScale > 0.5f, "o enquadramento deveria ampliar o projeto pequeno")

        // 2. Zoom da roda no cursor: o ponto do modelo sob o cursor não se move
        val cursor = Offset(600f, 200f)
        val modelX = view.convertXPixelToModel(600)
        val modelY = view.convertYPixelToModel(200)
        scene.sendPointerEvent(PointerEventType.Move, cursor)
        scene.sendPointerEvent(PointerEventType.Scroll, cursor, scrollDelta = Offset(0f, -3f))
        scene.render()
        shot("2-zoom")
        assertNear(fitScale * ZOOM_STEP.pow(3), view.getScale(), 0.001f)
        assertNear(modelX, view.convertXPixelToModel(600))
        assertNear(modelY, view.convertYPixelToModel(200))

        // 3. Pan com o botão do meio
        val before = view.offset
        val middle = PointerButtons(isTertiaryPressed = true)
        scene.sendPointerEvent(PointerEventType.Press, cursor, buttons = middle, button = PointerButton.Tertiary)
        scene.sendPointerEvent(PointerEventType.Move, cursor + Offset(100f, 50f), buttons = middle)
        scene.sendPointerEvent(PointerEventType.Release, cursor + Offset(100f, 50f), button = PointerButton.Tertiary)
        scene.render()
        shot("3-pan")
        assertNear(before.x + 100f, view.offset.x)
        assertNear(before.y + 50f, view.offset.y)

        // 4. F enquadra de novo
        scene.sendKeyEvent(KeyEvent(Key.F, KeyEventType.KeyDown))
        scene.render()
        assertNear(fitScale, view.getScale(), 0.001f)
        assertNear(fitOffset.x, view.offset.x)

        // 5. Limite do controller original (máximo 10)
        scene.sendPointerEvent(PointerEventType.Scroll, cursor, scrollDelta = Offset(0f, -200f))
        scene.render()
        assertEquals(10f, view.getScale())
        scene.close()
    }
}

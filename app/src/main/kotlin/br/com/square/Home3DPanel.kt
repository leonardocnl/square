package br.com.square

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.jme3.input.KeyInput

/**
 * Vista 3D integrada: o jME renderiza fora da tela e cada frame vira uma imagem do Compose.
 * Mesma entrada da janela destacada ([Home3DInput]), traduzida das teclas e do mouse do Compose.
 */
@Composable
fun Home3DPanel(view: Square3DView, modifier: Modifier = Modifier, frameRate: Int = 60, onFrameForBench: (() -> Unit)? = null) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var app by remember { mutableStateOf<Square3DApp?>(null) }
    val sink = remember(view) { FrameSink() }
    val focus = remember { FocusRequester() }
    val ready = size.width > 0 && size.height > 0

    // O app jME nasce quando o tamanho é conhecido; depois só reajusta o alvo (Square3DApp.resize).
    LaunchedEffect(view, ready) {
        if (ready && !view.isOpen) {
            app = view.openIntegrated(size.width, size.height, frameRate) { buffer, w, h ->
                sink.publish(buffer, w, h)
                onFrameForBench?.invoke()
            }
        }
    }
    LaunchedEffect(app, size) { app?.let { if (ready && (size.width != it.width || size.height != it.height)) it.resize(size.width, size.height) } }
    val current = rememberUpdatedState(app)
    DisposableEffect(view) {
        onDispose {
            // Só o app do painel: ao destacar, a janela nova não pode ser fechada junto.
            current.value?.let(view::close)
            sink.close()
        }
    }

    Canvas(
        modifier.fillMaxSize()
            .onSizeChanged { size = it }
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                val code = jmeKeyCode(e.key) ?: return@onKeyEvent false
                app?.input?.key(code, e.type == KeyEventType.KeyDown, e.isShiftPressed, e.isAltPressed)
                true
            }
            .pointerInput(app) {
                awaitPointerEventScope {
                    var shift = false
                    while (true) {
                        val e = awaitPointerEvent()
                        val change = e.changes.first()
                        shift = e.keyboardModifiers.isShiftPressed
                        when (e.type) {
                            PointerEventType.Press -> focus.requestFocus()
                            PointerEventType.Scroll -> {
                                app?.input?.wheel(-change.scrollDelta.y, shift)
                                change.consume()
                            }
                            PointerEventType.Move -> if (e.buttons.isPrimaryPressed) {
                                val d = change.positionChange()
                                app?.input?.drag(d.x, d.y, shift, e.keyboardModifiers.isAltPressed)
                            }
                        }
                    }
                }
            }
    ) {
        // O jME lê de baixo para cima; inverter aqui não custa CPU.
        synchronized(sink.lock) {
            sink.current?.let { image -> scale(1f, -1f) { drawImage(image) } }
        }
    }
}

private val keyCodes = mapOf(
    Key.DirectionUp to KeyInput.KEY_UP, Key.DirectionDown to KeyInput.KEY_DOWN,
    Key.DirectionLeft to KeyInput.KEY_LEFT, Key.DirectionRight to KeyInput.KEY_RIGHT,
    Key.W to KeyInput.KEY_W, Key.S to KeyInput.KEY_S, Key.A to KeyInput.KEY_A, Key.D to KeyInput.KEY_D,
    Key.Q to KeyInput.KEY_Q, Key.E to KeyInput.KEY_E, Key.V to KeyInput.KEY_V,
    Key.PageUp to KeyInput.KEY_PRIOR, Key.PageDown to KeyInput.KEY_NEXT,
    Key.MoveHome to KeyInput.KEY_HOME, Key.MoveEnd to KeyInput.KEY_END, Key.Escape to KeyInput.KEY_ESCAPE,
)

private fun jmeKeyCode(key: Key): Int? = keyCodes[key]

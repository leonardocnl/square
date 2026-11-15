package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.viewcontroller.HomeController3D
import com.jme3.input.KeyInput

/**
 * Entrada da vista 3D, traduzida para o HomeController3D original com os mesmos
 * valores e atalhos do swing/HomeComponent3D: setas e W/S andam, setas esquerda/direita
 * e A/D giram, Alt + seta esquerda/direita desliza, PgUp/PgDn inclinam, Home/End sobem
 * e descem, Shift acelera.
 *
 * Diferenças intencionais em relação ao SH3D: V alterna vista aérea/visitante e
 * Q/E giram (como A/D). Conflito resolvido a favor do SH3D: A/D giram, não deslizam,
 * e as setas esquerda/direita giram, não andam.
 */
class Home3DInput(private val home: Home, private val controller: HomeController3D) {
    private val held = HashMap<Int, () -> Unit>()
    private var pending = 0f

    /** Arrastar com o botão esquerdo (dx, dy em pixels, y para baixo). */
    fun drag(dx: Float, dy: Float, shift: Boolean, alt: Boolean) {
        if (controller.isEditingState) return
        if (alt) {
            controller.moveCamera(1.25f * -dy * (if (shift) 5 else 1))
        } else {
            controller.rotateCameraYaw(ANGLE_FACTOR * dx * (if (shift) 5 else 1))
            controller.rotateCameraPitch(ANGLE_FACTOR * dy)
        }
    }

    /** Roda do mouse: [notches] positivo = para cima (aproxima). */
    fun wheel(notches: Float, shift: Boolean) {
        if (!controller.isEditingState) controller.moveCamera(2.5f * notches * (if (shift) 5 else 1))
    }

    fun key(code: Int, down: Boolean, shift: Boolean, alt: Boolean) {
        if (!down) {
            held.remove(code)
            return
        }
        when (code) {
            KeyInput.KEY_V -> toggleCamera()
            KeyInput.KEY_ESCAPE -> controller.escape()
            else -> actionFor(code, shift, alt)?.let { held[code] = it; it() }
        }
    }

    /** Repete as ações das teclas seguradas a ~30 vezes por segundo (a repetição do Swing). */
    fun update(tpf: Float) {
        if (held.isEmpty()) return
        pending += tpf * REPEAT_RATE
        while (pending >= 1f) {
            held.values.toList().forEach { it() }
            pending -= 1f
        }
    }

    private fun toggleCamera() {
        if (home.camera === home.topCamera) controller.viewFromObserver() else controller.viewFromTop()
    }

    private fun actionFor(code: Int, shift: Boolean, alt: Boolean): (() -> Unit)? {
        val c = controller
        val move = if (shift) 32.5f else 6.5f
        val strafe = if (shift) 10f else 2.5f
        val elevate = if (shift) 10f else 2.5f
        val yaw = if (shift) Math.PI.toFloat() / 12 else Math.PI.toFloat() / 60
        val pitch = if (shift) Math.PI.toFloat() / 24 else Math.PI.toFloat() / 120
        return when (code) {
            KeyInput.KEY_UP, KeyInput.KEY_W -> { { c.moveCamera(move) } }
            KeyInput.KEY_DOWN, KeyInput.KEY_S -> { { c.moveCamera(-move) } }
            KeyInput.KEY_LEFT -> if (alt) { { c.moveCameraSideways(-strafe) } } else { { c.rotateCameraYaw(-yaw) } }
            KeyInput.KEY_RIGHT -> if (alt) { { c.moveCameraSideways(strafe) } } else { { c.rotateCameraYaw(yaw) } }
            KeyInput.KEY_A, KeyInput.KEY_Q -> { { c.rotateCameraYaw(-yaw) } }
            KeyInput.KEY_D, KeyInput.KEY_E -> { { c.rotateCameraYaw(yaw) } }
            KeyInput.KEY_PRIOR -> { { c.rotateCameraPitch(-pitch) } }
            KeyInput.KEY_NEXT -> { { c.rotateCameraPitch(pitch) } }
            KeyInput.KEY_HOME -> { { c.elevateCamera(elevate) } }
            KeyInput.KEY_END -> { { c.elevateCamera(-elevate) } }
            else -> null
        }
    }

    private companion object {
        const val ANGLE_FACTOR = 0.005f
        const val REPEAT_RATE = 30f
    }
}

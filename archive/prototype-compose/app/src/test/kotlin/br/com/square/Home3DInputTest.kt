package br.com.square

import com.eteks.sweethome3d.model.Home
import com.jme3.input.KeyInput
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Atalhos e mouse da vista 3D chegam ao HomeController3D original e mudam a câmera do Home.
class Home3DInputTest {
    private val home = Home()
    private val project = SquareProject(home)
    private val input = Home3DInput(home, project.home3DController)

    @Test
    fun vTogglesAerialAndObserver() {
        assertEquals(home.topCamera, home.camera)
        input.key(KeyInput.KEY_V, true, shift = false, alt = false)
        assertEquals(home.observerCamera, home.camera)
        input.key(KeyInput.KEY_V, true, shift = false, alt = false)
        assertEquals(home.topCamera, home.camera)
    }

    @Test
    fun observerWalksAndTurnsWithSh3dKeys() {
        project.home3DController.viewFromObserver()
        val camera = home.observerCamera
        val (x, y) = camera.x to camera.y
        input.key(KeyInput.KEY_W, true, shift = false, alt = false)
        input.key(KeyInput.KEY_W, false, shift = false, alt = false)
        assertTrue(hypot(camera.x - x, camera.y - y) in 6.4f..6.6f, "W deveria andar 6,5 cm")

        val yaw = camera.yaw
        input.key(KeyInput.KEY_A, true, shift = false, alt = false) // SH3D: A gira, não desliza
        input.key(KeyInput.KEY_A, false, shift = false, alt = false)
        assertNotEquals(yaw, camera.yaw)

        // segurar a tecla repete a ação
        val before = camera.yaw
        input.key(KeyInput.KEY_D, true, shift = true, alt = false)
        input.update(0.1f) // 3 repetições a 30 Hz, mais a inicial
        input.key(KeyInput.KEY_D, false, shift = true, alt = false)
        assertTrue(abs(camera.yaw - before) > 3 * (Math.PI / 12 - 0.01), "a tecla segurada deveria repetir")
    }

    @Test
    fun dragAndWheelMoveTheAerialCamera() {
        val camera = home.topCamera
        val yaw = camera.yaw
        input.drag(100f, 0f, shift = false, alt = false)
        assertEquals(yaw + 0.5f, camera.yaw, 1e-4f) // 0,005 rad por pixel, como o Swing
        val z = camera.z
        input.wheel(1f, shift = false)
        assertNotEquals(z, camera.z) // aproxima: a câmera aérea se move
    }
}

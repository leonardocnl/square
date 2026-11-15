package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// O painel integrado só renderiza e entrega frames quando algo muda.
class Home3DOnDemandTest {
    private fun waitFor(timeoutMs: Long = 5000, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun rendersOnlyWhenSomethingChanges() {
        val home = Home().apply {
            addWall(Wall(0f, 0f, 600f, 0f, 10f, 250f))
            addRoom(Room(arrayOf(floatArrayOf(0f, 0f), floatArrayOf(600f, 0f), floatArrayOf(600f, 400f), floatArrayOf(0f, 400f))))
        }
        val project = SquareProject(home)
        val frames = AtomicInteger()
        project.view3D.openIntegrated(640, 480) { _, _, _ -> frames.incrementAndGet() }
        try {
            assertTrue(waitFor { frames.get() >= 2 }, "o primeiro frame não chegou")

            // parado: nenhum frame novo
            Thread.sleep(500)
            val idle = frames.get()
            Thread.sleep(1000)
            assertEquals(idle, frames.get(), "renderizou parado")

            // mudar a câmera pelo controller original gera frames; depois para de novo
            project.home3DController.rotateCameraYaw(0.3f)
            assertTrue(waitFor { frames.get() > idle }, "a câmera mudou e nenhum frame veio")
            Thread.sleep(500)
            val afterCamera = frames.get()
            Thread.sleep(1000)
            assertEquals(afterCamera, frames.get(), "continuou renderizando depois da mudança")

            // mudar o modelo também
            home.addWall(Wall(0f, 400f, 600f, 400f, 10f, 250f))
            assertTrue(waitFor { frames.get() > afterCamera }, "o modelo mudou e nenhum frame veio")
        } finally {
            project.close()
        }
    }
}

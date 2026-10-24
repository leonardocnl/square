package br.com.square

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

/**
 * Medição (não é teste de regressão): FPS do jME, ritmo de frames da interface e CPU do processo
 * com a vista 3D integrada em 1080p. Só roda com SQUARE_BENCH=1:
 *   SQUARE_BENCH=1 ./gradlew :app:test --tests '*Home3DPanelBench*' -i
 */
class Home3DPanelBenchTest {
    /** Grade de [cols] x [rows] cômodos de 4 x 4 m, com as paredes compartilhadas entre eles. */
    private fun gridHome(cols: Int, rows: Int): Home = Home().apply {
        val size = 400f
        for (r in 0..rows) for (c in 0 until cols) addWall(Wall(c * size, r * size, (c + 1) * size, r * size, 10f, 250f))
        for (c in 0..cols) for (r in 0 until rows) addWall(Wall(c * size, r * size, c * size, (r + 1) * size, 10f, 250f))
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = c * size
            val y = r * size
            addRoom(Room(arrayOf(floatArrayOf(x, y), floatArrayOf(x + size, y), floatArrayOf(x + size, y + size), floatArrayOf(x, y + size))))
        }
    }

    private fun measure(name: String, home: Home, frameRate: Int) {
        val project = SquareProject(home)
        val jmeFrames = AtomicInteger()
        val uiIntervalsMs = ArrayList<Double>()
        val cpu = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val seconds = 6
        var cpuStart = 0L
        var wallStart = 0L
        var cpuEnd = 0L
        var wallEnd = 0L
        val done = java.util.concurrent.CountDownLatch(1)

        application(exitProcessOnExit = false) {
            Window(
                onCloseRequest = ::exitApplication, title = "bench",
                state = rememberWindowState(size = DpSize(1920.dp, 1080.dp))
            ) {
                // A vista conta os frames que chegam; o contador é lido pelo laço de medição.
                Home3DPanel(project.view3D, frameRate = frameRate, onFrameForBench = { jmeFrames.incrementAndGet() })
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000) // aquece (shaders, tamanho final)
                    jmeFrames.set(0)
                    cpuStart = cpu.processCpuTime
                    wallStart = System.nanoTime()
                    var last = androidx.compose.runtime.withFrameNanos { it }
                    val end = wallStart + seconds * 1_000_000_000L
                    while (System.nanoTime() < end) {
                        val now = androidx.compose.runtime.withFrameNanos { it }
                        uiIntervalsMs += (now - last) / 1e6
                        last = now
                    }
                    cpuEnd = cpu.processCpuTime
                    wallEnd = System.nanoTime()
                    done.countDown()
                    exitApplication()
                }
            }
        }
        check(done.await(90, java.util.concurrent.TimeUnit.SECONDS)) { "a medição não terminou" }
        project.close()
        val wallS = (wallEnd - wallStart) / 1e9
        val sorted = uiIntervalsMs.sorted()
        println(
            "BENCH $name cap=$frameRate: jME %.1f fps | UI %.1f fps, intervalo p50 %.1f ms p95 %.1f ms max %.1f ms | CPU %.0f%% de um núcleo"
                .format(
                    jmeFrames.get() / wallS, sorted.size / wallS, sorted[sorted.size / 2],
                    sorted[(sorted.size * 0.95).toInt()], sorted.last(), (cpuEnd - cpuStart) / 1e9 / wallS * 100
                )
        )
    }

    @Test
    fun integratedPanelAt1080p() {
        if (System.getenv("SQUARE_BENCH") == null) return
        // Só com limite de 60 fps: sem limite o jME gera centenas de imagens de 8 MB por segundo
        // e a memória nativa do Skia (que o GC não enxerga) esgotou a RAM da máquina.
        measure("simples", gridHome(1, 1), 60)
        measure("grande(12x10)", gridHome(12, 10), 60)
    }

    /** Atribui o custo: jME sozinho em 1080p, sem leitura, só com leitura dos pixels, e sem sombra/SSAO não medido aqui. */
    @Test
    fun jmeCostBreakdownAt1080p() {
        if (System.getenv("SQUARE_BENCH") == null) return
        for ((name, home) in listOf("simples" to gridHome(1, 1), "grande(12x10)" to gridHome(12, 10))) {
            for (read in listOf(false, true)) {
                val project = SquareProject(home)
                val app = project.view3D.openOffscreen(1920, 1080, 0) // sem limite de fps
                val count = AtomicInteger()
                if (read) app.frameListener = { _, _, _ -> count.incrementAndGet() }
                app.screenshot() // espera a cena pronta
                val framesBefore = app.frameCount
                val t0 = System.nanoTime()
                Thread.sleep(4000)
                val fps = (app.frameCount - framesBefore) / ((System.nanoTime() - t0) / 1e9)
                project.close()
                println("BENCH jME %s leitura=%s: %.1f fps".format(name, read, fps))
            }
        }
    }
}

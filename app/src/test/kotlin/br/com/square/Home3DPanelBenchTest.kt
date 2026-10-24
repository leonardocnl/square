package br.com.square

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import com.sun.management.OperatingSystemMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

/**
 * Medições (não são testes de regressão) da vista 3D integrada em 1080p, girando a câmera a cada frame.
 * Só rodam com SQUARE_BENCH=1 e SEMPRE pelo script com limite de memória:
 *   SQUARE_BENCH=1 TIMEOUT=400 ./scripts/run-limited.sh :app:test --tests '*Home3DPanelBench*' -i
 * Um vigia aborta o processo se a memória do grupo passar de [ABORT_BYTES], antes do limite do sistema.
 */
class Home3DPanelBenchTest {
    private val enabled = System.getenv("SQUARE_BENCH") != null

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

    // --- memória -------------------------------------------------------------------------------

    private val cgroupDir: File? = runCatching {
        File("/proc/self/cgroup").readText().trim().substringAfter("0::").let { File("/sys/fs/cgroup$it") }
    }.getOrNull()?.takeIf { File(it, "memory.current").exists() }

    private fun cgroupLong(name: String) = cgroupDir?.let { File(it, name).readText().trim().toLongOrNull() } ?: -1L
    private fun status(key: String) =
        File("/proc/self/status").readLines().first { it.startsWith("$key:") }.split(Regex("\\s+"))[1].toLong() / 1024 // MB

    private fun startWatchdog() = Thread {
        while (true) {
            val current = cgroupLong("memory.current")
            if (current > ABORT_BYTES) {
                System.err.println("BENCH ABORTADO: memória do grupo ${current / 1_048_576} MB > ${ABORT_BYTES / 1_048_576} MB")
                Runtime.getRuntime().halt(3)
            }
            Thread.sleep(500)
        }
    }.apply { isDaemon = true; start() }

    // --- cenário -------------------------------------------------------------------------------

    private class Result(val jmeFps: Double, val uiFps: Double, val p95: Double, val max: Double, val cpu: Double)

    /**
     * Abre a janela 1080p com o painel 3D e gira a câmera a cada frame por [seconds]; amostra a
     * memória a cada [sampleEverySeconds] (0 = sem amostras).
     */
    private fun scenario(home: Home, seconds: Int, sampleEverySeconds: Int = 0): Result {
        val project = SquareProject(home)
        val jmeFrames = AtomicInteger()
        val uiIntervalsMs = ArrayList<Double>()
        val cpuBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        var cpuStart = 0L
        var wallStart = 0L
        var cpuEnd = 0L
        var wallEnd = 0L
        val done = CountDownLatch(1)

        application(exitProcessOnExit = false) {
            Window(
                onCloseRequest = ::exitApplication, title = "medição 3D",
                state = rememberWindowState(size = DpSize(1920.dp, 1080.dp))
            ) {
                Home3DPanel(project.view3D, onFrameForBench = { jmeFrames.incrementAndGet() })
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000) // aquece: shaders e tamanho final
                    jmeFrames.set(0)
                    cpuStart = cpuBean.processCpuTime
                    wallStart = System.nanoTime()
                    var last = withFrameNanos { it }
                    var nextSample = wallStart
                    val end = wallStart + seconds * 1_000_000_000L
                    while (System.nanoTime() < end) {
                        project.home3DController.rotateCameraYaw(0.02f) // muda a câmera: o jME renderiza
                        val now = withFrameNanos { it }
                        uiIntervalsMs += (now - last) / 1e6
                        last = now
                        if (sampleEverySeconds > 0 && System.nanoTime() >= nextSample) {
                            nextSample += sampleEverySeconds * 1_000_000_000L
                            println(
                                "STAB t=%3ds rss=%d MB hwm=%d MB grupo=%d MB (pico %d MB) jME=%d frames"
                                    .format((System.nanoTime() - wallStart) / 1_000_000_000L, status("VmRSS"), status("VmHWM"),
                                        cgroupLong("memory.current") / 1_048_576, cgroupLong("memory.peak") / 1_048_576, jmeFrames.get())
                            )
                        }
                    }
                    cpuEnd = cpuBean.processCpuTime
                    wallEnd = System.nanoTime()
                    done.countDown()
                    exitApplication() // fecha a janela
                }
            }
        }
        check(done.await(seconds + 60L, TimeUnit.SECONDS)) { "a medição não terminou" }
        project.close()
        val wallS = (wallEnd - wallStart) / 1e9
        val sorted = uiIntervalsMs.sorted()
        return Result(
            jmeFrames.get() / wallS, sorted.size / wallS, sorted[(sorted.size * 0.95).toInt()], sorted.last(),
            (cpuEnd - cpuStart) / 1e9 / wallS * 100
        )
    }

    private fun report(name: String, r: Result) = println(
        "BENCH %s: jME %.1f fps | UI %.1f fps (p95 %.1f ms, máx %.0f ms) | CPU %.0f%% | pico RSS %d MB, pico grupo %d MB"
            .format(name, r.jmeFps, r.uiFps, r.p95, r.max, r.cpu, status("VmHWM"), cgroupLong("memory.peak") / 1_048_576)
    )

    @Test
    fun tableAt1080p() {
        if (!enabled) return
        startWatchdog()
        report("simples", scenario(gridHome(1, 1), 15))
        report("grande(12x10)", scenario(gridHome(12, 10), 15))
    }

    @Test
    fun memoryStaysStableFor2Minutes() {
        if (!enabled) return
        startWatchdog()
        report("estabilidade grande(12x10) 120 s", scenario(gridHome(12, 10), 120, sampleEverySeconds = 10))
    }

    private companion object {
        const val ABORT_BYTES = 2_600L * 1_048_576
    }
}

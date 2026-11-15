package br.com.square

import com.jme3.app.SimpleApplication
import com.jme3.app.state.AppState
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.post.SceneProcessor
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Box
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image
import com.jme3.util.BufferUtils
import java.awt.image.BufferedImage
import java.io.File
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** jME offscreen (LWJGL3): renderiza uma cena num framebuffer e devolve a imagem. */
private class OffscreenJme(private val w: Int, private val h: Int) : SimpleApplication(*emptyArray<AppState>()) {
    val image = CompletableFuture<BufferedImage>()
    private var frames = 0

    override fun simpleInitApp() {
        val fb = FrameBuffer(w, h, 1)
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth))
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8))
        viewPort.setOutputFrameBuffer(fb)
        viewPort.backgroundColor = ColorRGBA(0.95f, 0.95f, 0.93f, 1f)
        viewPort.addProcessor(object : SceneProcessor {
            override fun initialize(rm: RenderManager, vp: ViewPort) {}
            override fun reshape(vp: ViewPort, w: Int, h: Int) {}
            override fun isInitialized() = true
            override fun preFrame(tpf: Float) {}
            override fun postQueue(rq: RenderQueue) {}
            override fun postFrame(out: FrameBuffer?) {
                if (++frames < 3 || image.isDone) return
                val buf = BufferUtils.createByteBuffer(w * h * 4)
                renderManager.renderer.readFrameBuffer(fb, buf)
                val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                for (y in 0 until h) for (x in 0 until w) {
                    val i = ((h - 1 - y) * w + x) * 4 // OpenGL lê de baixo para cima
                    val r = buf[i].toInt() and 0xFF
                    val g = buf[i + 1].toInt() and 0xFF
                    val b = buf[i + 2].toInt() and 0xFF
                    img.setRGB(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
                }
                image.complete(img)
            }
            override fun cleanup() {}
            override fun setProfiler(p: com.jme3.profile.AppProfiler?) {}
        })
        val box = Geometry("caixa", Box(1f, 1f, 1f)).apply {
            material = Material(assetManager, "Common/MatDefs/Light/Lighting.j3md").apply {
                setBoolean("UseMaterialColors", true)
                setColor("Diffuse", ColorRGBA(0.8f, 0.3f, 0.2f, 1f))
                setColor("Ambient", ColorRGBA(0.8f, 0.3f, 0.2f, 1f))
            }
        }
        rootNode.attachChild(box)
        rootNode.addLight(AmbientLight(ColorRGBA(0.4f, 0.4f, 0.4f, 1f)))
        rootNode.addLight(DirectionalLight(Vector3f(-1f, -2f, -1f).normalizeLocal(), ColorRGBA.White))
        cam.location = Vector3f(3f, 3f, 5f)
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y)
    }
}

class JmeSmokeTest {
    private fun renderBox(type: JmeContext.Type = JmeContext.Type.OffscreenSurface): BufferedImage {
        val app = OffscreenJme(640, 480)
        app.setShowSettings(false)
        app.setSettings(AppSettings(true).apply {
            setResolution(640, 480)
            setRenderer(AppSettings.LWJGL_OPENGL32)
            setFrameRate(30)
        })
        app.start(type)
        try {
            return app.image.get(30, TimeUnit.SECONDS)
        } finally {
            app.stop(true)
        }
    }

    @Test
    fun jmeAloneRendersOffscreen() {
        val img = renderBox()
        File("app/build/screenshots").mkdirs()
        ImageIO.write(img, "png", File("app/build/screenshots/jme-smoke.png"))
        val colors = (0 until 480 step 8).flatMap { y -> (0 until 640 step 8).map { x -> img.getRGB(x, y) } }.toSet()
        assertTrue(colors.size >= 3, "imagem lisa demais: ${colors.size} cores")
    }

    /** Janela Compose (Skiko) aberta no mesmo processo enquanto o jME roda. */
    private fun withComposeWindow(block: () -> Unit) {
        val up = CountDownLatch(1)
        val done = CountDownLatch(1)
        val thread = thread(isDaemon = true) {
            application(exitProcessOnExit = false) {
                Window(onCloseRequest = ::exitApplication, title = "teste") {
                    LaunchedEffect(Unit) {
                        up.countDown()
                        withContext(Dispatchers.IO) { done.await() }
                        exitApplication()
                    }
                }
            }
        }
        try {
            assertTrue(up.await(30, TimeUnit.SECONDS), "janela Compose não abriu")
            block()
        } finally {
            done.countDown()
            thread.join(10_000)
        }
    }

    @Test
    fun jmeOffscreenWithComposeWindowOpen() = withComposeWindow {
        val img = renderBox()
        ImageIO.write(img, "png", File("app/build/screenshots/jme-smoke-with-compose.png"))
    }

    @Test
    fun jmeWindowWithComposeWindowOpen() = withComposeWindow {
        val img = renderBox(JmeContext.Type.Display)
        ImageIO.write(img, "png", File("app/build/screenshots/jme-smoke-window-with-compose.png"))
    }
}

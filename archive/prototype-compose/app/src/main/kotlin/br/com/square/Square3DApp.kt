package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.viewcontroller.HomeController3D
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AppState
import com.jme3.input.RawInputListener
import com.jme3.input.event.JoyAxisEvent
import com.jme3.input.event.JoyButtonEvent
import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import com.jme3.input.event.TouchEvent
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.post.SceneProcessor
import com.jme3.post.FilterPostProcessor
import com.jme3.post.ssao.SSAOFilter
import com.jme3.profile.AppProfiler
import com.jme3.shadow.DirectionalLightShadowRenderer
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.renderer.queue.RenderQueue.ShadowMode
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import com.jme3.scene.shape.Quad
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image
import com.jme3.util.BufferUtils
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan
import kotlin.math.tan

/**
 * Vista 3D do Home em jMonkeyEngine. A câmera é a do Home (alterada pelo
 * HomeController3D original); aqui só se lê e se aplica. [offscreen] renderiza
 * num framebuffer próprio, de onde [screenshot] lê a imagem (testes). Com [onDemand] (painel
 * integrado) só renderiza e entrega frames quando a câmera ou o modelo mudam; parado, não custa GPU
 * nem leitura de pixels.
 */
class Square3DApp(
    private val home: Home,
    controller: HomeController3D,
    private val offscreen: Boolean = false,
    private val onDemand: Boolean = false,
    private val onClosed: (WindowBounds?) -> Unit = {},
) : SimpleApplication(*emptyArray<AppState>()) {
    val input = Home3DInput(home, controller)

    private val renderRequested = AtomicBoolean(true)
    private var renderFramesLeft = 0
    private val geometryDirty = AtomicBoolean(true)
    private val cameraDirty = AtomicBoolean(true)
    private val sceneNode = Node("home")
    private var watcher: HomeChangeWatcher? = null

    private var targetBuffer: FrameBuffer? = null
    /**
     * Modo integrado: chamado na thread do jME a cada frame offscreen com os pixels RGBA
     * (de baixo para cima). O buffer só vale durante a chamada.
     */
    @Volatile var frameListener: ((ByteBuffer, Int, Int) -> Unit)? = null
    @Volatile private var frames = 0
    val frameCount get() = frames
    @Volatile private var captureRequest: Pair<Int, CompletableFuture<BufferedImage>>? = null

    override fun simpleInitApp() {
        if (offscreen) initOffscreenTarget()
        addCaptureProcessor()
        rootNode.attachChild(sceneNode)
        rootNode.addLight(AmbientLight(ColorRGBA(0.45f, 0.45f, 0.45f, 1f)))
        val sun = DirectionalLight(Vector3f(-0.4f, -1f, -0.6f).normalizeLocal(), ColorRGBA(0.9f, 0.9f, 0.9f, 1f))
        rootNode.addLight(sun)
        addShadowsAndSsao(sun)
        watcher = HomeChangeWatcher(
            home,
            { geometryDirty.set(true); renderRequested.set(true) },
            { cameraDirty.set(true); renderRequested.set(true) }
        )
        inputManager?.addRawInputListener(MouseAndKeys())
    }

    /** Valores provisórios (cm): o visual final será decidido depois. */
    private fun addShadowsAndSsao(sun: DirectionalLight) {
        viewPort.addProcessor(DirectionalLightShadowRenderer(assetManager, SHADOW_MAP_SIZE, 3).apply {
            light = sun
            shadowIntensity = 0.5f
            shadowZExtend = SHADOW_DISTANCE
        })
        viewPort.addProcessor(FilterPostProcessor(assetManager).apply {
            addFilter(SSAOFilter(SSAO_RADIUS, SSAO_INTENSITY, SSAO_SCALE, SSAO_BIAS))
        })
    }

    override fun simpleUpdate(tpf: Float) {
        input.update(tpf)
        if (geometryDirty.getAndSet(false)) rebuild()
        if (cameraDirty.getAndSet(false)) syncCamera()
        if (onDemand) {
            // 2 frames por mudança: o do estado novo e mais um para sombra/SSAO assentarem.
            if (renderRequested.getAndSet(false)) renderFramesLeft = SETTLE_FRAMES
            viewPort.isEnabled = renderFramesLeft > 0 || captureRequest != null
            if (renderFramesLeft > 0) renderFramesLeft--
        }
    }

    override fun destroy() {
        watcher?.close()
        val window = context as? com.jme3.system.lwjgl.LwjglWindow
        // No Wayland o GLFW não informa a posição da janela (o compositor decide): só tamanho.
        val knowsPosition = org.lwjgl.glfw.GLFW.glfwGetPlatform() != org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
        val bounds = if (!offscreen && window != null) {
            WindowBounds(
                if (knowsPosition) window.windowXPosition else null,
                if (knowsPosition) window.windowYPosition else null,
                settings.width, settings.height
            )
        } else null
        super.destroy()
        onClosed(bounds)
    }

    /** Reconstrói tudo; ainda sem atualização incremental por item (pendente). */
    private fun rebuild() {
        sceneNode.detachAllChildren()
        val env = home.environment
        viewPort.backgroundColor = rgb(env.skyColor)
        sceneNode.attachChild(ground(rgb(env.groundColor)))
        for (wall in home.walls) {
            val mesh = wallMesh(home, wall) ?: continue
            sceneNode.attachChild(geometry("wall", mesh, rgb(wall.leftSideColor ?: DEFAULT_WALL_COLOR), ShadowMode.CastAndReceive))
        }
        for (room in home.rooms) {
            roomMesh(home, room, ceiling = false)?.let {
                sceneNode.attachChild(geometry("floor", it, rgb(room.floorColor ?: DEFAULT_FLOOR_COLOR), ShadowMode.Receive))
            }
            roomMesh(home, room, ceiling = true)?.let {
                sceneNode.attachChild(geometry("ceiling", it, rgb(room.ceilingColor ?: DEFAULT_CEILING_COLOR), ShadowMode.Off))
            }
        }
        cameraDirty.set(true)
    }

    private fun rgb(color: Int) = ColorRGBA((color shr 16 and 0xFF) / 255f, (color shr 8 and 0xFF) / 255f, (color and 0xFF) / 255f, 1f)

    private fun material(color: ColorRGBA) = Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md").apply {
        setColor("BaseColor", color)
        setFloat("Metallic", 0f)
        setFloat("Roughness", 0.9f)
    }

    private fun geometry(name: String, data: MeshData, color: ColorRGBA, shadows: ShadowMode) = Geometry(name, Mesh().apply {
        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(*data.positions))
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(*data.normals))
        setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(*data.indices))
        updateBound()
        updateCounts()
    }).apply {
        material = material(color)
        shadowMode = shadows
    }

    /** Chão grande um pouco abaixo de 0 para não brigar com os pisos. */
    private fun ground(color: ColorRGBA) = Geometry("ground", Quad(GROUND_SIZE, GROUND_SIZE)).apply {
        material = material(color)
        shadowMode = ShadowMode.Receive
        setLocalRotation(Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X))
        setLocalTranslation(-GROUND_SIZE / 2, -0.5f, GROUND_SIZE / 2)
    }

    /** Mesma transformação do HomeComponent3D: posição (x, z, y), rotY(-yaw + PI) · rotX(-pitch). */
    private fun syncCamera() {
        val camera = home.camera
        val rotation = Quaternion().fromAngleAxis(-camera.yaw + FastMath.PI, Vector3f.UNIT_Y)
            .mult(Quaternion().fromAngleAxis(-camera.pitch, Vector3f.UNIT_X))
        cam.location = Vector3f(camera.x, camera.z, camera.y)
        cam.lookAtDirection(rotation.mult(Vector3f(0f, 0f, -1f)), rotation.mult(Vector3f.UNIT_Y))
        // O campo de visão do SH3D é horizontal; o do jME é vertical.
        val horizontal = if (camera.fieldOfView == 0f) Math.toRadians(63.0).toFloat() else camera.fieldOfView
        val aspect = cam.width.toFloat() / cam.height
        val vertical = 2 * atan(tan(horizontal / 2f) / aspect)
        cam.setFrustumPerspective(Math.toDegrees(vertical.toDouble()).toFloat(), aspect, NEAR, FAR)
    }

    // Vêm das configurações, não de `cam`: o jME só cria a câmera na própria thread, depois do start().
    val width get() = settings.width
    val height get() = settings.height

    /** Muda o tamanho do alvo offscreen (painel integrado redimensionado). */
    fun resize(w: Int, h: Int) {
        enqueue {
            if (offscreen && (w != cam.width || h != cam.height)) { // roda na thread do jME: `cam` já existe
                settings.setResolution(w, h)
                cam.resize(w, h, true)
                initOffscreenTarget()
                viewPort.processors.forEach { it.reshape(viewPort, w, h) }
                cameraDirty.set(true)
                renderRequested.set(true)
            }
        }
    }

    override fun reshape(w: Int, h: Int) {
        super.reshape(w, h)
        cameraDirty.set(true)
    }

    private fun initOffscreenTarget() {
        val w = cam.width
        val h = cam.height
        targetBuffer?.dispose()
        val fb = FrameBuffer(w, h, 1)
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth))
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8))
        targetBuffer = fb
        viewPort.setOutputFrameBuffer(fb)
    }

    /** Última etapa do frame: atende o pedido de captura, se houver. */
    private fun addCaptureProcessor() {
        viewPort.addProcessor(object : SceneProcessor {
            override fun initialize(rm: RenderManager, vp: ViewPort) {}
            override fun reshape(vp: ViewPort, w: Int, h: Int) {}
            override fun isInitialized() = true
            override fun preFrame(tpf: Float) {}
            override fun postQueue(rq: RenderQueue) {}
            override fun postFrame(out: FrameBuffer?) {
                frames++
                frameListener?.let { listener -> targetBuffer?.let { listener(readPixels(it), it.width, it.height) } }
                val (minFrame, future) = captureRequest ?: return
                if (frames < minFrame) return
                captureRequest = null
                future.complete(readImage(targetBuffer ?: return))
            }
            override fun cleanup() {}
            override fun setProfiler(profiler: AppProfiler?) {}
        })
    }

    private var pixels: ByteBuffer? = null

    /** Lê o framebuffer em RGBA, de baixo para cima; o buffer é reaproveitado entre frames. */
    private fun readPixels(fb: FrameBuffer): ByteBuffer {
        val buf = pixels?.takeIf { it.capacity() == fb.width * fb.height * 4 }
            ?: BufferUtils.createByteBuffer(fb.width * fb.height * 4).also { pixels = it }
        buf.clear()
        renderManager.renderer.readFrameBuffer(fb, buf)
        return buf
    }

    private fun readImage(fb: FrameBuffer): BufferedImage {
        val w = fb.width
        val h = fb.height
        val buf = readPixels(fb)
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) for (x in 0 until w) {
            val i = ((h - 1 - y) * w + x) * 4 // OpenGL lê de baixo para cima
            val r = buf[i].toInt() and 0xFF
            val g = buf[i + 1].toInt() and 0xFF
            val b = buf[i + 2].toInt() and 0xFF
            image.setRGB(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
        }
        return image
    }

    /** Captura um frame renderizado depois das mudanças feitas até agora (só no modo offscreen). */
    fun screenshot(timeoutSeconds: Long = 30): BufferedImage {
        check(offscreen) { "screenshot só existe no modo offscreen" }
        val future = CompletableFuture<BufferedImage>()
        // +2: o frame em andamento pode não ter visto as mudanças.
        captureRequest = frames + 2 to future
        return future.get(timeoutSeconds, TimeUnit.SECONDS)
    }

    /** Ponte do input cru do jME para o Home3DInput (que usa as convenções do Swing: y para baixo). */
    private inner class MouseAndKeys : RawInputListener {
        private var leftDown = false
        private var shift = false
        private var alt = false

        override fun onMouseMotionEvent(evt: MouseMotionEvent) {
            if (evt.deltaWheel != 0) input.wheel(evt.deltaWheel / WHEEL_UNITS_PER_NOTCH, shift)
            if (leftDown && (evt.dx != 0 || evt.dy != 0)) input.drag(evt.dx.toFloat(), -evt.dy.toFloat(), shift, alt)
        }

        override fun onMouseButtonEvent(evt: MouseButtonEvent) {
            if (evt.buttonIndex == 0) leftDown = evt.isPressed
        }

        override fun onKeyEvent(evt: KeyInputEvent) {
            when (evt.keyCode) {
                com.jme3.input.KeyInput.KEY_LSHIFT, com.jme3.input.KeyInput.KEY_RSHIFT -> shift = evt.isPressed
                com.jme3.input.KeyInput.KEY_LMENU, com.jme3.input.KeyInput.KEY_RMENU -> alt = evt.isPressed
                else -> if (!evt.isRepeating) input.key(evt.keyCode, evt.isPressed, shift, alt)
            }
        }

        override fun beginInput() {}
        override fun endInput() {}
        override fun onJoyAxisEvent(evt: JoyAxisEvent) {}
        override fun onJoyButtonEvent(evt: JoyButtonEvent) {}
        override fun onTouchEvent(evt: TouchEvent) {}
    }

    private companion object {
        const val SETTLE_FRAMES = 2
        const val SHADOW_MAP_SIZE = 2048
        const val SHADOW_DISTANCE = 8_000f
        const val SSAO_RADIUS = 60f
        const val SSAO_INTENSITY = 3f
        const val SSAO_SCALE = 0.02f
        const val SSAO_BIAS = 0.1f
        const val NEAR = 2.5f
        const val FAR = 100_000f
        const val GROUND_SIZE = 200_000f
        const val WHEEL_UNITS_PER_NOTCH = 1f
        const val DEFAULT_WALL_COLOR = 0xFFFFFF
        const val DEFAULT_FLOOR_COLOR = 0xC8C8C8
        const val DEFAULT_CEILING_COLOR = 0xFFFFFF
    }
}

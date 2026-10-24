package br.com.square

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.viewcontroller.HomeController3D
import com.eteks.sweethome3d.viewcontroller.View
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import java.nio.ByteBuffer

/**
 * View 3D do Square: um app jME por vez, em janela própria (destacada) ou offscreen (testes).
 * A janela destacada guarda posição e tamanho no Home com as mesmas chaves do SH3D
 * (HomePane: `<classe da vista>.detachedView*`), para os arquivos serem compatíveis.
 */
class Square3DView(private val home: Home, private val controller: HomeController3D) : View {
    private var app: Square3DApp? = null

    val isOpen get() = app != null

    /** A vista está numa janela própria (e não no painel)? Mesma propriedade `.detachedView` do SH3D. */
    var detached by mutableStateOf(home.getProperty(PROPERTY_PREFIX + DETACHED).toBoolean())
        private set

    /** Abre a vista numa janela do jME, na posição e no tamanho guardados no Home, se houver. */
    fun openWindow(): Square3DApp {
        fun number(key: String) = home.getNumericProperty(PROPERTY_PREFIX + key)?.toInt()
        val width = number(WIDTH) ?: DEFAULT_WIDTH
        val height = number(HEIGHT) ?: DEFAULT_HEIGHT
        val x = number(X)
        val y = number(Y)
        return start(false, width, height, JmeContext.Type.Display) {
            if (x != null && y != null) {
                setWindowXPosition(x)
                setWindowYPosition(y)
            } else {
                centerWindow = true
            }
        }.also {
            home.setProperty(PROPERTY_PREFIX + DETACHED, true.toString())
            detached = true
        }
    }

    /** Modo integrado: renderiza fora da tela e entrega cada frame (RGBA, de baixo para cima) a [onFrame]. */
    fun openIntegrated(width: Int, height: Int, frameRate: Int = 60, onFrame: (ByteBuffer, Int, Int) -> Unit): Square3DApp =
        start(true, width, height, JmeContext.Type.OffscreenSurface, onDemand = true, frameListener = onFrame) {
            setFrameRate(frameRate)
        }

    /** Renderiza fora da tela; use [Square3DApp.screenshot]. */
    fun openOffscreen(width: Int, height: Int, frameRate: Int = 60): Square3DApp =
        start(true, width, height, JmeContext.Type.OffscreenSurface) { setFrameRate(frameRate) }

    private fun start(
        offscreen: Boolean, width: Int, height: Int, type: JmeContext.Type,
        onDemand: Boolean = false, frameListener: ((ByteBuffer, Int, Int) -> Unit)? = null,
        configure: AppSettings.() -> Unit
    ): Square3DApp {
        check(app == null) { "a vista 3D já está aberta" }
        return Square3DApp(home, controller, offscreen, onDemand, onClosed = ::windowClosed).also {
            it.frameListener = frameListener // antes do start: o primeiro frame não pode se perder
            it.isShowSettings = false
            it.setSettings(AppSettings(true).apply {
                setResolution(width, height)
                setRenderer(AppSettings.LWJGL_OPENGL32)
                setFrameRate(60)
                setTitle("Square — 3D")
                configure()
            })
            app = it
            it.start(type)
        }
    }

    /** Chamado na thread do jME quando a vista fecha (pelo X da janela ou por [close]). */
    private fun windowClosed(bounds: WindowBounds?) {
        app = null
        if (bounds != null) {
            if (bounds.x != null && bounds.y != null) {
                home.setProperty(PROPERTY_PREFIX + X, bounds.x.toString())
                home.setProperty(PROPERTY_PREFIX + Y, bounds.y.toString())
            }
            home.setProperty(PROPERTY_PREFIX + WIDTH, bounds.width.toString())
            home.setProperty(PROPERTY_PREFIX + HEIGHT, bounds.height.toString())
            // Como no SH3D, fechar a janela destacada recoloca a vista no painel.
            home.setProperty(PROPERTY_PREFIX + DETACHED, false.toString())
            detached = false
        }
    }

    fun close() {
        app?.stop(true)
    }

    /** Fecha só [target]; não faz nada se a vista já foi aberta de outro jeito (painel → janela). */
    fun close(target: Square3DApp) {
        if (app === target) target.stop(true)
    }

    fun dispose() = close()

    private companion object {
        // Chave do SH3D: nome da classe da vista Swing + sufixo (HomePane.DETACHED_VIEW_*).
        const val PROPERTY_PREFIX = "com.eteks.sweethome3d.swing.HomeComponent3D"
        const val DETACHED = ".detachedView"
        const val X = ".detachedViewX"
        const val Y = ".detachedViewY"
        const val WIDTH = ".detachedViewWidth"
        const val HEIGHT = ".detachedViewHeight"
        const val DEFAULT_WIDTH = 1024
        const val DEFAULT_HEIGHT = 768
    }
}

/** [x] e [y] são nulos quando a plataforma (Wayland) não informa a posição. */
class WindowBounds(val x: Int?, val y: Int?, val width: Int, val height: Int)

package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.viewcontroller.HomeController3D
import com.eteks.sweethome3d.viewcontroller.View
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext

/** View 3D do Square: um app jME por vez, em janela própria ou offscreen (testes). */
class Square3DView(private val home: Home, private val controller: HomeController3D) : View {
    private var app: Square3DApp? = null

    val isOpen get() = app != null

    /** Abre a vista numa janela do jME. */
    fun openWindow(width: Int = 1024, height: Int = 768): Square3DApp = start(false, width, height, JmeContext.Type.Display)

    /** Renderiza fora da tela; use [Square3DApp.screenshot]. */
    fun openOffscreen(width: Int, height: Int): Square3DApp = start(true, width, height, JmeContext.Type.OffscreenSurface)

    private fun start(offscreen: Boolean, width: Int, height: Int, type: JmeContext.Type): Square3DApp {
        check(app == null) { "a vista 3D já está aberta" }
        return Square3DApp(home, controller, offscreen).also {
            it.isShowSettings = false
            it.setSettings(AppSettings(true).apply {
                setResolution(width, height)
                setRenderer(AppSettings.LWJGL_OPENGL32)
                setFrameRate(60)
                setTitle("Square — 3D")
            })
            it.start(type)
            app = it
        }
    }

    fun close() {
        app?.stop(true)
        app = null
    }

    fun dispose() = close()
}

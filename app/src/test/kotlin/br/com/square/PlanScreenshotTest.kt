package br.com.square

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.eteks.sweethome3d.io.HomeFileRecorder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Verificação visual padrão: renderiza o PlanCanvas sem janela e salva um PNG.
class PlanScreenshotTest {
    @Test
    fun rendersSimplePlan() {
        val home = HomeFileRecorder().readHome("samples/teste-simples.sh3d")
        val scene = ImageComposeScene(800, 600, Density(1f)) { PlanCanvas(home) }
        val png = scene.render().encodeToData()!!.bytes
        scene.close()
        val out = File("app/build/screenshots/plan.png").apply { parentFile.mkdirs() }
        out.writeBytes(png)
        assertTrue(png.size > 1000)
    }
}

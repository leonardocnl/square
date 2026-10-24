package br.com.square

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import java.io.File
import org.jetbrains.skia.Bitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// A planta reflete mudanças do modelo (listeners originais), em um redesenho por frame.
class ModelUpdateTest {
    @Test
    fun planFollowsModelChanges() {
        val wallA = Wall(0f, 0f, 400f, 0f, 10f, 250f)
        val wallB = Wall(0f, 300f, 400f, 300f, 10f, 250f)
        val home = Home().apply { addWall(wallA); addWall(wallB) }
        val view = planViewOf(home)
        val scene = ImageComposeScene(800, 600, Density(1f)) { PlanCanvas(home, view) }
        val dir = File("app/build/screenshots").apply { mkdirs() }

        var bitmap = Bitmap()
        fun render(name: String) {
            val image = scene.render()
            File(dir, "model-$name.png").writeBytes(image.encodeToData()!!.bytes)
            bitmap = Bitmap.makeFromImage(image)
        }
        fun pixelAt(x: Float, y: Float) =
            bitmap.getColor(view.convertXModelToScreen(x), view.convertYModelToScreen(y))
        val inkArgb = ink.toArgb()

        render("0-inicial")
        // O primeiro frame enquadra a planta e pede outro: renderiza até estabilizar.
        do { val n = view.drawCount; scene.render() } while (view.drawCount != n)
        assertEquals(inkArgb, pixelAt(200f, 0f))
        assertEquals(inkArgb, pixelAt(200f, 300f))
        val draws = view.drawCount

        // Mover uma parede: duas mudanças seguidas = um único redesenho
        wallA.yStart = 150f
        wallA.yEnd = 150f
        render("1-parede-movida")
        assertEquals(draws + 1, view.drawCount)
        assertNotEquals(inkArgb, pixelAt(200f, 0f))
        assertEquals(inkArgb, pixelAt(200f, 150f))

        // Adicionar um cômodo
        home.addRoom(Room(arrayOf(floatArrayOf(0f, 0f), floatArrayOf(400f, 0f), floatArrayOf(400f, 100f), floatArrayOf(0f, 100f))))
        render("2-comodo")
        assertEquals(roomFill.toArgb(), pixelAt(200f, 50f))

        // Remover uma parede
        home.deleteWall(wallB)
        render("3-parede-removida")
        assertNotEquals(inkArgb, pixelAt(200f, 300f))

        // Mudança em item adicionado depois também é observada
        val wallC = Wall(0f, 250f, 400f, 250f, 10f, 250f)
        home.addWall(wallC)
        render("4-nova")
        wallC.yStart = 280f
        wallC.yEnd = 280f
        render("5-nova-movida")
        assertEquals(inkArgb, pixelAt(200f, 280f))

        // Depois de dispose, o modelo não mexe mais na view
        view.dispose()
        val version = view.version
        wallC.yStart = 10f
        home.addWall(Wall(0f, 20f, 40f, 20f, 10f, 250f))
        assertEquals(version, view.version)
        scene.close()
    }
}

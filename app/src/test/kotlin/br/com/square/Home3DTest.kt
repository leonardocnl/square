package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class Home3DTest {
    /** Sala de 6 x 4 m com 4 paredes unidas e um cômodo. */
    private fun roomHome(): Home {
        val walls = listOf(
            Wall(0f, 0f, 600f, 0f, 10f, 250f), Wall(600f, 0f, 600f, 400f, 10f, 250f),
            Wall(600f, 400f, 0f, 400f, 10f, 250f), Wall(0f, 400f, 0f, 0f, 10f, 250f)
        )
        walls.indices.forEach { i ->
            walls[i].wallAtEnd = walls[(i + 1) % 4]
            walls[(i + 1) % 4].wallAtStart = walls[i]
        }
        return Home().apply {
            walls.forEach { addWall(it) }
            addRoom(Room(arrayOf(floatArrayOf(0f, 0f), floatArrayOf(600f, 0f), floatArrayOf(600f, 400f), floatArrayOf(0f, 400f))))
        }
    }

    private fun save(image: BufferedImage, name: String) {
        File("app/build/screenshots").mkdirs()
        ImageIO.write(image, "png", File("app/build/screenshots/$name.png"))
    }

    @Test
    fun aerialView() {
        val project = SquareProject(roomHome())
        val app = project.view3D.openOffscreen(800, 600)
        try {
            val image = app.screenshot()
            save(image, "3d-aerea")
            val colors = (0 until 600 step 6).flatMap { y -> (0 until 800 step 6).map { x -> image.getRGB(x, y) } }.toSet()
            assertTrue(colors.size > 4, "cena lisa demais: ${colors.size} cores")
        } finally {
            project.close()
        }
    }

    private fun differingPixels(a: BufferedImage, b: BufferedImage): Int {
        var count = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (a.getRGB(x, y) != b.getRGB(x, y)) count++
        return count
    }

    @Test
    fun viewFollowsCameraAndModel() {
        val home = roomHome()
        val project = SquareProject(home)
        val app = project.view3D.openOffscreen(800, 600)
        try {
            val before = app.screenshot()

            // A câmera é a do HomeController3D original: girar por ele muda a imagem
            project.home3DController.rotateCameraYaw(0.6f)
            val orbit = app.screenshot()
            save(orbit, "3d-orbita")
            assertTrue(differingPixels(before, orbit) > 20_000, "a câmera não mudou a imagem")

            // O modelo muda: remover uma parede e um cômodo atualiza a vista
            home.deleteWall(home.walls.first())
            home.deleteRoom(home.rooms.first())
            val changed = app.screenshot()
            save(changed, "3d-modelo-alterado")
            assertTrue(differingPixels(orbit, changed) > 10_000, "o modelo não mudou a imagem")
        } finally {
            project.close()
        }
    }
}

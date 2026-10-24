package br.com.square

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.eteks.sweethome3d.io.HomeFileRecorder
import com.eteks.sweethome3d.model.Home
import java.awt.FileDialog
import java.awt.Frame

// ESQUELETO INICIAL — não compilado no ambiente onde foi criado (sem acesso ao
// Maven Central). Objetivo: provar que o leitor ORIGINAL do SH3D abre um .sh3d
// e que o Compose desenha as paredes. A janela 3D (jMonkeyEngine) é o próximo
// passo do Marco 0.

private val background = Color(0xFFF4F4F0)
private val ink = Color(0xFF222222)
private val roomFill = Color(0xFFEBEBE7)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Square") {
        var home by remember { mutableStateOf<Home?>(null) }

        Column(Modifier.fillMaxSize().background(background)) {
            Button(onClick = { home = openHome() }, modifier = Modifier.padding(8.dp)) {
                Text("Abrir .sh3d")
            }
            home?.let { PlanCanvas(it) }
        }
    }
}

private fun openHome(): Home? {
    val dialog = FileDialog(null as Frame?, "Abrir projeto", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".sh3d") }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return HomeFileRecorder().readHome(dialog.directory + file)
}

@androidx.compose.runtime.Composable
private fun PlanCanvas(home: Home) {
    Canvas(Modifier.fillMaxSize()) {
        // Escala fixa provisória: 1 px = 2 cm. Pan e zoom vêm a seguir.
        val scale = 0.5f
        val origin = Offset(size.width / 2, size.height / 2)

        fun pathOf(points: Array<FloatArray>): Path = Path().apply {
            points.forEachIndexed { i, p ->
                val x = origin.x + p[0] * scale
                val y = origin.y + p[1] * scale
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        home.rooms.forEach { drawPath(pathOf(it.points), roomFill) }
        // getPoints() de Wall já devolve o contorno com as junções calculadas
        // pelo próprio SH3D.
        home.walls.forEach { drawPath(pathOf(it.points), ink) }
    }
}

package br.com.square

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.eteks.sweethome3d.io.HomeFileRecorder
import com.eteks.sweethome3d.model.Home
import java.awt.FileDialog
import java.awt.Frame

// ESQUELETO INICIAL. Objetivo: provar que o leitor ORIGINAL do SH3D abre um .sh3d
// e que o Compose desenha as paredes. A janela 3D (jMonkeyEngine) é o próximo
// passo do Marco 0.

fun main(args: Array<String>) = application {
    Window(onCloseRequest = ::exitApplication, title = "Square") {
        var home by remember { mutableStateOf(args.firstOrNull()?.let { HomeFileRecorder().readHome(it) }) }

        Column(Modifier.fillMaxSize().background(background)) {
            // Botão provisório só com foundation; os componentes próprios vêm depois.
            Box(
                Modifier.padding(8.dp).background(ink).clickable { home = openHome() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BasicText("Abrir .sh3d", style = TextStyle(color = background))
            }
            home?.let { PlanCanvas(it, remember(it) { planViewOf(it) }) }
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

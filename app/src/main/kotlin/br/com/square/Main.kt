package br.com.square

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

fun main(args: Array<String>) = application {
    Window(onCloseRequest = ::exitApplication, title = "Square") {
        var home by remember { mutableStateOf(args.firstOrNull()?.let { HomeFileRecorder().readHome(it) }) }
        val project = remember(home) { home?.let { SquareProject(it) } }
        DisposableEffect(project) { onDispose { project?.close() } }

        // Se o arquivo foi salvo com a vista 3D destacada, ela reabre destacada.
        LaunchedEffect(project) { project?.view3D?.let { if (it.detached) it.openWindow() } }

        Column(Modifier.fillMaxSize().background(background)) {
            Row {
                ToolButton("Abrir .sh3d") { home = openHome() }
                project?.let { p ->
                    ToolButton(if (p.view3D.detached) "Recolocar 3D" else "Destacar 3D") {
                        val view = p.view3D
                        if (view.detached) view.close() // a janela fechada recoloca a vista no painel
                        else {
                            view.close() // fecha o painel antes de abrir a janela
                            view.openWindow()
                        }
                    }
                }
            }
            project?.let { p ->
                Row(Modifier.fillMaxSize()) {
                    PlanCanvas(p.home, p.planView, Modifier.weight(1f))
                    if (!p.view3D.detached) Home3DPanel(p.view3D, Modifier.weight(1f))
                }
            }
        }
    }
}

// Botão provisório só com foundation; os componentes próprios vêm depois.
@Composable
private fun ToolButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(8.dp).background(ink).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BasicText(text, style = TextStyle(color = background))
    }
}

private fun openHome(): Home? {
    val dialog = FileDialog(null as Frame?, "Abrir projeto", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".sh3d") }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return HomeFileRecorder().readHome(dialog.directory + file)
}

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

        Column(Modifier.fillMaxSize().background(background)) {
            Row {
                ToolButton("Abrir .sh3d") { home = openHome() }
                project?.let { p ->
                    ToolButton("Janela 3D") { if (p.view3D.isOpen) p.view3D.close() else p.view3D.openWindow() }
                }
            }
            project?.let { PlanCanvas(it.home, it.planView) }
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

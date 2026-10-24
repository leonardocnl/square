package br.com.square

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.eteks.sweethome3d.model.Home

internal val background = Color(0xFFF4F4F0)
internal val ink = Color(0xFF222222)
internal val roomFill = Color(0xFFEBEBE7)

@androidx.compose.runtime.Composable
internal fun PlanCanvas(home: Home) {
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

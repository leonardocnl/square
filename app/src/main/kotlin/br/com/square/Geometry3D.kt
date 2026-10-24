package br.com.square

import com.eteks.sweethome3d.model.Home
import com.eteks.sweethome3d.model.Level
import com.eteks.sweethome3d.model.Room
import com.eteks.sweethome3d.model.Wall
import com.jme3.math.Vector3f
import earcut4j.Earcut
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometria 3D do modelo, sem jME: dá para testar sem GL. Coordenadas do jME:
 * (x, elevação, y do plano), como o HomeComponent3D do SH3D (x, z, y) em cm.
 *
 * Paridade: segue j3d/Wall3D.java (elevações, topo inclinado, paredes redondas)
 * e j3d/Room3D.java (sobreposição de cômodos). Ainda não cobre portas/janelas,
 * rodapés, texturas nem materiais por lado.
 */
class MeshData(val positions: FloatArray, val normals: FloatArray, val indices: IntArray)

/** Malha com normais planas; o enrolamento dos triângulos é ajustado à normal pedida. */
private class MeshBuilder {
    private val positions = ArrayList<Float>()
    private val normals = ArrayList<Float>()
    private val indices = ArrayList<Int>()

    fun triangle(a: Vector3f, b: Vector3f, c: Vector3f, normal: Vector3f) {
        val geometric = b.subtract(a).crossLocal(c.subtract(a))
        val flip = geometric.dot(normal) < 0
        val base = positions.size / 3
        for (v in if (flip) listOf(a, c, b) else listOf(a, b, c)) {
            positions += listOf(v.x, v.y, v.z)
            normals += listOf(normal.x, normal.y, normal.z)
        }
        indices += listOf(base, base + 1, base + 2)
    }

    fun quad(a: Vector3f, b: Vector3f, c: Vector3f, d: Vector3f, normal: Vector3f) {
        triangle(a, b, c, normal)
        triangle(a, c, d, normal)
    }

    fun build() = if (indices.isEmpty()) null
    else MeshData(positions.toFloatArray(), normals.toFloatArray(), indices.toIntArray())
}

/** Elevações de uma parede, como em Wall3D.getWallElevation / getWallTopElevationAt*. */
private class WallElevations(home: Home, wall: Wall) {
    val bottom: Float
    val topAtStart: Float
    val topAtEnd: Float

    init {
        val level = wall.level
        val levels = home.levels
        val floorThickness = if (level == null || (levels.isNotEmpty() && levels[0].elevation == level.elevation)) 0f
        else level.floorThickness
        bottom = when {
            level == null -> 0f
            floorThickness > 0 -> level.elevation - (floorThickness - LEVEL_ELEVATION_SHIFT)
            else -> level.elevation
        }
        val shift = if (level != null && levels.last() != level) LEVEL_ELEVATION_SHIFT else 0f
        topAtStart = (wall.height ?: home.wallHeight) + bottom + floorThickness + shift
        topAtEnd = if (wall.isTrapezoidal) wall.heightAtEnd + bottom + floorThickness + shift else topAtStart
    }
}

private const val LEVEL_ELEVATION_SHIFT = 0.1f

fun Level?.isShown() = this == null || isViewableAndVisible

private fun earcutTriangles(rings: List<List<FloatArray>>): Pair<List<FloatArray>, List<Int>> {
    val vertices = rings.flatten()
    val data = DoubleArray(vertices.size * 2)
    vertices.forEachIndexed { i, p -> data[2 * i] = p[0].toDouble(); data[2 * i + 1] = p[1].toDouble() }
    val holeStarts = rings.drop(1).runningFold(rings[0].size) { acc, ring -> acc + ring.size }.dropLast(1)
    val triangles = Earcut.earcut(data, holeStarts.toIntArray().takeIf { it.isNotEmpty() }, 2)
    return vertices to triangles
}

fun wallMesh(home: Home, wall: Wall): MeshData? {
    if (!wall.level.isShown()) return null
    val points = wall.points
    if (points.size < 3) return null
    val elevations = WallElevations(home, wall)

    // Linha do topo: elevação linear na projeção do ponto sobre o eixo da parede (Wall3D).
    val yaw = atan2((wall.yEnd - wall.yStart).toDouble(), (wall.xEnd - wall.xStart).toDouble())
    val cosYaw = cos(yaw)
    val sinYaw = sin(yaw)
    val xStart = cosYaw * wall.xStart + sinYaw * wall.yStart
    val xEnd = cosYaw * wall.xEnd + sinYaw * wall.yEnd
    val alpha = if (elevations.topAtStart == elevations.topAtEnd) 0.0
    else (elevations.topAtEnd - elevations.topAtStart) / (xEnd - xStart)
    val beta = elevations.topAtStart - alpha * xStart
    fun topAt(p: FloatArray) = (alpha * (cosYaw * p[0] + sinYaw * p[1]) + beta).toFloat()

    // Distância ao meio da parede: arestas sobre o eixo (extremidades sem espessura) não têm face.
    val arcExtent = wall.arcExtent
    val round = arcExtent != null && arcExtent != 0f
    fun distanceSqToMiddle(p: FloatArray): Double =
        if (round) {
            val center = Point2D.Float(wall.xArcCircleCenter, wall.yArcCircleCenter)
            val radius = center.distance(wall.xStart.toDouble(), wall.yStart.toDouble())
            (radius - center.distance(p[0].toDouble(), p[1].toDouble())).let { it * it }
        } else if (wall.xStart != wall.xEnd || wall.yStart != wall.yEnd) {
            java.awt.geom.Line2D.ptLineDistSq(
                wall.xStart.toDouble(), wall.yStart.toDouble(), wall.xEnd.toDouble(), wall.yEnd.toDouble(),
                p[0].toDouble(), p[1].toDouble()
            )
        } else {
            Point2D.distanceSq(wall.xStart.toDouble(), wall.yStart.toDouble(), p[0].toDouble(), p[1].toDouble())
        }

    val signedArea = points.indices.sumOf { i ->
        val p = points[i]; val q = points[(i + 1) % points.size]
        (p[0] * q[1] - q[0] * p[1]).toDouble()
    }
    val mesh = MeshBuilder()
    for (i in points.indices) {
        val p = points[i]
        val q = points[(i + 1) % points.size]
        if (distanceSqToMiddle(p) <= 0.001 && distanceSqToMiddle(q) <= 0.001) continue
        val dx = q[0] - p[0]
        val dy = q[1] - p[1]
        val outward = Vector3f(dy, 0f, -dx).let { if (signedArea < 0) it.negateLocal() else it }
        if (outward.lengthSquared() == 0f) continue
        mesh.quad(
            Vector3f(p[0], elevations.bottom, p[1]), Vector3f(q[0], elevations.bottom, q[1]),
            Vector3f(q[0], topAt(q), q[1]), Vector3f(p[0], topAt(p), p[1]),
            outward.normalizeLocal()
        )
    }
    val (vertices, triangles) = earcutTriangles(listOf(points.toList()))
    for (t in triangles.chunked(3)) {
        val (a, b, c) = t.map { vertices[it] }
        mesh.triangle(
            Vector3f(a[0], topAt(a), a[1]), Vector3f(b[0], topAt(b), b[1]), Vector3f(c[0], topAt(c), c[1]),
            Vector3f.UNIT_Y
        )
    }
    return mesh.build()
}

private class Polygon(val outer: List<FloatArray>, val holes: List<List<FloatArray>>)

/** Contornos de uma Area: o sentido dominante é o dos contornos externos; os opostos são furos. */
private fun areaToPolygons(area: Area): List<Polygon> {
    val loops = ArrayList<List<FloatArray>>()
    var current = ArrayList<FloatArray>()
    val coords = FloatArray(6)
    val it = area.getPathIterator(null, 0.5)
    while (!it.isDone) {
        when (it.currentSegment(coords)) {
            PathIterator.SEG_MOVETO -> { current = arrayListOf(floatArrayOf(coords[0], coords[1])) }
            PathIterator.SEG_LINETO -> current += floatArrayOf(coords[0], coords[1])
            PathIterator.SEG_CLOSE -> { if (current.size > 2) loops += current; current = ArrayList() }
        }
        it.next()
    }
    fun signedArea(l: List<FloatArray>) = l.indices.sumOf { i ->
        val p = l[i]; val q = l[(i + 1) % l.size]
        (p[0] * q[1] - q[0] * p[1]).toDouble()
    }
    val dominant = loops.maxByOrNull { Math.abs(signedArea(it)) }?.let { signedArea(it) } ?: return emptyList()
    val (outers, holes) = loops.partition { signedArea(it) * dominant > 0 }
    return outers.map { outer ->
        val shape = Path2D.Float().apply {
            outer.forEachIndexed { i, p -> if (i == 0) moveTo(p[0], p[1]) else lineTo(p[0], p[1]) }
            closePath()
        }
        Polygon(outer, holes.filter { shape.contains(it[0][0].toDouble(), it[0][1].toDouble()) })
    }
}

private fun Room.toArea() = Area(Path2D.Float().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p[0], p[1]) else lineTo(p[0], p[1]) }
    closePath()
})

/**
 * Piso ou teto de um cômodo. Cômodos do mesmo nível que vêm depois na lista
 * cobrem os anteriores (Room3D), então a área deles vira furo.
 */
fun roomMesh(home: Home, room: Room, ceiling: Boolean): MeshData? {
    if (!room.level.isShown() || room.points.size < 3) return null
    if (if (ceiling) !room.isCeilingVisible else !room.isFloorVisible) return null
    val level = room.level
    val elevation = if (ceiling) (level?.elevation ?: 0f) + (level?.height ?: home.wallHeight)
    else level?.elevation ?: 0f

    val area = room.toArea()
    val rooms = home.rooms.toList()
    for (other in rooms.drop(rooms.indexOf(room) + 1)) {
        val visible = if (ceiling) other.isCeilingVisible else other.isFloorVisible
        if (other.level == level && visible) area.subtract(other.toArea())
    }
    val mesh = MeshBuilder()
    val normal = if (ceiling) Vector3f(0f, -1f, 0f) else Vector3f.UNIT_Y
    for (polygon in areaToPolygons(area)) {
        val (vertices, triangles) = earcutTriangles(listOf(polygon.outer) + polygon.holes)
        for (t in triangles.chunked(3)) {
            val (a, b, c) = t.map { vertices[it] }
            mesh.triangle(
                Vector3f(a[0], elevation, a[1]), Vector3f(b[0], elevation, b[1]), Vector3f(c[0], elevation, c[1]),
                normal
            )
        }
    }
    return mesh.build()
}

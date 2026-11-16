package br.com.square.mesh;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para a camada agnostica de geometria MeshData.
 */
public class MeshDataTest {

    @Test
    public void testMeshDataNormalComputationAndBounds() {
        // Triangulo no plano XY com vertice apontando para cima
        float[] positions = new float[] {
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                0.5f, 1.0f, 0.0f
        };
        int[] indices = new int[] {0, 1, 2};

        MeshData mesh = new MeshData("TrianguloTeste", positions, null, null, indices);
        assertEquals("TrianguloTeste", mesh.getName());
        assertEquals(3, mesh.getVertexCount());
        assertEquals(1, mesh.getTriangleCount());

        // Calcula normais automaticamente
        mesh.computeNormalsIfNeeded();
        assertNotNull("Normais devem ter sido geradas", mesh.getNormals());
        assertEquals(9, mesh.getNormals().length);

        // A normal de um triangulo no plano XY (em sentido anti-horario) aponta para +Z
        assertTrue("Normal Z deve ser positiva", mesh.getNormals()[2] > 0.9f);

        // Bounding box
        float[] bounds = mesh.computeBoundingBox();
        assertEquals(0.0f, bounds[0], 0.001f); // minX
        assertEquals(0.0f, bounds[1], 0.001f); // minY
        assertEquals(0.0f, bounds[2], 0.001f); // minZ
        assertEquals(1.0f, bounds[3], 0.001f); // maxX
        assertEquals(1.0f, bounds[4], 0.001f); // maxY
        assertEquals(0.0f, bounds[5], 0.001f); // maxZ

        // Translacao e Escala
        mesh.translate(10.0f, 5.0f, -2.0f);
        float[] translatedBounds = mesh.computeBoundingBox();
        assertEquals(10.0f, translatedBounds[0], 0.001f);
        assertEquals(11.0f, translatedBounds[3], 0.001f);

        mesh.scale(2.0f, 2.0f, 2.0f);
        float[] scaledBounds = mesh.computeBoundingBox();
        assertEquals(20.0f, scaledBounds[0], 0.001f);
        assertEquals(22.0f, scaledBounds[3], 0.001f);
    }
}


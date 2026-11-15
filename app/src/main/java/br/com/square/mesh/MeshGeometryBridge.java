package br.com.square.mesh;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;

/**
 * Adaptador de conversao entre o modelo agnostico MeshData e o grafo de cena Java 3D.
 */
public final class MeshGeometryBridge {

    private MeshGeometryBridge() {
    }

    /**
     * Converte uma estrutura MeshData em um no Shape3D pronto para insercao no grafo de cena.
     */
    public static Shape3D createShape3D(MeshData meshData) {
        if (meshData == null || meshData.getPositions().length == 0) {
            return new Shape3D();
        }

        meshData.computeNormalsIfNeeded();

        int vertexCount = meshData.getVertexCount();
        int[] indices = meshData.getIndices();
        boolean hasIndices = indices != null && indices.length > 0;
        int indexCount = hasIndices ? indices.length : vertexCount;

        int vertexFormat = GeometryArray.COORDINATES | GeometryArray.NORMALS;
        boolean hasTexCoords = meshData.getTexCoords().length == vertexCount * 2;
        if (hasTexCoords) {
            vertexFormat |= GeometryArray.TEXTURE_COORDINATE_2;
        }

        IndexedTriangleArray geometry = new IndexedTriangleArray(vertexCount, vertexFormat, indexCount);
        geometry.setCoordinates(0, meshData.getPositions());
        geometry.setNormals(0, meshData.getNormals());

        if (hasTexCoords) {
            geometry.setTextureCoordinates(0, 0, meshData.getTexCoords());
        }

        if (hasIndices) {
            geometry.setCoordinateIndices(0, indices);
            geometry.setNormalIndices(0, indices);
            if (hasTexCoords) {
                geometry.setTextureCoordinateIndices(0, 0, indices);
            }
        } else {
            int[] sequentialIndices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                sequentialIndices[i] = i;
            }
            geometry.setCoordinateIndices(0, sequentialIndices);
            geometry.setNormalIndices(0, sequentialIndices);
            if (hasTexCoords) {
                geometry.setTextureCoordinateIndices(0, 0, sequentialIndices);
            }
        }

        Appearance appearance = createAppearance(meshData);
        Shape3D shape = new Shape3D(geometry, appearance);
        shape.setName(meshData.getName());
        return shape;
    }

    /**
     * Cria a aparencia visual e propriedades de iluminacao para a malha.
     */
    public static Appearance createAppearance(MeshData meshData) {
        Appearance appearance = new Appearance();

        float[] baseColor = meshData.getBaseColor();
        Color3f color = new Color3f(baseColor[0], baseColor[1], baseColor[2]);

        Material material = new Material();
        material.setDiffuseColor(color);
        material.setAmbientColor(new Color3f(color.x * 0.2f, color.y * 0.2f, color.z * 0.2f));
        material.setSpecularColor(new Color3f(0.5f, 0.5f, 0.5f));
        material.setShininess(64.0f);
        material.setLightingEnable(true);
        appearance.setMaterial(material);

        ColoringAttributes coloring = new ColoringAttributes();
        coloring.setColor(color);
        appearance.setColoringAttributes(coloring);

        // Transparencia caso o canal Alpha seja menor que 1.0
        if (baseColor.length > 3 && baseColor[3] < 0.999f) {
            TransparencyAttributes transparency = new TransparencyAttributes(
                    TransparencyAttributes.BLENDED, 1.0f - baseColor[3]);
            appearance.setTransparencyAttributes(transparency);
        }

        return appearance;
    }
}

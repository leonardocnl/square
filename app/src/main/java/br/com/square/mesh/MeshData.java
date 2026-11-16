package br.com.square.mesh;

import java.util.Arrays;

/**
 * Representacao agnostica e desacoplada de malhas geometricas tridimensionais.
 * Nao possui dependencias com classes graficas nativas ou de pipeline de renderizacao,
 * permitindo processamento headless, testes unitarios e futuras exportacoes.
 */
public class MeshData {

    private String name;
    private float[] positions;
    private float[] normals;
    private float[] texCoords;
    private int[] indices;

    // Propriedades do Material
    private float[] baseColor = new float[] {0.8f, 0.8f, 0.8f, 1.0f};
    private float metallicFactor = 0.0f;
    private float roughnessFactor = 0.5f;

    public MeshData() {
        this("mesh", new float[0], new float[0], new float[0], new int[0]);
    }

    public MeshData(String name, float[] positions, float[] normals, float[] texCoords, int[] indices) {
        this.name = name != null ? name : "mesh";
        this.positions = positions != null ? positions : new float[0];
        this.normals = normals != null ? normals : new float[0];
        this.texCoords = texCoords != null ? texCoords : new float[0];
        this.indices = indices != null ? indices : new int[0];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float[] getPositions() {
        return positions;
    }

    public void setPositions(float[] positions) {
        this.positions = positions != null ? positions : new float[0];
    }

    public float[] getNormals() {
        return normals;
    }

    public void setNormals(float[] normals) {
        this.normals = normals != null ? normals : new float[0];
    }

    public float[] getTexCoords() {
        return texCoords;
    }

    public void setTexCoords(float[] texCoords) {
        this.texCoords = texCoords != null ? texCoords : new float[0];
    }

    public int[] getIndices() {
        return indices;
    }

    public void setIndices(int[] indices) {
        this.indices = indices != null ? indices : new int[0];
    }

    public float[] getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(float r, float g, float b, float a) {
        this.baseColor = new float[] {r, g, b, a};
    }

    public float getMetallicFactor() {
        return metallicFactor;
    }

    public void setMetallicFactor(float metallicFactor) {
        this.metallicFactor = metallicFactor;
    }

    public float getRoughnessFactor() {
        return roughnessFactor;
    }

    public void setRoughnessFactor(float roughnessFactor) {
        this.roughnessFactor = roughnessFactor;
    }

    public int getVertexCount() {
        return positions.length / 3;
    }

    public int getTriangleCount() {
        if (indices.length > 0) {
            return indices.length / 3;
        }
        return positions.length / 9;
    }

    /**
     * Calcula as normais por face caso o modelo nao possua normais atribuidas.
     */
    public void computeNormalsIfNeeded() {
        if (normals != null && normals.length == positions.length && normals.length > 0) {
            return;
        }

        int vertexCount = positions.length / 3;
        float[] computedNormals = new float[positions.length];

        if (indices != null && indices.length >= 3) {
            for (int i = 0; i < indices.length; i += 3) {
                int i0 = indices[i];
                int i1 = indices[i + 1];
                int i2 = indices[i + 2];

                float v0x = positions[i0 * 3];
                float v0y = positions[i0 * 3 + 1];
                float v0z = positions[i0 * 3 + 2];

                float v1x = positions[i1 * 3];
                float v1y = positions[i1 * 3 + 1];
                float v1z = positions[i1 * 3 + 2];

                float v2x = positions[i2 * 3];
                float v2y = positions[i2 * 3 + 1];
                float v2z = positions[i2 * 3 + 2];

                float ax = v1x - v0x;
                float ay = v1y - v0y;
                float az = v1z - v0z;

                float bx = v2x - v0x;
                float by = v2y - v0y;
                float bz = v2z - v0z;

                float nx = ay * bz - az * by;
                float ny = az * bx - ax * bz;
                float nz = ax * by - ay * bx;

                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0.00001f) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }

                computedNormals[i0 * 3] += nx;
                computedNormals[i0 * 3 + 1] += ny;
                computedNormals[i0 * 3 + 2] += nz;

                computedNormals[i1 * 3] += nx;
                computedNormals[i1 * 3 + 1] += ny;
                computedNormals[i1 * 3 + 2] += nz;

                computedNormals[i2 * 3] += nx;
                computedNormals[i2 * 3 + 1] += ny;
                computedNormals[i2 * 3 + 2] += nz;
            }

            for (int i = 0; i < vertexCount; i++) {
                float nx = computedNormals[i * 3];
                float ny = computedNormals[i * 3 + 1];
                float nz = computedNormals[i * 3 + 2];
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0.00001f) {
                    computedNormals[i * 3] = nx / len;
                    computedNormals[i * 3 + 1] = ny / len;
                    computedNormals[i * 3 + 2] = nz / len;
                } else {
                    computedNormals[i * 3 + 1] = 1.0f;
                }
            }
        } else {
            // Sem indices, processa triangulos sequenciais
            for (int i = 0; i < vertexCount; i += 3) {
                if (i + 2 >= vertexCount) break;
                float v0x = positions[i * 3];
                float v0y = positions[i * 3 + 1];
                float v0z = positions[i * 3 + 2];

                float v1x = positions[(i + 1) * 3];
                float v1y = positions[(i + 1) * 3 + 1];
                float v1z = positions[(i + 1) * 3 + 2];

                float v2x = positions[(i + 2) * 3];
                float v2y = positions[(i + 2) * 3 + 1];
                float v2z = positions[(i + 2) * 3 + 2];

                float ax = v1x - v0x;
                float ay = v1y - v0y;
                float az = v1z - v0z;

                float bx = v2x - v0x;
                float by = v2y - v0y;
                float bz = v2z - v0z;

                float nx = ay * bz - az * by;
                float ny = az * bx - ax * bz;
                float nz = ax * by - ay * bx;

                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0.00001f) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }

                for (int k = 0; k < 3; k++) {
                    computedNormals[(i + k) * 3] = nx;
                    computedNormals[(i + k) * 3 + 1] = ny;
                    computedNormals[(i + k) * 3 + 2] = nz;
                }
            }
        }
        this.normals = computedNormals;
    }

    /**
     * Calcula o bounding box da malha [minX, minY, minZ, maxX, maxY, maxZ].
     */
    public float[] computeBoundingBox() {
        if (positions.length == 0) {
            return new float[] {0, 0, 0, 0, 0, 0};
        }
        float minX = positions[0], maxX = positions[0];
        float minY = positions[1], maxY = positions[1];
        float minZ = positions[2], maxZ = positions[2];

        for (int i = 3; i < positions.length; i += 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }

        return new float[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    /**
     * Translada todos os vertices da malha.
     */
    public void translate(float dx, float dy, float dz) {
        for (int i = 0; i < positions.length; i += 3) {
            positions[i] += dx;
            positions[i + 1] += dy;
            positions[i + 2] += dz;
        }
    }

    /**
     * Escala a malha nos eixos X, Y e Z.
     */
    public void scale(float sx, float sy, float sz) {
        for (int i = 0; i < positions.length; i += 3) {
            positions[i] *= sx;
            positions[i + 1] *= sy;
            positions[i + 2] *= sz;
        }
    }
}


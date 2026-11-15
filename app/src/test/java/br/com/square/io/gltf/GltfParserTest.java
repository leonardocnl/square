package br.com.square.io.gltf;

import br.com.square.mesh.MeshData;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o parser e carregador de modelos glTF 2.0 e GLB.
 */
public class GltfParserTest {

    @Test
    public void testGltfAsciiParsingWithEmbeddedBase64() throws IOException {
        // Gera um buffer de 3 vertices float (9 floats = 36 bytes)
        ByteBuffer rawBuffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        rawBuffer.putFloat(0.0f).putFloat(0.0f).putFloat(0.0f);
        rawBuffer.putFloat(2.0f).putFloat(0.0f).putFloat(0.0f);
        rawBuffer.putFloat(0.0f).putFloat(3.0f).putFloat(0.0f);

        String base64Buffer = Base64.getEncoder().encodeToString(rawBuffer.array());

        String gltfJson = "{\n" +
                "  \"asset\": { \"version\": \"2.0\" },\n" +
                "  \"buffers\": [\n" +
                "    { \"byteLength\": 36, \"uri\": \"data:application/octet-stream;base64," + base64Buffer + "\" }\n" +
                "  ],\n" +
                "  \"bufferViews\": [\n" +
                "    { \"buffer\": 0, \"byteOffset\": 0, \"byteLength\": 36 }\n" +
                "  ],\n" +
                "  \"accessors\": [\n" +
                "    { \"bufferView\": 0, \"byteOffset\": 0, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC3\" }\n" +
                "  ],\n" +
                "  \"meshes\": [\n" +
                "    {\n" +
                "      \"name\": \"PoligonoGltf\",\n" +
                "      \"primitives\": [\n" +
                "        { \"attributes\": { \"POSITION\": 0 } }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        GltfParser parser = new GltfParser();
        List<MeshData> meshes = parser.parse(gltfJson.getBytes(StandardCharsets.UTF_8));

        assertNotNull("Lista de malhas nao deve ser nula", meshes);
        assertEquals(1, meshes.size());

        MeshData mesh = meshes.get(0);
        assertEquals("PoligonoGltf", mesh.getName());
        assertEquals(3, mesh.getVertexCount());
        assertEquals(9, mesh.getPositions().length);
        assertEquals(2.0f, mesh.getPositions()[3], 0.001f);
        assertEquals(3.0f, mesh.getPositions()[7], 0.001f);
    }

    @Test
    public void testGlbBinaryContainerParsing() throws IOException {
        // Buffer binario com 3 vertices (36 bytes)
        ByteBuffer rawBuffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        rawBuffer.putFloat(10.0f).putFloat(20.0f).putFloat(30.0f);
        rawBuffer.putFloat(40.0f).putFloat(50.0f).putFloat(60.0f);
        rawBuffer.putFloat(70.0f).putFloat(80.0f).putFloat(90.0f);
        byte[] binData = rawBuffer.array();

        String gltfJson = "{\n" +
                "  \"asset\": { \"version\": \"2.0\" },\n" +
                "  \"buffers\": [ { \"byteLength\": 36 } ],\n" +
                "  \"bufferViews\": [ { \"buffer\": 0, \"byteOffset\": 0, \"byteLength\": 36 } ],\n" +
                "  \"accessors\": [ { \"bufferView\": 0, \"byteOffset\": 0, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC3\" } ],\n" +
                "  \"meshes\": [ { \"name\": \"MovelGLB\", \"primitives\": [ { \"attributes\": { \"POSITION\": 0 } } ] } ]\n" +
                "}";
        byte[] jsonBytes = gltfJson.getBytes(StandardCharsets.UTF_8);

        // Monta o container binario GLB
        ByteArrayOutputStream glbStream = new ByteArrayOutputStream();
        ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        int totalLength = 12 + 8 + jsonBytes.length + 8 + binData.length;
        header.putInt(0x46546C67); // magic 'glTF'
        header.putInt(2);          // version
        header.putInt(totalLength);
        glbStream.write(header.array());

        // Chunk 0: JSON
        ByteBuffer chunk0Header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        chunk0Header.putInt(jsonBytes.length);
        chunk0Header.putInt(0x4E4F534A); // 'JSON'
        glbStream.write(chunk0Header.array());
        glbStream.write(jsonBytes);

        // Chunk 1: BIN
        ByteBuffer chunk1Header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        chunk1Header.putInt(binData.length);
        chunk1Header.putInt(0x004E4942); // 'BIN\0'
        glbStream.write(chunk1Header.array());
        glbStream.write(binData);

        byte[] glbBytes = glbStream.toByteArray();

        GltfParser parser = new GltfParser();
        List<MeshData> meshes = parser.parse(glbBytes);

        assertNotNull("Malhas extraidas do GLB nao devem ser nulas", meshes);
        assertEquals(1, meshes.size());

        MeshData mesh = meshes.get(0);
        assertEquals("MovelGLB", mesh.getName());
        assertEquals(3, mesh.getVertexCount());
        assertEquals(10.0f, mesh.getPositions()[0], 0.001f);
        assertEquals(50.0f, mesh.getPositions()[4], 0.001f);
        assertEquals(90.0f, mesh.getPositions()[8], 0.001f);
    }
}

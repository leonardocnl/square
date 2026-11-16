package br.com.square.io.gltf;

import br.com.square.mesh.MeshData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parser de modelos 3D no padrao glTF 2.0 (suportando arquivos .gltf e .glb).
 */
public class GltfParser {

    private static final int GLB_MAGIC = 0x46546C67; // 'glTF' em little-endian
    private static final int CHUNK_TYPE_JSON = 0x4E4F534A; // 'JSON'
    private static final int CHUNK_TYPE_BIN = 0x004E4942; // 'BIN\0'

    public List<MeshData> parse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        byte[] bytes = buffer.toByteArray();
        return parse(bytes);
    }

    @SuppressWarnings("unchecked")
    public List<MeshData> parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 4) {
            throw new IOException("Dados glTF vazios ou invalidos");
        }

        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int magic = bb.getInt(0);

        String jsonString;
        List<byte[]> binaryBuffers = new ArrayList<byte[]>();

        if (magic == GLB_MAGIC) {
            // Formato binario GLB
            if (bytes.length < 12) {
                throw new IOException("Cabecalho GLB corrompido");
            }
            int version = bb.getInt(4);
            int totalLength = bb.getInt(8);
            if (version != 2) {
                throw new IOException("Apenas glTF versao 2.0 e suportado (encontrado: " + version + ")");
            }

            int offset = 12;
            byte[] jsonBytes = null;

            while (offset < totalLength && offset + 8 <= bytes.length) {
                int chunkLength = bb.getInt(offset);
                int chunkType = bb.getInt(offset + 4);
                offset += 8;

                if (chunkType == CHUNK_TYPE_JSON) {
                    jsonBytes = new byte[chunkLength];
                    System.arraycopy(bytes, offset, jsonBytes, 0, chunkLength);
                } else if (chunkType == CHUNK_TYPE_BIN) {
                    byte[] binChunk = new byte[chunkLength];
                    System.arraycopy(bytes, offset, binChunk, 0, chunkLength);
                    binaryBuffers.add(binChunk);
                }
                offset += chunkLength;
            }

            if (jsonBytes == null) {
                throw new IOException("Chunk JSON nao encontrado no arquivo GLB");
            }
            jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
        } else {
            // Formato ASCII .gltf
            jsonString = new String(bytes, StandardCharsets.UTF_8);
        }

        Object rootObj = SimpleJson.parse(jsonString);
        if (!(rootObj instanceof Map)) {
            throw new IOException("Estrutura raiz do glTF deve ser um objeto JSON");
        }
        Map<String, Object> root = (Map<String, Object>) rootObj;

        // Processa buffers adicionais embutidos em data URIs base64
        List<Object> buffersList = (List<Object>) root.get("buffers");
        if (buffersList != null) {
            for (int i = binaryBuffers.size(); i < buffersList.size(); i++) {
                Map<String, Object> bufMap = (Map<String, Object>) buffersList.get(i);
                if (bufMap != null && bufMap.get("uri") instanceof String) {
                    String uri = (String) bufMap.get("uri");
                    if (uri.startsWith("data:")) {
                        int commaIdx = uri.indexOf(',');
                        if (commaIdx != -1) {
                            String base64Data = uri.substring(commaIdx + 1);
                            byte[] decoded = Base64.getDecoder().decode(base64Data);
                            binaryBuffers.add(decoded);
                        }
                    }
                }
            }
        }

        List<Object> bufferViews = (List<Object>) root.get("bufferViews");
        List<Object> accessors = (List<Object>) root.get("accessors");
        List<Object> meshes = (List<Object>) root.get("meshes");
        List<Object> materials = (List<Object>) root.get("materials");

        if (meshes == null || meshes.isEmpty()) {
            return Collections.emptyList();
        }

        List<MeshData> resultMeshes = new ArrayList<MeshData>();

        for (Object meshObj : meshes) {
            if (!(meshObj instanceof Map)) continue;
            Map<String, Object> meshMap = (Map<String, Object>) meshObj;
            String meshName = (String) meshMap.get("name");

            List<Object> primitives = (List<Object>) meshMap.get("primitives");
            if (primitives == null) continue;

            for (Object primObj : primitives) {
                if (!(primObj instanceof Map)) continue;
                Map<String, Object> primMap = (Map<String, Object>) primObj;

                Map<String, Object> attributes = (Map<String, Object>) primMap.get("attributes");
                if (attributes == null || !attributes.containsKey("POSITION")) continue;

                int posAccessorIdx = ((Number) attributes.get("POSITION")).intValue();
                float[] positions = readFloatAccessor(posAccessorIdx, accessors, bufferViews, binaryBuffers, 3);

                float[] normals = null;
                if (attributes.containsKey("NORMAL")) {
                    int normAccessorIdx = ((Number) attributes.get("NORMAL")).intValue();
                    normals = readFloatAccessor(normAccessorIdx, accessors, bufferViews, binaryBuffers, 3);
                }

                float[] texCoords = null;
                if (attributes.containsKey("TEXCOORD_0")) {
                    int texAccessorIdx = ((Number) attributes.get("TEXCOORD_0")).intValue();
                    texCoords = readFloatAccessor(texAccessorIdx, accessors, bufferViews, binaryBuffers, 2);
                }

                int[] indices = null;
                if (primMap.containsKey("indices")) {
                    int indAccessorIdx = ((Number) primMap.get("indices")).intValue();
                    indices = readIndexAccessor(indAccessorIdx, accessors, bufferViews, binaryBuffers);
                }

                MeshData meshData = new MeshData(meshName, positions, normals, texCoords, indices);

                // Aplica material se presente
                if (primMap.containsKey("material") && materials != null) {
                    int matIdx = ((Number) primMap.get("material")).intValue();
                    if (matIdx >= 0 && matIdx < materials.size()) {
                        Map<String, Object> matMap = (Map<String, Object>) materials.get(matIdx);
                        if (matMap != null && matMap.containsKey("pbrMetallicRoughness")) {
                            Map<String, Object> pbr = (Map<String, Object>) matMap.get("pbrMetallicRoughness");
                            if (pbr.containsKey("baseColorFactor")) {
                                List<Object> colList = (List<Object>) pbr.get("baseColorFactor");
                                if (colList != null && colList.size() >= 4) {
                                    float r = ((Number) colList.get(0)).floatValue();
                                    float g = ((Number) colList.get(1)).floatValue();
                                    float b = ((Number) colList.get(2)).floatValue();
                                    float a = ((Number) colList.get(3)).floatValue();
                                    meshData.setBaseColor(r, g, b, a);
                                }
                            }
                            if (pbr.containsKey("metallicFactor")) {
                                meshData.setMetallicFactor(((Number) pbr.get("metallicFactor")).floatValue());
                            }
                            if (pbr.containsKey("roughnessFactor")) {
                                meshData.setRoughnessFactor(((Number) pbr.get("roughnessFactor")).floatValue());
                            }
                        }
                    }
                }

                meshData.computeNormalsIfNeeded();
                resultMeshes.add(meshData);
            }
        }

        return resultMeshes;
    }

    @SuppressWarnings("unchecked")
    private float[] readFloatAccessor(int accessorIndex, List<Object> accessors,
                                      List<Object> bufferViews, List<byte[]> binaryBuffers, int componentCount) {
        if (accessorIndex < 0 || accessorIndex >= accessors.size()) return new float[0];
        Map<String, Object> accessor = (Map<String, Object>) accessors.get(accessorIndex);

        int bufferViewIdx = ((Number) accessor.get("bufferView")).intValue();
        int byteOffset = accessor.containsKey("byteOffset") ? ((Number) accessor.get("byteOffset")).intValue() : 0;
        int count = ((Number) accessor.get("count")).intValue();

        Map<String, Object> bufferView = (Map<String, Object>) bufferViews.get(bufferViewIdx);
        int bvBufferIdx = ((Number) bufferView.get("buffer")).intValue();
        int bvByteOffset = bufferView.containsKey("byteOffset") ? ((Number) bufferView.get("byteOffset")).intValue() : 0;

        if (bvBufferIdx < 0 || bvBufferIdx >= binaryBuffers.size()) return new float[0];
        byte[] rawBuffer = binaryBuffers.get(bvBufferIdx);

        int totalOffset = bvByteOffset + byteOffset;
        ByteBuffer bb = ByteBuffer.wrap(rawBuffer).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(totalOffset);

        float[] result = new float[count * componentCount];
        for (int i = 0; i < result.length; i++) {
            if (bb.remaining() >= 4) {
                result[i] = bb.getFloat();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private int[] readIndexAccessor(int accessorIndex, List<Object> accessors,
                                    List<Object> bufferViews, List<byte[]> binaryBuffers) {
        if (accessorIndex < 0 || accessorIndex >= accessors.size()) return new int[0];
        Map<String, Object> accessor = (Map<String, Object>) accessors.get(accessorIndex);

        int bufferViewIdx = ((Number) accessor.get("bufferView")).intValue();
        int byteOffset = accessor.containsKey("byteOffset") ? ((Number) accessor.get("byteOffset")).intValue() : 0;
        int count = ((Number) accessor.get("count")).intValue();
        int componentType = ((Number) accessor.get("componentType")).intValue();

        Map<String, Object> bufferView = (Map<String, Object>) bufferViews.get(bufferViewIdx);
        int bvBufferIdx = ((Number) bufferView.get("buffer")).intValue();
        int bvByteOffset = bufferView.containsKey("byteOffset") ? ((Number) bufferView.get("byteOffset")).intValue() : 0;

        if (bvBufferIdx < 0 || bvBufferIdx >= binaryBuffers.size()) return new int[0];
        byte[] rawBuffer = binaryBuffers.get(bvBufferIdx);

        int totalOffset = bvByteOffset + byteOffset;
        ByteBuffer bb = ByteBuffer.wrap(rawBuffer).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(totalOffset);

        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            if (componentType == 5121) { // UNSIGNED_BYTE
                indices[i] = bb.get() & 0xFF;
            } else if (componentType == 5123) { // UNSIGNED_SHORT
                indices[i] = bb.getShort() & 0xFFFF;
            } else if (componentType == 5125) { // UNSIGNED_INT
                indices[i] = bb.getInt();
            } else {
                indices[i] = bb.getShort() & 0xFFFF;
            }
        }
        return indices;
    }
}


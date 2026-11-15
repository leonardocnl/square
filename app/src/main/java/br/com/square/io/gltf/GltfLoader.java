package br.com.square.io.gltf;

import br.com.square.mesh.MeshData;
import br.com.square.mesh.MeshGeometryBridge;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Carregador de modelos 3D no formato glTF 2.0 (.gltf e .glb) compativel com a interface Loader do Java 3D.
 */
public class GltfLoader extends LoaderBase implements Loader {

    private final GltfParser parser = new GltfParser();

    public GltfLoader() {
    }

    @Override
    public Scene load(String filename) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("Arquivo nao encontrado: " + filename);
        }
        try (InputStream in = new FileInputStream(file)) {
            return loadFromStream(in);
        } catch (IOException e) {
            throw new ParsingErrorException("Erro ao ler arquivo glTF: " + e.getMessage());
        }
    }

    @Override
    public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        try (InputStream in = url.openStream()) {
            return loadFromStream(in);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new ParsingErrorException("Erro ao abrir URL glTF: " + e.getMessage());
        }
    }

    @Override
    public Scene load(Reader reader) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            return loadFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new ParsingErrorException("Erro ao ler stream glTF: " + e.getMessage());
        }
    }

    public Scene loadFromStream(InputStream in) throws IOException {
        List<MeshData> meshes = parser.parse(in);
        if (meshes.isEmpty()) {
            throw new IncorrectFormatException("Nenhuma geometria encontrada no modelo glTF");
        }

        BranchGroup root = new BranchGroup();
        SceneBase scene = new SceneBase();

        for (MeshData mesh : meshes) {
            Shape3D shape = MeshGeometryBridge.createShape3D(mesh);
            root.addChild(shape);
            if (mesh.getName() != null) {
                scene.addNamedObject(mesh.getName(), shape);
            }
        }

        scene.setSceneGroup(root);
        return scene;
    }
}

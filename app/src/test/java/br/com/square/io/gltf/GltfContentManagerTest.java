package br.com.square.io.gltf;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.viewcontroller.ContentManager.ContentType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes para validacao de aceitacao de modelos glTF e GLB no FileContentManager.
 */
public class GltfContentManagerTest {

    @Test
    public void testGltfAndGlbAcceptanceInContentManager() {
        FileContentManager manager = new FileContentManager(new DefaultUserPreferences());

        // Modelos glTF e GLB devem ser aceitos como ContentType.MODEL
        assertTrue("Arquivo .gltf deve ser aceito como modelo",
                manager.isAcceptable("/path/to/furniture.gltf", ContentType.MODEL));
        assertTrue("Arquivo .glb deve ser aceito como modelo",
                manager.isAcceptable("/path/to/furniture.glb", ContentType.MODEL));
        assertTrue("Arquivo .obj legado deve continuar sendo aceito",
                manager.isAcceptable("/path/to/furniture.obj", ContentType.MODEL));

        // Formatos nao compativeis como modelo 3D devem ser rejeitados
        assertFalse("Arquivo .pdf nao deve ser aceito como modelo",
                manager.isAcceptable("/path/to/document.pdf", ContentType.MODEL));
        assertFalse("Arquivo .txt nao deve ser aceito como modelo",
                manager.isAcceptable("/path/to/notes.txt", ContentType.MODEL));
    }
}

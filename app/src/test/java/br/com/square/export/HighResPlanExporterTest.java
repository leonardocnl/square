package br.com.square.export;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o exportador de alta resolucao HighResPlanExporter.
 */
public class HighResPlanExporterTest {

    private File tempPngFile;

    @Before
    public void setUp() throws IOException {
        tempPngFile = File.createTempFile("highres_plan_", ".png");
    }

    @After
    public void tearDown() {
        if (tempPngFile != null && tempPngFile.exists()) {
            tempPngFile.delete();
        }
    }

    @Test
    public void testHighResPlanRenderingAndExport() throws IOException {
        Home home = new Home();

        Wall w1 = new Wall(0f, 0f, 400f, 0f, 15f, 250f);
        Wall w2 = new Wall(400f, 0f, 400f, 300f, 15f, 250f);
        home.addWall(w1);
        home.addWall(w2);

        Room room = new Room(new float[][]{{0f, 0f}, {400f, 0f}, {400f, 300f}, {0f, 300f}});
        home.addRoom(room);

        CatalogPieceOfFurniture catPiece = new CatalogPieceOfFurniture("Mesa", null, null, 120f, 80f, 75f, true, false);
        HomePieceOfFurniture piece = new HomePieceOfFurniture(catPiece);
        piece.setX(200f);
        piece.setY(150f);
        home.addPieceOfFurniture(piece);

        int targetWidth = 1920;
        int targetHeight = 1080;

        BufferedImage image = HighResPlanExporter.renderToImage(home, targetWidth, targetHeight);

        assertNotNull("Imagem gerada nao deve ser nula", image);
        assertEquals(targetWidth, image.getWidth());
        assertEquals(targetHeight, image.getHeight());

        // Exportacao para arquivo
        HighResPlanExporter.exportToFile(home, targetWidth, targetHeight, tempPngFile, "PNG");
        assertTrue("Arquivo PNG deve existir", tempPngFile.exists());
        assertTrue("Arquivo PNG deve ter tamanho positivo", tempPngFile.length() > 0);
    }
}

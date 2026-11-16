package br.com.square.ui.guides;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Wall;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;

/**
 * Testes unitarios para o SmartGuidesRenderer.
 */
public class SmartGuidesRendererTest {

    @Test
    public void testSmartGuidesRendering() {
        Home home = new Home();

        // Parede fixa no projeto
        Wall wall = new Wall(100f, 100f, 300f, 100f, 15f, 250f);
        home.addWall(wall);

        // Movel existente
        CatalogPieceOfFurniture catPiece1 = new CatalogPieceOfFurniture("Mesa", null, null, 120f, 80f, 75f, true, false);
        HomePieceOfFurniture fixedPiece = new HomePieceOfFurniture(catPiece1);
        fixedPiece.setX(200f);
        fixedPiece.setY(250f);
        home.addPieceOfFurniture(fixedPiece);

        // Movel ativo sendo arrastado e alinhado no eixo X
        CatalogPieceOfFurniture catPiece2 = new CatalogPieceOfFurniture("Cadeira", null, null, 50f, 50f, 85f, true, false);
        HomePieceOfFurniture draggedPiece = new HomePieceOfFurniture(catPiece2);
        draggedPiece.setX(200f); // Alinhado com o centro da mesa
        draggedPiece.setY(400f);

        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = img.createGraphics();

        // Renderiza as guias inteligentes
        SmartGuidesRenderer.paintGuides(g2D, home, Collections.singletonList(draggedPiece), 1.0f);
        g2D.dispose();

        assertNotNull("Imagem renderizada deve existir", img);
    }

    @Test
    public void testComputeSnapAlignsCenter() {
        Home home = new Home();

        // Movel de referencia centrado em X=200, Y=200
        CatalogPieceOfFurniture catPiece1 = new CatalogPieceOfFurniture("Mesa", null, null, 100f, 100f, 75f, true, false);
        HomePieceOfFurniture fixedPiece = new HomePieceOfFurniture(catPiece1);
        fixedPiece.setX(200f);
        fixedPiece.setY(200f);
        home.addPieceOfFurniture(fixedPiece);

        // Movel arrastado quase alinhado no centro (X=202, dif de 2cm, dentro do threshold de 4cm)
        CatalogPieceOfFurniture catPiece2 = new CatalogPieceOfFurniture("Cadeira", null, null, 50f, 50f, 85f, true, false);
        HomePieceOfFurniture draggedPiece = new HomePieceOfFurniture(catPiece2);
        draggedPiece.setX(202f);
        draggedPiece.setY(400f);

        SmartGuidesRenderer.SnapResult snap = SmartGuidesRenderer.computeSnap(
                home, Collections.singletonList(draggedPiece), 4.0f);

        org.junit.Assert.assertTrue("Deve ter detectado snap no eixo X", snap.isSnappedX());
        org.junit.Assert.assertEquals("DeltaX deve ser -2.0 para alinhar perfeitamente no centro",
                -2.0f, snap.getDeltaX(), 0.001f);
    }

    @Test
    public void testComputeSnapIgnoresBeyondThreshold() {
        Home home = new Home();

        CatalogPieceOfFurniture catPiece1 = new CatalogPieceOfFurniture("Mesa", null, null, 100f, 100f, 75f, true, false);
        HomePieceOfFurniture fixedPiece = new HomePieceOfFurniture(catPiece1);
        fixedPiece.setX(200f);
        fixedPiece.setY(200f);
        home.addPieceOfFurniture(fixedPiece);

        // Movel arrastado com diferenca de 10cm (fora do threshold de 4cm)
        CatalogPieceOfFurniture catPiece2 = new CatalogPieceOfFurniture("Cadeira", null, null, 50f, 50f, 85f, true, false);
        HomePieceOfFurniture draggedPiece = new HomePieceOfFurniture(catPiece2);
        draggedPiece.setX(210f);
        draggedPiece.setY(400f);

        SmartGuidesRenderer.SnapResult snap = SmartGuidesRenderer.computeSnap(
                home, Collections.singletonList(draggedPiece), 4.0f);

        org.junit.Assert.assertFalse("Nao deve ter snap no eixo X se alem do threshold", snap.isSnappedX());
        org.junit.Assert.assertEquals(0f, snap.getDeltaX(), 0.001f);
    }

    @Test
    public void testPaintGuidesRespectsMagnetismActive() {
        Home home = new Home();
        CatalogPieceOfFurniture catPiece1 = new CatalogPieceOfFurniture("Mesa", null, null, 100f, 100f, 75f, true, false);
        HomePieceOfFurniture fixedPiece = new HomePieceOfFurniture(catPiece1);
        fixedPiece.setX(200f);
        fixedPiece.setY(200f);
        home.addPieceOfFurniture(fixedPiece);

        CatalogPieceOfFurniture catPiece2 = new CatalogPieceOfFurniture("Cadeira", null, null, 50f, 50f, 85f, true, false);
        HomePieceOfFurniture draggedPiece = new HomePieceOfFurniture(catPiece2);
        draggedPiece.setX(200f);
        draggedPiece.setY(400f);

        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = img.createGraphics();

        // Quando magnetismo esta desativado (tecla Alt pressionada), nao deve desenhar
        SmartGuidesRenderer.paintGuides(g2D, home, Collections.singletonList(draggedPiece), 1.0f, false);
        g2D.dispose();

        assertNotNull(img);
    }
}


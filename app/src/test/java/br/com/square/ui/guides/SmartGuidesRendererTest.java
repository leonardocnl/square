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
}

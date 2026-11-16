package br.com.square.export;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o exportador vetorial SvgPlanExporter.
 */
public class SvgPlanExporterTest {

    private File tempSvgFile;

    @Before
    public void setUp() throws IOException {
        tempSvgFile = File.createTempFile("plan_export_", ".svg");
    }

    @After
    public void tearDown() {
        if (tempSvgFile != null && tempSvgFile.exists()) {
            tempSvgFile.delete();
        }
    }

    @Test
    public void testSvgExportGeneration() throws IOException {
        Home home = new Home();

        // Paredes
        Wall w1 = new Wall(0f, 0f, 400f, 0f, 15f, 250f);
        Wall w2 = new Wall(400f, 0f, 400f, 300f, 15f, 250f);
        home.addWall(w1);
        home.addWall(w2);

        // Comodo com area
        Room room = new Room(new float[][]{{0f, 0f}, {400f, 0f}, {400f, 300f}, {0f, 300f}});
        room.setName("Sala de Estar");
        home.addRoom(room);

        // Movel
        CatalogPieceOfFurniture catPiece = new CatalogPieceOfFurniture("Sofa 3 Lugares", null, null, 200f, 90f, 85f, true, false);
        HomePieceOfFurniture piece = new HomePieceOfFurniture(catPiece);
        piece.setX(150f);
        piece.setY(100f);
        home.addPieceOfFurniture(piece);

        // Cota
        DimensionLine dim = new DimensionLine(0f, -30f, 400f, -30f, 0f);
        home.addDimensionLine(dim);

        // Anotacao
        Label label = new Label("Piso Porcelanato", 200f, 150f);
        home.addLabel(label);

        // Execucao da exportacao para string
        String svg = SvgPlanExporter.exportToSvg(home);

        assertNotNull("Conteudo SVG nao deve ser nulo", svg);
        assertTrue("Deve comecar com declaracao XML", svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue("Deve conter elemento raiz <svg", svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""));
        assertTrue("Deve fechar tag </svg>", svg.endsWith("</svg>\n") || svg.endsWith("</svg>"));

        // Validacao das camadas estruturadas
        assertTrue("Deve conter camada de comodos", svg.contains("<g id=\"rooms\">"));
        assertTrue("Deve conter o nome do comodo", svg.contains("Sala de Estar"));
        assertTrue("Deve conter camada de paredes", svg.contains("<g id=\"walls\">"));
        assertTrue("Deve conter camada de moveis", svg.contains("<g id=\"furniture\">"));
        assertTrue("Deve conter o nome do movel", svg.contains("Sofa 3 Lugares"));
        assertTrue("Deve conter camada de cotas", svg.contains("<g id=\"dimensionLines\">"));
        assertTrue("Deve conter camada de anotacoes", svg.contains("<g id=\"labels\">"));
        assertTrue("Deve conter texto da anotacao", svg.contains("Piso Porcelanato"));

        // Ausencia de valores invalidos
        assertFalse("Nao deve conter NaN", svg.contains("NaN"));
        assertFalse("Nao deve conter Infinity", svg.contains("Infinity"));

        // Gravacao em arquivo
        SvgPlanExporter.exportToFile(home, tempSvgFile);
        assertTrue("Arquivo SVG deve ter sido gravado", tempSvgFile.exists());
        assertTrue("Arquivo SVG deve ter tamanho positivo", tempSvgFile.length() > 0);
    }
}

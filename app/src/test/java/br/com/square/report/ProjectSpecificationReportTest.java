package br.com.square.report;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o gerador de Memorial Descritivo e Quantitativos (ProjectSpecificationReport).
 */
public class ProjectSpecificationReportTest {

    private File tempHtmlFile;
    private File tempCsvFile;

    @Before
    public void setUp() throws IOException {
        tempHtmlFile = File.createTempFile("report_", ".html");
        tempCsvFile = File.createTempFile("report_", ".csv");
    }

    @After
    public void tearDown() {
        if (tempHtmlFile != null && tempHtmlFile.exists()) {
            tempHtmlFile.delete();
        }
        if (tempCsvFile != null && tempCsvFile.exists()) {
            tempCsvFile.delete();
        }
    }

    @Test
    public void testReportDataGenerationAndRoomAssignment() throws IOException {
        Home home = new Home();

        // 2 paredes (400 cm e 300 cm, altura 250 cm)
        Wall w1 = new Wall(0f, 0f, 400f, 0f, 15f, 250f);
        Wall w2 = new Wall(400f, 0f, 400f, 300f, 15f, 250f);
        home.addWall(w1);
        home.addWall(w2);

        // 1 comodo (400x300 cm = 12 m2)
        Room room = new Room(new float[][]{{0f, 0f}, {400f, 0f}, {400f, 300f}, {0f, 300f}});
        room.setName("Sala Principal");
        home.addRoom(room);

        // 1 movel posicionado DENTRO da Sala Principal (x=150, y=100)
        CatalogPieceOfFurniture catFurniture = new CatalogPieceOfFurniture("Sofa Retratil", null, null,
                220f, 95f, 90f, true, false);
        HomePieceOfFurniture sofa = new HomePieceOfFurniture(catFurniture);
        sofa.setX(150f);
        sofa.setY(100f);
        sofa.setPrice(BigDecimal.valueOf(2500.00));
        home.addPieceOfFurniture(sofa);

        // 1 esquadria (Porta)
        com.eteks.sweethome3d.model.CatalogDoorOrWindow catDoor = new com.eteks.sweethome3d.model.CatalogDoorOrWindow(
                null, "Porta Madeira", null, null, null, 80f, 10f, 210f, 0f, false,
                15f, 0f, new com.eteks.sweethome3d.model.Sash[0], com.eteks.sweethome3d.model.PieceOfFurniture.IDENTITY_ROTATION,
                null, true, BigDecimal.valueOf(650.00), BigDecimal.ZERO);
        com.eteks.sweethome3d.model.HomeDoorOrWindow door = new com.eteks.sweethome3d.model.HomeDoorOrWindow(catDoor);
        door.setX(200f);
        door.setY(0f);
        door.setElevation(0f);
        home.addPieceOfFurniture(door);

        ProjectSpecificationReport.ReportData data = ProjectSpecificationReport.generateReportData(home);

        assertNotNull(data);
        assertEquals(12.0, data.getTotalAreaM2(), 0.01);
        assertEquals(7.0, data.getTotalWallLengthM(), 0.01);
        assertEquals(35.0, data.getTotalWallSurfaceAreaM2(), 0.01); // 7m * 2.5m * 2 faces
        assertEquals(1, data.getTotalFurnitureCount());
        assertEquals(1, data.getTotalDoorsAndWindowsCount());
        assertEquals(BigDecimal.valueOf(3150.00), data.getTotalCost());

        assertEquals(1, data.getRooms().size());
        assertEquals("Sala Principal", data.getRooms().get(0).getName());

        assertEquals(1, data.getFurnitureList().size());
        assertEquals("Sofa Retratil", data.getFurnitureList().get(0).getName());
        assertEquals("Sala Principal", data.getFurnitureList().get(0).getRoomName());

        // Validacao de geracao HTML
        String html = ProjectSpecificationReport.generateHtmlReport(home);
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Memorial Descritivo e Quantitativo Executivo"));
        assertTrue(html.contains("Sala Principal"));
        assertTrue(html.contains("Sofa Retratil"));
        assertTrue(html.contains("Porta Madeira"));

        ProjectSpecificationReport.exportHtmlToFile(home, tempHtmlFile);
        assertTrue(tempHtmlFile.exists());
        assertTrue(tempHtmlFile.length() > 0);

        // Validacao de geracao CSV
        String csv = ProjectSpecificationReport.generateCsvReport(home);
        assertNotNull(csv);
        assertTrue(csv.contains("TIPO;ITEM;COMODO"));
        assertTrue(csv.contains("COMODO;Sala Principal"));
        assertTrue(csv.contains("MOVEL;Sofa Retratil"));

        ProjectSpecificationReport.exportCsvToFile(home, tempCsvFile);
        assertTrue(tempCsvFile.exists());
        assertTrue(tempCsvFile.length() > 0);
    }
}

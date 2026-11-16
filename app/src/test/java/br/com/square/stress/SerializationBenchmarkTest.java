package br.com.square.stress;

import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Benchmark e teste de estresse de serializacao e desserializacao (.sh3d)
 * para projetos de grande escala sob o formato binario compativel com o SH3D 7.5.
 */
public class SerializationBenchmarkTest {

    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("large_home_stress_", ".sh3d");
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testSerializationAndDeserializationLargeHome() throws Exception {
        int wallCount = 500;
        int furnitureCount = 1000;
        int roomCount = 100;

        Home originalHome = LargeHomeStressTest.createLargeHome(wallCount, furnitureCount, roomCount);

        HomeFileRecorder recorder = new HomeFileRecorder(0, false, null, false, true);

        // 1. Medicao de tempo de escrita
        long writeStart = System.currentTimeMillis();
        recorder.writeHome(originalHome, tempFile.getAbsolutePath());
        long writeDuration = System.currentTimeMillis() - writeStart;

        assertTrue("Arquivo .sh3d deve ser gravado no disco", tempFile.exists());
        long fileSize = tempFile.length();
        assertTrue("Arquivo .sh3d deve possuir tamanho positivo", fileSize > 0);

        // Limite de tempo de gravacao para 1600 elementos: < 3500 ms
        assertTrue("Gravacao do arquivo massivo deve durar menos de 3500 ms (durou: " + writeDuration + " ms)",
                writeDuration < 3500);

        // 2. Medicao de tempo de leitura
        long readStart = System.currentTimeMillis();
        Home loadedHome = recorder.readHome(tempFile.getAbsolutePath());
        long readDuration = System.currentTimeMillis() - readStart;

        assertNotNull("Objeto Home desserializado nao pode ser nulo", loadedHome);

        // Limite de tempo de leitura para 1600 elementos: < 3500 ms
        assertTrue("Leitura do arquivo massivo deve durar menos de 3500 ms (durou: " + readDuration + " ms)",
                readDuration < 3500);

        // 3. Verificacao estrita de paridade
        assertEquals("Total de paredes carregadas deve ser identico ao original",
                originalHome.getWalls().size(), loadedHome.getWalls().size());
        assertEquals("Total de moveis carregados deve ser identico ao original",
                originalHome.getFurniture().size(), loadedHome.getFurniture().size());
        assertEquals("Total de comodos carregados deve ser identico ao original",
                originalHome.getRooms().size(), loadedHome.getRooms().size());

        // Verificacao de amostra de coordenadas e areas
        Wall origFirstWall = originalHome.getWalls().iterator().next();
        Wall loadedFirstWall = loadedHome.getWalls().iterator().next();
        assertEquals(origFirstWall.getXStart(), loadedFirstWall.getXStart(), 0.001f);
        assertEquals(origFirstWall.getYStart(), loadedFirstWall.getYStart(), 0.001f);
        assertEquals(origFirstWall.getXEnd(), loadedFirstWall.getXEnd(), 0.001f);
        assertEquals(origFirstWall.getYEnd(), loadedFirstWall.getYEnd(), 0.001f);

        HomePieceOfFurniture origFirstPiece = originalHome.getFurniture().get(0);
        HomePieceOfFurniture loadedFirstPiece = loadedHome.getFurniture().get(0);
        assertEquals(origFirstPiece.getName(), loadedFirstPiece.getName());
        assertEquals(origFirstPiece.getX(), loadedFirstPiece.getX(), 0.001f);
        assertEquals(origFirstPiece.getY(), loadedFirstPiece.getY(), 0.001f);

        Room origFirstRoom = originalHome.getRooms().get(0);
        Room loadedFirstRoom = loadedHome.getRooms().get(0);
        assertEquals(origFirstRoom.getName(), loadedFirstRoom.getName());
        assertEquals(origFirstRoom.getArea(), loadedFirstRoom.getArea(), 0.01f);
    }
}

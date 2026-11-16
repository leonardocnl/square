package br.com.square.stress;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste de estresse para geracao, conexao geometrica e manipulacao
 * de plantas de grande escala (500 paredes, 1.000 moveis, 100 comodos).
 */
public class LargeHomeStressTest {

    public static Home createLargeHome(int targetWalls, int targetFurniture, int targetRooms) {
        Home home = new Home();

        // 1. Criacao de malha ortogonal de paredes conectadas
        // Malha de tamanho gridCols x gridRows com paredes horizontais e verticais
        int gridDim = (int) Math.ceil(Math.sqrt(targetWalls / 2.0));
        float cellSize = 300.0f; // 3 metros por celula
        float wallThickness = 15.0f;
        float wallHeight = 250.0f;

        List<Wall> createdWalls = new ArrayList<>();

        // Paredes horizontais
        for (int r = 0; r <= gridDim && createdWalls.size() < targetWalls; r++) {
            for (int c = 0; c < gridDim && createdWalls.size() < targetWalls; c++) {
                float x1 = c * cellSize;
                float y1 = r * cellSize;
                float x2 = (c + 1) * cellSize;
                float y2 = y1;
                Wall w = new Wall(x1, y1, x2, y2, wallThickness, wallHeight);
                home.addWall(w);
                createdWalls.add(w);
            }
        }

        // Paredes verticais
        for (int c = 0; c <= gridDim && createdWalls.size() < targetWalls; c++) {
            for (int r = 0; r < gridDim && createdWalls.size() < targetWalls; r++) {
                float x1 = c * cellSize;
                float y1 = r * cellSize;
                float x2 = x1;
                float y2 = (r + 1) * cellSize;
                Wall w = new Wall(x1, y1, x2, y2, wallThickness, wallHeight);
                home.addWall(w);
                createdWalls.add(w);
            }
        }

        // Se ainda faltar para atingir targetWalls, completa sequencialmente
        int seqIndex = 0;
        while (createdWalls.size() < targetWalls) {
            float startX = seqIndex * cellSize;
            float endX = (seqIndex + 1) * cellSize;
            Wall extra = new Wall(startX, -100.0f, endX, -100.0f, wallThickness, wallHeight);
            home.addWall(extra);
            createdWalls.add(extra);
            seqIndex++;
        }

        // 2. Criacao de comodos fechados
        int roomsAdded = 0;
        for (int r = 0; r < gridDim && roomsAdded < targetRooms; r++) {
            for (int c = 0; c < gridDim && roomsAdded < targetRooms; c++) {
                float minX = c * cellSize;
                float minY = r * cellSize;
                float maxX = (c + 1) * cellSize;
                float maxY = (r + 1) * cellSize;

                float[][] points = new float[][] {
                    {minX, minY},
                    {maxX, minY},
                    {maxX, maxY},
                    {minX, maxY}
                };
                Room room = new Room(points);
                room.setName("Comodo " + (roomsAdded + 1));
                home.addRoom(room);
                roomsAdded++;
            }
        }

        // 3. Criacao e distribuicao de moveis
        for (int i = 0; i < targetFurniture; i++) {
            CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
                    "Movel " + (i + 1),
                    null,
                    null,
                    60.0f,
                    60.0f,
                    80.0f,
                    true,
                    false
            );
            HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
            float posX = (i % (gridDim * 5)) * 60.0f;
            float posY = (i / (gridDim * 5)) * 60.0f;
            piece.setX(posX);
            piece.setY(posY);
            piece.setElevation(0.0f);
            piece.setAngle((float) ((i % 8) * (Math.PI / 4.0)));
            home.addPieceOfFurniture(piece);
        }

        return home;
    }

    @Test
    public void testLargeHomeCreationAndConsistency() {
        long startTime = System.currentTimeMillis();

        int targetWalls = 500;
        int targetFurniture = 1000;
        int targetRooms = 100;

        Home home = createLargeHome(targetWalls, targetFurniture, targetRooms);

        long duration = System.currentTimeMillis() - startTime;

        // Validacoes de contagem
        assertEquals("Contagem de paredes deve atingir 500", targetWalls, home.getWalls().size());
        assertEquals("Contagem de moveis deve atingir 1000", targetFurniture, home.getFurniture().size());
        assertEquals("Contagem de comodos deve atingir 100", targetRooms, home.getRooms().size());

        // Validacoes geometricas
        for (Room room : home.getRooms()) {
            float area = room.getArea();
            assertFalse("Area do comodo nao deve ser NaN", Float.isNaN(area));
            assertTrue("Area do comodo deve ser positiva", area > 0.0f);
        }

        // Validacao de desempenho: criacao massiva deve ser menor que 2.500 ms
        assertTrue("Criacao da planta massiva deve ser concluida em menos de 2500 ms (durou: " + duration + " ms)",
                duration < 2500);
    }
}

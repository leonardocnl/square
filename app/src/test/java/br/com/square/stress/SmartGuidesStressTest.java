package br.com.square.stress;

import br.com.square.ui.guides.SmartGuidesRenderer;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Selectable;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste de estresse e benchmark de desempenho para o algoritmo de Guias Inteligentes (Smart Guides)
 * e atracao magnetica (snapping) sob cenarios de altissima densidade de obstaculos vizinhos.
 */
public class SmartGuidesStressTest {

    @Test
    public void testSmartGuidesSnapPerformanceUnderHeavyLoad() {
        // Cria cenario denso com 500 paredes, 1.000 moveis e 100 comodos
        Home home = LargeHomeStressTest.createLargeHome(500, 1000, 100);

        HomePieceOfFurniture movingPiece = home.getFurniture().get(0);
        List<? extends Selectable> activeItems = Collections.singletonList(movingPiece);

        float thresholdCm = 4.0f;
        int iterations = 5000;

        // Warm-up da JVM
        for (int i = 0; i < 200; i++) {
            movingPiece.setX(100.0f + (i % 50));
            movingPiece.setY(100.0f + (i % 50));
            SmartGuidesRenderer.computeSnap(home, activeItems, thresholdCm);
        }

        long start = System.nanoTime();

        int snappedCount = 0;
        for (int i = 0; i < iterations; i++) {
            // Simula arraste continuo pela planta
            float testX = (i * 7.5f) % 3000.0f;
            float testY = (i * 11.2f) % 3000.0f;
            movingPiece.setX(testX);
            movingPiece.setY(testY);

            SmartGuidesRenderer.SnapResult result = SmartGuidesRenderer.computeSnap(home, activeItems, thresholdCm);
            assertNotNull(result);

            if (result.isSnappedX() || result.isSnappedY()) {
                snappedCount++;
                if (result.isSnappedX()) {
                    assertTrue("Delta X de snap deve respeitar o limiar", Math.abs(result.getDeltaX()) <= thresholdCm);
                }
                if (result.isSnappedY()) {
                    assertTrue("Delta Y de snap deve respeitar o limiar", Math.abs(result.getDeltaY()) <= thresholdCm);
                }
            }
        }

        long totalNanos = System.nanoTime() - start;
        double totalMillis = totalNanos / 1_000_000.0;
        double avgMillisPerCall = totalMillis / iterations;

        // Validacoes de desempenho:
        // Cada avaliacao de snap com centenas de vizinhos deve rodar em menos de 0.8 ms em media
        assertTrue("Tempo medio por calculo de snap deve ser menor que 0.8 ms (media: " + avgMillisPerCall + " ms)",
                avgMillisPerCall < 0.8);

        assertTrue("Snap deve ter sido ativado em pontos de aproximacao durante o teste", snappedCount > 0);
    }
}

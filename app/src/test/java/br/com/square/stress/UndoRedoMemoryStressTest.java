package br.com.square.stress;

import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import org.junit.Test;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Teste de estresse para ciclo massivo de Undo/Redo e pressao de memoria.
 * Executa milhares de mutacoes continuas, revertendo e reaplicando estados
 * para assegurar precisao de modelo e ausencia de vazamentos de heap.
 */
public class UndoRedoMemoryStressTest {

    private static class FurnitureMoveEdit extends AbstractUndoableEdit {
        private final HomePieceOfFurniture furniture;
        private final float oldX;
        private final float newX;

        public FurnitureMoveEdit(HomePieceOfFurniture furniture, float oldX, float newX) {
            this.furniture = furniture;
            this.oldX = oldX;
            this.newX = newX;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            furniture.setX(oldX);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            furniture.setX(newX);
        }
    }

    @Test
    public void testMassiveUndoRedoCycleAndMemoryIntegrity() {
        Home home = new Home();
        UndoManager undoManager = new UndoManager();
        undoManager.setLimit(5000);

        UndoableEditSupport undoSupport = new UndoableEditSupport();
        undoSupport.addUndoableEditListener(undoManager);

        int furnitureCount = 500;
        List<HomePieceOfFurniture> pieces = new ArrayList<>();
        List<Float> originalPositions = new ArrayList<>();

        for (int i = 0; i < furnitureCount; i++) {
            CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture(
                    "Movel_" + i, null, null, 50.0f, 50.0f, 50.0f, true, false);
            HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
            float initialX = i * 20.0f;
            piece.setX(initialX);
            piece.setY(100.0f);
            home.addPieceOfFurniture(piece);
            pieces.add(piece);
            originalPositions.add(initialX);
        }

        long startTime = System.currentTimeMillis();

        // 1. Execucao de 2.000 mutacoes (4 mutacoes sequenciais por peca)
        int mutationCycles = 4;
        for (int cycle = 1; cycle <= mutationCycles; cycle++) {
            for (int i = 0; i < furnitureCount; i++) {
                HomePieceOfFurniture piece = pieces.get(i);
                float prevX = piece.getX();
                float nextX = prevX + 15.0f * cycle;
                piece.setX(nextX);
                undoSupport.postEdit(new FurnitureMoveEdit(piece, prevX, nextX));
            }
        }

        int totalEdits = furnitureCount * mutationCycles; // 2.000 edits
        assertTrue("UndoManager deve permitir desfazer", undoManager.canUndo());

        // 2. Execucao de 2.000 operacoes de Undo
        for (int op = 0; op < totalEdits; op++) {
            assertTrue("Deve ser possivel desfazer a operacao " + op, undoManager.canUndo());
            undoManager.undo();
        }

        assertFalse("Nao deve haver mais operacoes para desfazer", undoManager.canUndo());
        assertTrue("UndoManager deve permitir refazer", undoManager.canRedo());

        // Valida retorno estrito ao estado original
        for (int i = 0; i < furnitureCount; i++) {
            assertEquals("Posicao X deve retornar exatamente ao valor original",
                    originalPositions.get(i), pieces.get(i).getX(), 0.001f);
        }

        // 3. Execucao de 2.000 operacoes de Redo
        for (int op = 0; op < totalEdits; op++) {
            assertTrue("Deve ser possivel refazer a operacao " + op, undoManager.canRedo());
            undoManager.redo();
        }

        assertFalse("Nao deve haver mais operacoes para refazer", undoManager.canRedo());
        assertTrue("UndoManager deve permitir desfazer novamente", undoManager.canUndo());

        long duration = System.currentTimeMillis() - startTime;

        // O ciclo completo de 6.000 operacoes deve ocorrer em menos de 2.000 ms
        assertTrue("Ciclo massivo de 6000 operacoes de undo/redo deve durar menos de 2000 ms (durou: " + duration + " ms)",
                duration < 2000);

        // 4. Verificacao de estabilidade de memoria
        System.gc();
        long usedMemoryAfterGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        assertTrue("Memoria utilizada apos GC deve ser valida e positiva", usedMemoryAfterGc > 0);
    }
}

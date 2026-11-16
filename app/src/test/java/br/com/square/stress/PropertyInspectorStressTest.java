package br.com.square.stress;

import br.com.square.ui.inspector.PropertyInspectorPanel;
import com.eteks.sweethome3d.io.FileUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Teste de estresse e responsividade para o PropertyInspectorPanel
 * sob centenas de alternancias rapidas de selecao na interface.
 */
public class PropertyInspectorStressTest {

    @Test
    public void testRapidSelectionSwitchingStress() {
        int wallCount = 100;
        int furnitureCount = 200;
        int roomCount = 50;

        Home home = LargeHomeStressTest.createLargeHome(wallCount, furnitureCount, roomCount);
        UserPreferences preferences = new FileUserPreferences();
        ViewFactory viewFactory = new SwingViewFactory();
        HomeController controller = new HomeController(home, preferences, viewFactory);

        PropertyInspectorPanel inspector = new PropertyInspectorPanel(home, controller, preferences);
        assertNotNull(inspector);

        long startTime = System.currentTimeMillis();

        // Alterna selecao sucessivamente entre paredes, moveis e comodos
        int totalSwitches = 0;

        for (Wall wall : home.getWalls()) {
            inspector.updateInspectorForSelection(Collections.singletonList(wall));
            assertTrue("Inspetor deve ter filhos apos inspecionar parede", inspector.getComponentCount() > 0);
            totalSwitches++;
        }

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            inspector.updateInspectorForSelection(Collections.singletonList(piece));
            assertTrue("Inspetor deve ter filhos apos inspecionar movel", inspector.getComponentCount() > 0);
            totalSwitches++;
        }

        for (Room room : home.getRooms()) {
            inspector.updateInspectorForSelection(Collections.singletonList(room));
            assertTrue("Inspetor deve ter filhos apos inspecionar comodo", inspector.getComponentCount() > 0);
            totalSwitches++;
        }

        // Selecao vazia (resumo geral)
        inspector.updateInspectorForSelection(Collections.emptyList());
        assertTrue("Inspetor deve exibir resumo quando nada selecionado", inspector.getComponentCount() > 0);
        totalSwitches++;

        long duration = System.currentTimeMillis() - startTime;

        // O ciclo completo de 350+ alternancias de painel deve ocorrer em menos de 1000 ms
        assertTrue("Alternancias rapidas de selecao (" + totalSwitches + " trocas) devem durar menos de 1000 ms (durou: " + duration + " ms)",
                duration < 1000);
    }
}

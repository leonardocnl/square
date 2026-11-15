package br.com.square.ui.inspector;

import com.eteks.sweethome3d.io.FileUserPreferences;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o PropertyInspectorPanel.
 */
public class PropertyInspectorPanelTest {

    @Test
    public void testPropertyInspectorSelectionReaction() {
        Home home = new Home();
        UserPreferences preferences = new FileUserPreferences();
        ViewFactory viewFactory = new SwingViewFactory();
        HomeController controller = new HomeController(home, preferences, viewFactory);

        PropertyInspectorPanel inspector = new PropertyInspectorPanel(home, controller, preferences);
        assertNotNull("Inspetor deve ser instanciado", inspector);

        // Estado inicial vazio
        inspector.updateInspectorForSelection(Collections.emptyList());
        assertEquals("Inspetor deve ter tamanho preferencial de 280px", 280, inspector.getPreferredSize().width);

        // Seleciona uma parede
        Wall wall = new Wall(10f, 10f, 200f, 10f, 15f, 250f);
        home.addWall(wall);
        home.setSelectedItems(Collections.singletonList(wall));

        inspector.updateInspectorForSelection(home.getSelectedItems());
        assertTrue("Inspetor deve conter componentes para a parede", inspector.getComponentCount() > 0);

        // Seleciona um movel
        CatalogPieceOfFurniture catalogPiece = new CatalogPieceOfFurniture("Cadeira", null, null, 50f, 50f, 80f, true, false);
        HomePieceOfFurniture piece = new HomePieceOfFurniture(catalogPiece);
        home.addPieceOfFurniture(piece);
        home.setSelectedItems(Collections.singletonList(piece));

        inspector.updateInspectorForSelection(home.getSelectedItems());
        assertTrue("Inspetor deve conter componentes para o movel", inspector.getComponentCount() > 0);
    }
}

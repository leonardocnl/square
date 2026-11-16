package br.com.square.ui.palette;

import com.eteks.sweethome3d.io.FileUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeView.ActionType;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import org.junit.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para a Paleta de Comandos e seu modelo de busca.
 */
public class CommandPaletteTest {

    @Test
    public void testCommandPaletteIndexingAndExecution() {
        Home home = new Home();
        UserPreferences preferences = new FileUserPreferences();
        ViewFactory viewFactory = new SwingViewFactory();
        HomeController controller = new HomeController(home, preferences, viewFactory);

        final AtomicBoolean actionExecuted = new AtomicBoolean(false);
        ActionMap actionMap = new ActionMap();
        Action customAction = new AbstractAction("Criar Parede de Teste") {
            public void actionPerformed(ActionEvent e) {
                actionExecuted.set(true);
            }
        };
        actionMap.put(ActionType.CREATE_WALLS, customAction);

        CommandPaletteModel model = new CommandPaletteModel(home, controller, preferences, actionMap);
        assertNotNull("Modelo deve ser instanciado", model);
        assertFalse("Itens indexados nao devem estar vazios", model.getAllItems().isEmpty());

        // Filtragem por termo
        List<PaletteItem> searchResults = model.filter("parede");
        assertFalse("Busca por 'parede' deve retornar resultados", searchResults.isEmpty());

        PaletteItem matchedItem = searchResults.get(0);
        assertEquals("Criar Parede de Teste", matchedItem.getTitle());
        assertEquals(PaletteItem.ItemType.COMMAND, matchedItem.getType());

        // Execucao do comando
        matchedItem.execute();
        assertTrue("Acao do item deve ser executada com sucesso", actionExecuted.get());

        // Validar que acoes internas como 'release' e 'press' sao filtradas
        actionMap.put("release", new AbstractAction("release") {
            public void actionPerformed(ActionEvent e) {}
        });
        actionMap.put("press", new AbstractAction("press") {
            public void actionPerformed(ActionEvent e) {}
        });
        model.reindex();

        for (PaletteItem item : model.getAllItems()) {
            assertFalse("Acoes internas 'release' nao podem vazar", "release".equalsIgnoreCase(item.getTitle()));
            assertFalse("Acoes internas 'press' nao podem vazar", "press".equalsIgnoreCase(item.getTitle()));
        }

        // Filtragem por acao estendida SVG
        List<PaletteItem> svgResults = model.filter("svg");
        assertFalse("Busca por 'svg' deve retornar a acao de exportacao", svgResults.isEmpty());
        assertTrue("Item deve conter mencao a SVG", svgResults.get(0).getTitle().contains("SVG"));

        // Filtragem por acao estendida Memorial Descritivo
        List<PaletteItem> memorialResults = model.filter("memorial");
        assertFalse("Busca por 'memorial' deve retornar a acao de memorial", memorialResults.isEmpty());
        assertTrue("Item deve conter mencao a Memorial", memorialResults.get(0).getTitle().contains("Memorial"));
    }
}

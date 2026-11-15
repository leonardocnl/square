package br.com.square.ui.dock;

import com.eteks.sweethome3d.viewcontroller.HomeView.ActionType;
import org.junit.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitários para o FloatingDockComponent e DockOverlayPane.
 */
public class FloatingDockComponentTest {

    @Test
    public void testFloatingDockCreationAndLayout() {
        ActionMap actionMap = new ActionMap();
        actionMap.put(ActionType.SELECT, new AbstractAction("Selecionar") {
            public void actionPerformed(ActionEvent e) {
            }
        });
        actionMap.put(ActionType.CREATE_WALLS, new AbstractAction("Criar Paredes") {
            public void actionPerformed(ActionEvent e) {
            }
        });

        FloatingDockComponent dock = new FloatingDockComponent(actionMap);
        assertNotNull("Dock não deve ser nulo", dock);
        assertTrue("Dock deve conter componentes filhos adicionados", dock.getComponentCount() > 0);

        JPanel mainContent = new JPanel();
        DockOverlayPane overlay = new DockOverlayPane(mainContent, dock);
        overlay.setSize(new Dimension(1024, 768));
        overlay.doLayout();

        assertEquals("Main content deve preencher a largura total", 1024, mainContent.getWidth());
        assertEquals("Main content deve preencher a altura total", 768, mainContent.getHeight());
        assertTrue("Dock deve estar visível por padrão", dock.isVisible());
        assertTrue("Dock deve estar centralizado no topo com margem superior", dock.getY() > 0);

        overlay.setDockVisible(false);
        assertFalse("Dock deve ser ocultável", dock.isVisible());

        overlay.setDockVisible(true);
        assertTrue("Dock deve ser reexibível", dock.isVisible());
    }
}

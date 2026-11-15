package br.com.square.ui.history;

import com.eteks.sweethome3d.io.FileUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeView;
import com.eteks.sweethome3d.viewcontroller.ViewFactory;
import org.junit.Test;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o ActionHistoryPanel.
 */
public class ActionHistoryPanelTest {

    private static class SampleEdit extends AbstractUndoableEdit {
        private final String name;
        private boolean undone = false;

        public SampleEdit(String name) {
            this.name = name;
        }

        @Override
        public String getPresentationName() {
            return this.name;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            this.undone = true;
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            this.undone = false;
        }

        public boolean isUndone() {
            return undone;
        }
    }

    @Test
    public void testActionHistoryTrackingAndNavigation() {
        Home home = new Home();
        UserPreferences preferences = new FileUserPreferences();

        final HomeView dummyHomeView = (HomeView) java.lang.reflect.Proxy.newProxyInstance(
                HomeView.class.getClassLoader(),
                new Class<?>[] { HomeView.class },
                new java.lang.reflect.InvocationHandler() {
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        return null;
                    }
                });

        ViewFactory dummyViewFactory = (ViewFactory) java.lang.reflect.Proxy.newProxyInstance(
                ViewFactory.class.getClassLoader(),
                new Class<?>[] { ViewFactory.class },
                new java.lang.reflect.InvocationHandler() {
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("createHomeView".equals(method.getName())) {
                            return dummyHomeView;
                        }
                        return null;
                    }
                });

        HomeController controller = new HomeController(home, preferences, dummyViewFactory);

        ActionHistoryPanel historyPanel = new ActionHistoryPanel(controller);
        assertNotNull("Painel de historico deve ser criado com sucesso", historyPanel);
        assertEquals("Contagem inicial de acoes deve ser zero", 0, historyPanel.getRecordedActionsCount());

        // Dispara acoes de teste
        SampleEdit edit1 = new SampleEdit("Mover Parede");
        controller.getUndoableEditSupport().postEdit(edit1);

        SampleEdit edit2 = new SampleEdit("Adicionar Móvel");
        controller.getUndoableEditSupport().postEdit(edit2);

        // Aguarda despacho do EDT se necessario
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    // Atualizacao no EDT concluida
                }
            });
        } catch (Exception ignored) {
        }

        assertEquals("Historico deve conter 2 acoes registradas", 2, historyPanel.getRecordedActionsCount());

        // Testar retrocesso direto para passo inicial (-1)
        historyPanel.jumpToStep(-1);
        assertTrue("A primeira acao deve ter sido revertida", edit1.isUndone());
        assertTrue("A segunda acao deve ter sido revertida", edit2.isUndone());

        // Testar avanco para o primeiro passo (0)
        historyPanel.jumpToStep(0);
        assertTrue("A primeira acao deve estar refeita", !edit1.isUndone());
        assertTrue("A segunda acao ainda deve estar desfeita", edit2.isUndone());

        // Testar avanco para o segundo passo (1)
        historyPanel.jumpToStep(1);
        assertTrue("A segunda acao deve estar refeita", !edit2.isUndone());
    }
}


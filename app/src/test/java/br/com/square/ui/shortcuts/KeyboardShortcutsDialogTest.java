package br.com.square.ui.shortcuts;

import org.junit.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Testes unitarios para o catalogo e dialogo de atalhos de teclado do Square.
 */
public class KeyboardShortcutsDialogTest {

    @Test
    public void testShortcutsCatalogData() {
        List<KeyboardShortcutsDialog.ShortcutItem> shortcuts = KeyboardShortcutsDialog.createDefaultShortcuts();
        assertNotNull("Lista de atalhos nao deve ser nula", shortcuts);
        assertFalse("Lista de atalhos nao deve estar vazia", shortcuts.isEmpty());
        assertTrue("Deve conter mais de 10 atalhos documentados", shortcuts.size() >= 10);

        boolean foundCtrlK = false;
        boolean foundF11 = false;
        boolean foundSvg = false;
        boolean foundRender = false;
        boolean foundUndo = false;

        for (KeyboardShortcutsDialog.ShortcutItem item : shortcuts) {
            assertNotNull("Categoria deve existir", item.getCategory());
            assertNotNull("Nome da acao deve existir", item.getActionName());
            assertNotNull("Teclas devem existir", item.getKeys());

            if (item.getKeys().contains("Ctrl + K") || item.getKeys().contains("Cmd + K")) {
                foundCtrlK = true;
            }
            if (item.getKeys().contains("F11")) {
                foundF11 = true;
            }
            if (item.getActionName().toLowerCase().contains("svg")) {
                foundSvg = true;
            }
            if (item.getActionName().toLowerCase().contains("render")) {
                foundRender = true;
            }
            if (item.getActionName().toLowerCase().contains("undo") || item.getActionName().toLowerCase().contains("desfazer")) {
                foundUndo = true;
            }
        }

        assertTrue("Atalho Ctrl+K deve estar presente", foundCtrlK);
        assertTrue("Atalho F11 deve estar presente", foundF11);
        assertTrue("Acao de SVG deve estar presente", foundSvg);
        assertTrue("Acao de Render deve estar presente", foundRender);
        assertTrue("Acao de Undo deve estar presente", foundUndo);

        if (!GraphicsEnvironment.isHeadless()) {
            KeyboardShortcutsDialog dialog = new KeyboardShortcutsDialog(null);
            assertNotNull("Dialogo deve ser instanciado fora do modo headless", dialog);
            dialog.dispose();
        }
    }
}

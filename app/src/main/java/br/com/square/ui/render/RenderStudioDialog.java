package br.com.square.ui.render;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.PhotoController;
import com.eteks.sweethome3d.viewcontroller.View;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Dialogo moderno e nao-bloqueante que envelopa o RenderStudioPanel.
 * Permite ao usuario visualizar o progresso de renderizacao fotorrealista
 * em alta definicao sem interromper a navegacao do projeto.
 */
public class RenderStudioDialog implements DialogView {

    private final Home home;
    private final UserPreferences preferences;
    private final PhotoController photoController;
    private JDialog dialog;

    public RenderStudioDialog(Home home, UserPreferences preferences, PhotoController photoController) {
        this.home = home;
        this.preferences = preferences;
        this.photoController = photoController;
    }

    @Override
    public void displayView(View parentView) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }

        if (dialog != null && dialog.isDisplayable()) {
            dialog.toFront();
            return;
        }

        Component parent = parentView instanceof Component ? (Component) parentView : null;
        dialog = new JDialog(parent != null ? SwingUtilities.getWindowAncestor(parent) : null,
                "Estudio de Renderizacao Foto e Iluminacao");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        RenderStudioPanel panel = new RenderStudioPanel(home, preferences, photoController);
        dialog.setContentPane(panel);
        dialog.setSize(new Dimension(1020, 680));
        dialog.setMinimumSize(new Dimension(800, 500));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public JDialog getDialog() {
        return dialog;
    }
}

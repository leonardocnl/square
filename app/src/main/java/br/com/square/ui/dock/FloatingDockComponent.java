package br.com.square.ui.dock;

import br.com.square.ui.theme.ThemeManager;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.viewcontroller.HomeView.ActionType;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Dock flutuante de ferramentas estilo Figma para o Square.
 * Sobrepõe-se à área principal de desenho com cantos arredondados,
 * fundo translúcido moderno, agrupamento ergonômico de ações
 * e suporte dinâmico a tema claro e escuro.
 */
public class FloatingDockComponent extends JPanel {

    private static final int CORNER_RADIUS = 16;
    private static final Color DARK_BG = new Color(24, 24, 27, 230);
    private static final Color DARK_BORDER = new Color(255, 255, 255, 30);
    private static final Color LIGHT_BG = new Color(255, 255, 255, 235);
    private static final Color LIGHT_BORDER = new Color(0, 0, 0, 25);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 40);

    public FloatingDockComponent(ActionMap actionMap) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        buildToolGroups(actionMap);

        ThemeManager.addThemeChangeListener(new ThemeManager.ThemeChangeListener() {
            public void themeChanged(ThemeManager.ThemeMode newMode, boolean isDark) {
                repaint();
            }
        });
    }

    private void buildToolGroups(ActionMap actionMap) {
        if (actionMap == null) {
            return;
        }

        // Grupo 1: Seleção e Pan (Navegação)
        addButton(actionMap, ActionType.SELECT, true);
        addButton(actionMap, ActionType.PAN, true);

        addSeparator();

        // Grupo 2: Construção Arquitetural
        addButton(actionMap, ActionType.CREATE_WALLS, true);
        addButton(actionMap, ActionType.CREATE_ROOMS, true);
        addButton(actionMap, ActionType.CREATE_POLYLINES, true);
        addButton(actionMap, ActionType.CREATE_DIMENSION_LINES, true);
        addButton(actionMap, ActionType.CREATE_LABELS, true);

        addSeparator();

        // Grupo 3: Catálogo, Undo e Redo
        addButton(actionMap, ActionType.ADD_HOME_FURNITURE, false);
        addButton(actionMap, ActionType.UNDO, false);
        addButton(actionMap, ActionType.REDO, false);

        addSeparator();

        // Grupo 4: Zoom e Visualização 3D
        addButton(actionMap, ActionType.ZOOM_IN, false);
        addButton(actionMap, ActionType.ZOOM_OUT, false);
        addButton(actionMap, ActionType.VIEW_FROM_TOP, true);
        addButton(actionMap, ActionType.VIEW_FROM_OBSERVER, true);
    }

    private void addButton(ActionMap actionMap, ActionType actionType, boolean isToggle) {
        Action action = actionMap.get(actionType);
        if (action == null || action.getValue(Action.NAME) == null) {
            return;
        }

        Action toolBarAction = new ResourceAction.ToolBarAction(action);
        final AbstractButton button;

        if (isToggle && action.getValue(ResourceAction.TOGGLE_BUTTON_MODEL) != null) {
            JToggleButton toggleBtn = new JToggleButton(toolBarAction);
            toggleBtn.setModel((JToggleButton.ToggleButtonModel) action.getValue(ResourceAction.TOGGLE_BUTTON_MODEL));
            button = toggleBtn;
        } else {
            button = new JButton(toolBarAction);
        }

        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("Component.arc", 10);
        button.setMargin(new Insets(3, 4, 3, 4));

        add(button);
        add(Box.createRigidArea(new Dimension(2, 0)));
    }

    private void addSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setMaximumSize(new Dimension(1, 20));
        separator.setPreferredSize(new Dimension(1, 20));
        add(Box.createRigidArea(new Dimension(4, 0)));
        add(separator);
        add(Box.createRigidArea(new Dimension(4, 0)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Sombra sutil arredondada
        g2.setColor(SHADOW_COLOR);
        g2.fillRoundRect(1, 2, w - 2, h - 2, CORNER_RADIUS, CORNER_RADIUS);

        // Fundo translúcido moderno adaptativo
        boolean dark = ThemeManager.isDark();
        g2.setColor(dark ? DARK_BG : LIGHT_BG);
        g2.fillRoundRect(0, 0, w - 1, h - 2, CORNER_RADIUS, CORNER_RADIUS);

        // Borda minimalista
        g2.setColor(dark ? DARK_BORDER : LIGHT_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 2, CORNER_RADIUS, CORNER_RADIUS);

        g2.dispose();
        super.paintComponent(g);
    }
}

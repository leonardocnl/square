package br.com.square.ui.dock;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Container em camadas (JLayeredPane) que posiciona o FloatingDockComponent
 * sobreposto a area de trabalho. Permite movimentacao livre em X e Y,
 * com margem inicial posicionada abaixo da regua da planta 2D.
 */
public class DockOverlayPane extends JLayeredPane {

    // Margem inicial de 42px para nao cobrir os numeros da regua da planta 2D
    public static final int DEFAULT_DOCK_OFFSET_Y = 42;

    private final JComponent mainContent;
    private final FloatingDockComponent floatingDock;
    private int dockOffsetY = DEFAULT_DOCK_OFFSET_Y;
    private Integer customDockX = null;
    private Integer customDockY = null;

    public DockOverlayPane(JComponent mainContent, FloatingDockComponent floatingDock) {
        this.mainContent = mainContent;
        this.floatingDock = floatingDock;

        setLayout(null);
        add(mainContent, JLayeredPane.DEFAULT_LAYER);
        add(floatingDock, JLayeredPane.PALETTE_LAYER);

        setupDockDragging();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutComponents();
            }
        });
    }

    private void setupDockDragging() {
        MouseAdapter dragAdapter = new MouseAdapter() {
            private int initialMouseX;
            private int initialMouseY;
            private int initialDockX;
            private int initialDockY;

            @Override
            public void mousePressed(MouseEvent e) {
                initialMouseX = e.getXOnScreen();
                initialMouseY = e.getYOnScreen();
                initialDockX = floatingDock.getX();
                initialDockY = floatingDock.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getXOnScreen() - initialMouseX;
                int deltaY = e.getYOnScreen() - initialMouseY;

                int newX = initialDockX + deltaX;
                int newY = initialDockY + deltaY;

                int maxX = Math.max(0, getWidth() - floatingDock.getWidth());
                int maxY = Math.max(0, getHeight() - floatingDock.getHeight());

                customDockX = Math.max(0, Math.min(newX, maxX));
                // Permite movimentacao vertical livre mantendo no minimo 36px do topo
                customDockY = Math.max(36, Math.min(newY, maxY));

                floatingDock.setLocation(customDockX, customDockY);
                repaint();
            }
        };

        floatingDock.addMouseListener(dragAdapter);
        floatingDock.addMouseMotionListener(dragAdapter);
    }

    private void relayoutComponents() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        mainContent.setBounds(0, 0, w, h);

        Dimension dockSize = floatingDock.getPreferredSize();
        int dockW = Math.min(dockSize.width, w - 20);
        int dockH = dockSize.height;

        int dockX;
        if (customDockX != null) {
            int maxX = Math.max(0, w - dockW);
            dockX = Math.max(0, Math.min(customDockX, maxX));
        } else {
            dockX = Math.max(10, (w - dockW) / 2);
        }

        int dockY;
        if (customDockY != null) {
            int maxY = Math.max(0, h - dockH);
            dockY = Math.max(36, Math.min(customDockY, maxY));
        } else {
            dockY = Math.min(dockOffsetY, Math.max(0, h - dockH));
        }

        floatingDock.setBounds(dockX, dockY, dockW, dockH);
    }

    @Override
    public void doLayout() {
        relayoutComponents();
    }

    public FloatingDockComponent getFloatingDock() {
        return floatingDock;
    }

    public void setDockVisible(boolean visible) {
        floatingDock.setVisible(visible);
        revalidate();
        repaint();
    }

    public boolean isDockVisible() {
        return floatingDock.isVisible();
    }

    public void setDockOffsetY(int offset) {
        this.dockOffsetY = offset;
        this.customDockY = null;
        relayoutComponents();
    }

    public void dockToBottom() {
        int h = getHeight();
        int dockH = floatingDock.getPreferredSize().height;
        this.customDockY = Math.max(36, h - dockH - 16);
        relayoutComponents();
    }

    public void dockToTop() {
        this.customDockY = DEFAULT_DOCK_OFFSET_Y;
        relayoutComponents();
    }
}

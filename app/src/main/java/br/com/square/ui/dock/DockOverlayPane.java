package br.com.square.ui.dock;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Container em camadas (JLayeredPane) que posiciona o FloatingDockComponent
 * sobreposto ao topo da área de trabalho dos quatro painéis.
 */
public class DockOverlayPane extends JLayeredPane {

    private final JComponent mainContent;
    private final FloatingDockComponent floatingDock;
    private int dockOffsetY = 12;
    private Integer customDockX = null;

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
            private int initialDockX;

            @Override
            public void mousePressed(MouseEvent e) {
                initialMouseX = e.getXOnScreen();
                initialDockX = floatingDock.getX();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getXOnScreen() - initialMouseX;
                int newX = initialDockX + deltaX;
                int maxX = Math.max(0, getWidth() - floatingDock.getWidth());
                customDockX = Math.max(0, Math.min(newX, maxX));
                floatingDock.setLocation(customDockX, dockOffsetY);
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

        floatingDock.setBounds(dockX, dockOffsetY, dockW, dockH);
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
}

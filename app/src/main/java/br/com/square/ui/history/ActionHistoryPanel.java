package br.com.square.ui.history;

import br.com.square.ui.theme.ThemeManager;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel visual de Historico de Acoes (Undo/Redo granular).
 * Exibe a cronologia das modificacoes realizadas e permite retroceder
 * ou avancar para qualquer ponto anterior com um unico clique.
 */
public class ActionHistoryPanel extends JPanel {

    public static class HistoryEntry {
        private final String presentationName;
        private final boolean isCurrent;
        private final boolean isRedoable;

        public HistoryEntry(String presentationName, boolean isCurrent, boolean isRedoable) {
            this.presentationName = presentationName;
            this.isCurrent = isCurrent;
            this.isRedoable = isRedoable;
        }

        public String getPresentationName() {
            return presentationName;
        }

        public boolean isCurrent() {
            return isCurrent;
        }

        public boolean isRedoable() {
            return isRedoable;
        }
    }

    private final HomeController controller;
    private final UndoManager undoManager;
    private final DefaultListModel<HistoryEntry> listModel;
    private final JList<HistoryEntry> historyList;
    private final JLabel statusLabel;

    private final List<String> recordedActions = new ArrayList<String>();
    private int currentActionIndex = -1;

    public ActionHistoryPanel(final HomeController controller) {
        this.controller = controller;
        this.undoManager = controller.getUndoManager();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(280, 0));
        setMinimumSize(new Dimension(200, 0));

        // Cabecalho do Historico
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel titleLabel = new JLabel("Hist\u00F3rico de A\u00E7\u00F5es");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel("Estado Inicial");
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        headerPanel.add(statusLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Lista de passos
        listModel = new DefaultListModel<HistoryEntry>();
        historyList = new JList<HistoryEntry>(listModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());

        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        setupListeners();
        refreshHistoryList();

        ThemeManager.addThemeChangeListener(new ThemeManager.ThemeChangeListener() {
            public void themeChanged(ThemeManager.ThemeMode newMode, boolean isDark) {
                repaint();
            }
        });
    }

    private void setupListeners() {
        if (controller.getUndoableEditSupport() != null) {
            controller.getUndoableEditSupport().addUndoableEditListener(new UndoableEditListener() {
                public void undoableEditHappened(final UndoableEditEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            String name = e.getEdit().getPresentationName();
                            if (name == null || name.trim().isEmpty()) {
                                name = "A\u00E7\u00E3o";
                            }

                            // Remove passos futuros se um novo edit for realizado apos undo
                            while (recordedActions.size() > currentActionIndex + 1) {
                                recordedActions.remove(recordedActions.size() - 1);
                            }

                            recordedActions.add(name);
                            currentActionIndex = recordedActions.size() - 1;
                            refreshHistoryList();
                        }
                    });
                }
            });
        }

        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = historyList.getSelectedIndex();
                if (clickedIndex >= 0 && clickedIndex != currentActionIndex + 1) {
                    jumpToStep(clickedIndex - 1);
                }
            }
        });
    }

    /**
     * Pula diretamente para o passo desejado executando sucessivos undo ou redo.
     */
    public void jumpToStep(int targetIndex) {
        if (targetIndex < -1 || targetIndex >= recordedActions.size()) {
            return;
        }

        if (targetIndex < currentActionIndex) {
            int steps = currentActionIndex - targetIndex;
            for (int i = 0; i < steps; i++) {
                if (undoManager.canUndo()) {
                    controller.undo();
                }
            }
        } else if (targetIndex > currentActionIndex) {
            int steps = targetIndex - currentActionIndex;
            for (int i = 0; i < steps; i++) {
                if (undoManager.canRedo()) {
                    controller.redo();
                }
            }
        }

        currentActionIndex = targetIndex;
        refreshHistoryList();
    }

    public void refreshHistoryList() {
        listModel.clear();

        // Ponto zero: Estado Inicial
        boolean isInitialCurrent = (currentActionIndex == -1);
        listModel.addElement(new HistoryEntry("Estado Inicial do Projeto", isInitialCurrent, currentActionIndex < -1));

        for (int i = 0; i < recordedActions.size(); i++) {
            boolean isCurrent = (i == currentActionIndex);
            boolean isRedoable = (i > currentActionIndex);
            listModel.addElement(new HistoryEntry(recordedActions.get(i), isCurrent, isRedoable));
        }

        int selectedViewIndex = currentActionIndex + 1;
        if (selectedViewIndex >= 0 && selectedViewIndex < listModel.size()) {
            historyList.setSelectedIndex(selectedViewIndex);
            historyList.ensureIndexIsVisible(selectedViewIndex);
        }

        statusLabel.setText(listModel.size() > 1
                ? (selectedViewIndex) + "/" + (listModel.size() - 1)
                : "Inicial");
    }

    public int getRecordedActionsCount() {
        return recordedActions.size();
    }

    /**
     * Renderizador estilizado para os passos do historico de acoes.
     */
    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        private final JPanel panel;
        private final JLabel bulletLabel;
        private final JLabel nameLabel;
        private final JLabel badgeLabel;

        public HistoryCellRenderer() {
            panel = new JPanel(new BorderLayout(8, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            bulletLabel = new JLabel("\u2022");
            bulletLabel.setFont(bulletLabel.getFont().deriveFont(Font.BOLD, 14f));
            panel.add(bulletLabel, BorderLayout.WEST);

            nameLabel = new JLabel();
            nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
            panel.add(nameLabel, BorderLayout.CENTER);

            badgeLabel = new JLabel();
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(10f));
            panel.add(badgeLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value instanceof HistoryEntry) {
                HistoryEntry entry = (HistoryEntry) value;
                nameLabel.setText(entry.getPresentationName());

                if (entry.isCurrent()) {
                    bulletLabel.setForeground(ThemeManager.isDark() ? new Color(0x60, 0xA5, 0xFA) : new Color(0x25, 0x63, 0xEB));
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
                    badgeLabel.setText("ATUAL");
                    badgeLabel.setForeground(bulletLabel.getForeground());
                    panel.setBackground(UIManager.getColor("List.selectionBackground"));
                } else if (entry.isRedoable()) {
                    bulletLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                    nameLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
                    badgeLabel.setText("REFAZER");
                    badgeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                    panel.setBackground(list.getBackground());
                } else {
                    bulletLabel.setForeground(ThemeManager.isDark() ? new Color(0x34, 0xD3, 0x99) : new Color(0x05, 0x96, 0x69));
                    nameLabel.setForeground(list.getForeground());
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
                    badgeLabel.setText("");
                    panel.setBackground(list.getBackground());
                }
            }
            return panel;
        }
    }
}


package br.com.square.ui.palette;

import br.com.square.ui.theme.ThemeManager;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Paleta universal de comandos e busca rapida (Ctrl+K).
 * Apresenta a interface flutuante e delega a busca para o CommandPaletteModel.
 */
public class CommandPaletteDialog extends JDialog {

    private final CommandPaletteModel model;
    private final JTextField searchField;
    private final JList<PaletteItem> resultsList;
    private final DefaultListModel<PaletteItem> listModel;

    public CommandPaletteDialog(Window parent, Home home, HomeController controller,
                                UserPreferences preferences, ActionMap actionMap) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.model = new CommandPaletteModel(home, controller, preferences, actionMap);

        setUndecorated(true);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(560, 380));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createLineBorder(
                ThemeManager.isDark() ? new Color(60, 60, 65) : new Color(210, 210, 215), 1));

        // Topo: Campo de busca
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Buscar comandos, ferramentas ou m\u00F3veis...");
        searchField.putClientProperty("Component.arc", 8);
        searchField.setFont(searchField.getFont().deriveFont(14f));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.isDark() ? new Color(50, 50, 55) : new Color(220, 220, 225), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        searchPanel.add(searchField, BorderLayout.CENTER);
        contentPanel.add(searchPanel, BorderLayout.NORTH);

        // Centro: Lista de resultados
        listModel = new DefaultListModel<PaletteItem>();
        resultsList = new JList<PaletteItem>(listModel);
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsList.setCellRenderer(new PaletteCellRenderer());

        JScrollPane scrollPane = new JScrollPane(resultsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 14));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Rodape: Instrucoes de atalhos
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel hintsLabel = new JLabel("Navegar: \u2191\u2193   \u2022   Executar: Enter   \u2022   Fechar: Esc");
        hintsLabel.setFont(hintsLabel.getFont().deriveFont(11f));
        hintsLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        footerPanel.add(hintsLabel, BorderLayout.WEST);
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        setupEvents();
        filterItems("");

        pack();
        if (parent != null) {
            setLocation(parent.getX() + (parent.getWidth() - getWidth()) / 2,
                    parent.getY() + Math.max(60, parent.getHeight() / 6));
        } else {
            setLocationRelativeTo(null);
        }
    }

    private void setupEvents() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        filterItems(searchField.getText());
                    }
                });
            }
        });

        // Navegacao por teclado
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int next = resultsList.getSelectedIndex() + 1;
                    if (next < listModel.size()) {
                        resultsList.setSelectedIndex(next);
                        resultsList.ensureIndexIsVisible(next);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int prev = resultsList.getSelectedIndex() - 1;
                    if (prev >= 0) {
                        resultsList.setSelectedIndex(prev);
                        resultsList.ensureIndexIsVisible(prev);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeSelected();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                    e.consume();
                }
            }
        });

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 || e.getClickCount() == 2) {
                    executeSelected();
                }
            }
        });

        // Fechar com ESC em todo o dialogo
        getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void filterItems(String query) {
        listModel.clear();
        List<PaletteItem> filtered = model.filter(query);
        for (PaletteItem item : filtered) {
            listModel.addElement(item);
        }

        if (!listModel.isEmpty()) {
            resultsList.setSelectedIndex(0);
        }
    }

    private void executeSelected() {
        PaletteItem selected = resultsList.getSelectedValue();
        if (selected != null) {
            dispose();
            selected.execute();
        }
    }

    /**
     * Renderizador customizado para itens da lista da paleta.
     */
    private static class PaletteCellRenderer extends DefaultListCellRenderer {
        private final JPanel cellPanel;
        private final JLabel iconLabel;
        private final JLabel titleLabel;
        private final JLabel subtitleLabel;
        private final JLabel badgeLabel;

        public PaletteCellRenderer() {
            cellPanel = new JPanel(new BorderLayout(10, 0));
            cellPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            iconLabel = new JLabel();
            cellPanel.add(iconLabel, BorderLayout.WEST);

            JPanel textPanel = new JPanel(new BorderLayout(4, 0));
            textPanel.setOpaque(false);
            titleLabel = new JLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
            subtitleLabel = new JLabel();
            subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(11f));
            subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

            textPanel.add(titleLabel, BorderLayout.WEST);
            textPanel.add(subtitleLabel, BorderLayout.CENTER);
            cellPanel.add(textPanel, BorderLayout.CENTER);

            badgeLabel = new JLabel();
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(10f));
            badgeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            cellPanel.add(badgeLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value instanceof PaletteItem) {
                PaletteItem item = (PaletteItem) value;
                titleLabel.setText(item.getTitle());
                subtitleLabel.setText("  " + item.getSubtitle());
                iconLabel.setIcon(item.getIcon());
                badgeLabel.setText(item.getType() == PaletteItem.ItemType.COMMAND ? "A\u00C7\u00C3O" : "CAT\u00C1LOGO");

                if (isSelected) {
                    cellPanel.setBackground(UIManager.getColor("List.selectionBackground"));
                    titleLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                    subtitleLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                    badgeLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                } else {
                    cellPanel.setBackground(list.getBackground());
                    titleLabel.setForeground(list.getForeground());
                    subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                    badgeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                }
            }
            return cellPanel;
        }
    }
}

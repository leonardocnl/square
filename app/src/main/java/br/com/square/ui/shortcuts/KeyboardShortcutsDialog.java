package br.com.square.ui.shortcuts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Dialogo interativo de consulta e busca de atalhos de teclado do Square.
 * Organiza comandos de navegacao, desenho 2D, renderizacao e gestao em categorias
 * com filtragem em tempo real e visual compativel com FlatLaf.
 */
public class KeyboardShortcutsDialog extends JDialog {

    public static class ShortcutItem {
        private final String category;
        private final String actionName;
        private final String keys;

        public ShortcutItem(String category, String actionName, String keys) {
            this.category = category;
            this.actionName = actionName;
            this.keys = keys;
        }

        public String getCategory() { return category; }
        public String getActionName() { return actionName; }
        public String getKeys() { return keys; }
    }

    private final List<ShortcutItem> shortcuts = new ArrayList<>();
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;

    public KeyboardShortcutsDialog(Component parent) {
        super(parent != null ? SwingUtilities.getWindowAncestor(parent) : null,
                "Atalhos de Teclado e Comandos \u2014 Square");

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(840, 560));
        setMinimumSize(new Dimension(640, 400));
        if (parent != null) {
            setLocationRelativeTo(parent);
        }

        initShortcutsData();
        initComponents();
        setupKeyBindings();
    }

    private void initShortcutsData() {
        this.shortcuts.addAll(createDefaultShortcuts());
    }

    public static List<ShortcutItem> createDefaultShortcuts() {
        List<ShortcutItem> list = new ArrayList<>();

        // Geral & Navegacao
        list.add(new ShortcutItem("Geral & Navegacao", "Abrir Paleta de Comandos universal", "Ctrl + K (Cmd + K)"));
        list.add(new ShortcutItem("Geral & Navegacao", "Alternar Tema Claro / Escuro", "Ctrl + Shift + T"));
        list.add(new ShortcutItem("Geral & Navegacao", "Exibir / Ocultar Dock Flutuante", "Ctrl + Shift + D"));
        list.add(new ShortcutItem("Geral & Navegacao", "Exibir / Ocultar Painel Lateral (Inspetor)", "Ctrl + Shift + I"));
        list.add(new ShortcutItem("Geral & Navegacao", "Abrir Painel de Historico de Acoes", "Ctrl + Shift + H"));
        list.add(new ShortcutItem("Geral & Navegacao", "Modo Apresentacao (Tela Cheia)", "F11"));
        list.add(new ShortcutItem("Geral & Navegacao", "Abrir Central de Atalhos de Teclado", "Ctrl + / ou F1"));

        // Desenho 2D & Alinhamento
        list.add(new ShortcutItem("Desenho 2D & Planta", "Ignorar Snapping Magnetico (Movimento Livre)", "Alt (segurar durante arraste)"));
        list.add(new ShortcutItem("Desenho 2D & Planta", "Travar angulo em multiplos de 15, 45 e 90 graus", "Shift (segurar)"));
        list.add(new ShortcutItem("Desenho 2D & Planta", "Mover planta (Pan / Rolagem livre)", "Espaco + Arraste ou Botao do Meio"));
        list.add(new ShortcutItem("Desenho 2D & Planta", "Aproximar / Afastar Zoom", "Ctrl + Roda do Mouse"));
        list.add(new ShortcutItem("Desenho 2D & Planta", "Ajustar visualizacao da planta (Zoom 100%)", "Ctrl + 0"));
        list.add(new ShortcutItem("Desenho 2D & Planta", "Enquadrar desenho na tela", "F"));

        // Edicao & Gestao
        list.add(new ShortcutItem("Edicao & Gestao", "Desfazer ultima acao (Undo)", "Ctrl + Z"));
        list.add(new ShortcutItem("Edicao & Gestao", "Refazer ultima acao (Redo)", "Ctrl + Y (Ctrl + Shift + Z)"));
        list.add(new ShortcutItem("Edicao & Gestao", "Selecionar Todos os Itens", "Ctrl + A"));
        list.add(new ShortcutItem("Edicao & Gestao", "Excluir itens selecionados", "Delete ou Backspace"));
        list.add(new ShortcutItem("Edicao & Gestao", "Duplicar selecao", "Ctrl + D"));
        list.add(new ShortcutItem("Edicao & Gestao", "Salvar projeto", "Ctrl + S"));
        list.add(new ShortcutItem("Edicao & Gestao", "Salvar projeto como...", "Ctrl + Shift + S"));

        // Renderizacao & Exportacao
        list.add(new ShortcutItem("Render & Exportacao", "Abrir Estudio de Renderizacao Foto e Sol", "Ctrl + Shift + R"));
        list.add(new ShortcutItem("Render & Exportacao", "Exportar Planta em Formato Vetorial SVG", "Ctrl + Shift + E"));
        list.add(new ShortcutItem("Render & Exportacao", "Gerar Memorial Descritivo e Quantitativos (BOM)", "Ctrl + Shift + Q"));
        list.add(new ShortcutItem("Render & Exportacao", "Exportar para formato OBJ 3D", "Ctrl + Shift + O"));
        list.add(new ShortcutItem("Render & Exportacao", "Imprimir Planta / Exportar para PDF", "Ctrl + P"));

        return list;
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Topo: Titulo e Campo de Busca
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Atalhos de Teclado e Comandos");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(new Color(0x38, 0xBD, 0xF8));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        topPanel.add(titleLabel);

        topPanel.add(Box.createVerticalStrut(6));

        JLabel descLabel = new JLabel("Consulte ou filtre comandos para agilizar o fluxo de projeto no Square.");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        descLabel.setForeground(new Color(0x94, 0xA3, 0xB8));
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        topPanel.add(descLabel);

        topPanel.add(Box.createVerticalStrut(12));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Digite para buscar comando ou tecla (ex: snap, render, ctrl+k)...");
        searchField.setFont(searchField.getFont().deriveFont(13f));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        topPanel.add(searchField);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // Centro: Tabela
        String[] columns = {"Categoria", "Acao / Comando", "Atalho de Teclado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (ShortcutItem item : shortcuts) {
            tableModel.addRow(new Object[]{item.category, item.actionName, item.keys});
        }

        rowSorter = new TableRowSorter<>(tableModel);
        JTable table = new JTable(tableModel);
        table.setRowSorter(rowSorter);
        table.setRowHeight(28);
        table.setFont(table.getFont().deriveFont(13f));
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);

        DefaultTableCellRenderer keysRenderer = new DefaultTableCellRenderer();
        keysRenderer.setFont(keysRenderer.getFont().deriveFont(Font.BOLD, 12f));
        keysRenderer.setForeground(new Color(0x38, 0xBD, 0xF8));
        table.getColumnModel().getColumn(2).setCellRenderer(keysRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x3F, 0x3F, 0x46), 1));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Rodape: Botao de Fechar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton closeBtn = new JButton("Fechar");
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);
        contentPane.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private void filter() {
        String text = searchField.getText();
        if (text == null || text.trim().isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }

    private void setupKeyBindings() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JPanel.WHEN_IN_FOCUSED_WINDOW);
    }

    public List<ShortcutItem> getShortcuts() {
        return shortcuts;
    }
}

package br.com.square.ui.report;

import br.com.square.report.ProjectSpecificationReport;
import br.com.square.report.ProjectSpecificationReport.FurnitureSpec;
import br.com.square.report.ProjectSpecificationReport.ReportData;
import br.com.square.report.ProjectSpecificationReport.RoomSpec;
import com.eteks.sweethome3d.model.Home;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Dialogo executivo de Memorial Descritivo e Quantitativo (BOM).
 * Exibe metricas de area, extensao de paredes, relacao de moveis e esquadrias
 * com botoes de exportacao rapida para HTML e CSV.
 */
public class ProjectSpecificationDialog extends JDialog {

    private final Home home;
    private final ReportData data;

    private static final DecimalFormat METRIC_DF;
    private static final DecimalFormat CURRENCY_DF;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        METRIC_DF = new DecimalFormat("0.00", symbols);
        CURRENCY_DF = new DecimalFormat("#,##0.00", symbols);
    }

    public ProjectSpecificationDialog(Component parent, Home home) {
        super(parent != null ? SwingUtilities.getWindowAncestor(parent) : null,
                "Memorial Descritivo e Quantitativos Executivos — Square");
        this.home = home;
        this.data = ProjectSpecificationReport.generateReportData(home);

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(980, 680));
        setMinimumSize(new Dimension(750, 500));
        if (parent != null) {
            setLocationRelativeTo(parent);
        }

        initComponents();
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(new EmptyBorder(16, 16, 16, 16));

        // 1. Topo: Cartoes de KPI
        JPanel kpiPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        kpiPanel.add(createKpiCard("Area Total Util", METRIC_DF.format(data.getTotalAreaM2()) + " m\u00B2"));
        kpiPanel.add(createKpiCard("Paredes Lineares", METRIC_DF.format(data.getTotalWallLengthM()) + " m"));
        kpiPanel.add(createKpiCard("Superficie Paredes", METRIC_DF.format(data.getTotalWallSurfaceAreaM2()) + " m\u00B2"));
        kpiPanel.add(createKpiCard("Moveis", data.getTotalFurnitureCount() + " un."));
        kpiPanel.add(createKpiCard("Esquadrias", data.getTotalDoorsAndWindowsCount() + " un."));

        contentPane.add(kpiPanel, BorderLayout.NORTH);

        // 2. Centro: Abas com Tabelas
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Comodos e Areas", createRoomsTab());
        tabbedPane.addTab("Mobiliario (" + data.getFurnitureList().size() + ")", createFurnitureTab());
        tabbedPane.addTab("Esquadrias (" + data.getDoorsAndWindowsList().size() + ")", createDoorsWindowsTab());

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // 3. Rodape: Botoes de Acao
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton exportHtmlBtn = new JButton("Exportar HTML...");
        exportHtmlBtn.addActionListener(e -> exportHtml());
        bottomBar.add(exportHtmlBtn);

        JButton exportCsvBtn = new JButton("Exportar CSV...");
        exportCsvBtn.addActionListener(e -> exportCsv());
        bottomBar.add(exportCsvBtn);

        JButton closeBtn = new JButton("Fechar");
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn);

        contentPane.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private JPanel createKpiCard(String label, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3F, 0x3F, 0x46), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(valLbl.getFont().deriveFont(Font.BOLD, 18f));
        valLbl.setForeground(new Color(0x38, 0xBD, 0xF8));
        card.add(valLbl);

        card.add(Box.createVerticalStrut(4));

        JLabel textLbl = new JLabel(label);
        textLbl.setFont(textLbl.getFont().deriveFont(Font.PLAIN, 11f));
        textLbl.setForeground(new Color(0x94, 0xA3, 0xB8));
        card.add(textLbl);

        return card;
    }

    private JScrollPane createRoomsTab() {
        String[] cols = {"Comodo", "Area (m\u00B2)", "Perimetro (m)", "Acabamento Piso", "Acabamento Teto"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (RoomSpec r : data.getRooms()) {
            model.addRow(new Object[]{
                    r.getName(),
                    METRIC_DF.format(r.getAreaM2()),
                    METRIC_DF.format(r.getPerimeterM()),
                    r.getFloorFinish(),
                    r.getCeilingFinish()
            });
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private JScrollPane createFurnitureTab() {
        String[] cols = {"Movel", "Comodo", "Qtd", "Dimensoes (LxPxA cm)", "Preco Unitario", "Preco Total"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (FurnitureSpec f : data.getFurnitureList()) {
            model.addRow(new Object[]{
                    f.getName(),
                    f.getRoomName(),
                    f.getQuantity(),
                    Math.round(f.getWidthCm()) + "x" + Math.round(f.getDepthCm()) + "x" + Math.round(f.getHeightCm()),
                    "R$ " + CURRENCY_DF.format(f.getUnitPrice()),
                    "R$ " + CURRENCY_DF.format(f.getTotalPrice())
            });
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private JScrollPane createDoorsWindowsTab() {
        String[] cols = {"Descricao", "Comodo", "Qtd", "Largura (cm)", "Altura (cm)", "Peitoril (cm)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        for (FurnitureSpec d : data.getDoorsAndWindowsList()) {
            model.addRow(new Object[]{
                    d.getName(),
                    d.getRoomName(),
                    d.getQuantity(),
                    Math.round(d.getWidthCm()),
                    Math.round(d.getHeightCm()),
                    Math.round(d.getElevationCm())
            });
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private void exportHtml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Memorial Descritivo em HTML");
        chooser.setFileFilter(new FileNameExtensionFilter("Documento HTML (*.html)", "html"));
        chooser.setSelectedFile(new File("memorial-descritivo.html"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".html")) {
                target = new File(target.getAbsolutePath() + ".html");
            }
            try {
                ProjectSpecificationReport.exportHtmlToFile(home, target);
                JOptionPane.showMessageDialog(this, "Relatorio HTML salvo com sucesso em:\n" + target.getAbsolutePath(),
                        "Exportacao Concluida", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Falha ao gravar arquivo HTML: " + ex.getMessage(),
                        "Erro de Gravacao", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Quantitativos em CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("Planilha CSV (*.csv)", "csv"));
        chooser.setSelectedFile(new File("quantitativos.csv"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getAbsolutePath() + ".csv");
            }
            try {
                ProjectSpecificationReport.exportCsvToFile(home, target);
                JOptionPane.showMessageDialog(this, "Planilha CSV salva com sucesso em:\n" + target.getAbsolutePath(),
                        "Exportacao Concluida", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Falha ao gravar arquivo CSV: " + ex.getMessage(),
                        "Erro de Gravacao", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

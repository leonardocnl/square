package br.com.square.ui.render;

import br.com.square.export.SvgPlanExporter;
import br.com.square.render.RenderStudioService;
import br.com.square.render.RenderStudioService.RenderQuality;
import br.com.square.render.RenderStudioService.RenderStatus;
import br.com.square.render.RenderStudioService.ResolutionPreset;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;

/**
 * Painel moderno do Estudio de Renderizacao Foto e Iluminacao do Square.
 * Opera de forma desacoplada em segundo plano, com controles intuitivos
 * de resolucao, qualidade, horario solar e exportacao direta.
 */
public class RenderStudioPanel extends JPanel {

    private final Home home;
    private final HomeController controller;
    private final UserPreferences preferences;
    private final RenderStudioService renderService;

    private JComboBox<ResolutionPreset> resolutionCombo;
    private JComboBox<RenderQuality> qualityCombo;
    private JSlider sunHourSlider;
    private JLabel sunHourLabel;
    private JCheckBox ceilingLightsCheckBox;

    private JButton renderButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private JButton savePngButton;
    private JButton saveJpegButton;
    private JButton copyClipboardButton;
    private JButton exportSvgButton;

    private PreviewCanvas previewCanvas;
    private BufferedImage currentRenderedImage;

    public RenderStudioPanel(Home home, UserPreferences preferences) {
        this(home, null, preferences);
    }

    public RenderStudioPanel(Home home, UserPreferences preferences, com.eteks.sweethome3d.viewcontroller.PhotoController photoController) {
        this(home, null, preferences);
    }

    public RenderStudioPanel(Home home, HomeController controller, UserPreferences preferences) {
        this.home = home;
        this.controller = controller;
        this.preferences = preferences;
        this.renderService = new RenderStudioService();

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
    }

    private void initComponents() {
        JPanel controlsPanel = createControlsPanel();
        JPanel previewPanel = createPreviewPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlsPanel, previewPanel);
        splitPane.setDividerLocation(340);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(4, 4, 4, 12));

        // Titulo da secao
        JLabel titleLabel = new JLabel("Estudio de Renderizacao");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(16));

        // 1. Resolucao
        JLabel resLabel = new JLabel("Resolucao:");
        resLabel.setFont(resLabel.getFont().deriveFont(Font.BOLD, 12f));
        resLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(resLabel);
        panel.add(Box.createVerticalStrut(4));

        resolutionCombo = new JComboBox<>(ResolutionPreset.values());
        resolutionCombo.setSelectedItem(ResolutionPreset.FULL_HD_1080P);
        resolutionCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        resolutionCombo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(resolutionCombo);
        panel.add(Box.createVerticalStrut(14));

        // 2. Qualidade
        JLabel qualityLabel = new JLabel("Qualidade de Renderizacao:");
        qualityLabel.setFont(qualityLabel.getFont().deriveFont(Font.BOLD, 12f));
        qualityLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(qualityLabel);
        panel.add(Box.createVerticalStrut(4));

        qualityCombo = new JComboBox<>(RenderQuality.values());
        qualityCombo.setSelectedItem(RenderQuality.HIGH);
        qualityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        qualityCombo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(qualityCombo);
        panel.add(Box.createVerticalStrut(14));

        // 3. Iluminacao e Sol
        JLabel lightHeader = new JLabel("Iluminacao e Ambiente:");
        lightHeader.setFont(lightHeader.getFont().deriveFont(Font.BOLD, 12f));
        lightHeader.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lightHeader);
        panel.add(Box.createVerticalStrut(4));

        double initialHour = RenderStudioService.getSunHour(home);
        sunHourLabel = new JLabel(formatHourLabel(initialHour));
        sunHourLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(sunHourLabel);

        sunHourSlider = new JSlider(0, 48, (int) Math.round(initialHour * 2.0));
        sunHourSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        sunHourSlider.setAlignmentX(LEFT_ALIGNMENT);
        sunHourSlider.addChangeListener(e -> {
            double hour = sunHourSlider.getValue() / 2.0;
            sunHourLabel.setText(formatHourLabel(hour));
            RenderStudioService.setSunHour(home, hour);
        });
        panel.add(sunHourSlider);
        panel.add(Box.createVerticalStrut(8));

        ceilingLightsCheckBox = new JCheckBox("Luzes do Teto Ligadas", RenderStudioService.isCeilingLightsEnabled(home));
        ceilingLightsCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        ceilingLightsCheckBox.addActionListener(e -> {
            RenderStudioService.setCeilingLightsEnabled(home, ceilingLightsCheckBox.isSelected());
        });
        panel.add(ceilingLightsCheckBox);
        panel.add(Box.createVerticalStrut(20));

        // 4. Botoes de Acao de Render
        renderButton = new JButton("Renderizar Foto");
        renderButton.setFont(renderButton.getFont().deriveFont(Font.BOLD, 13f));
        renderButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        renderButton.setAlignmentX(LEFT_ALIGNMENT);
        renderButton.addActionListener(e -> startRender());
        panel.add(renderButton);
        panel.add(Box.createVerticalStrut(6));

        cancelButton = new JButton("Cancelar");
        cancelButton.setEnabled(false);
        cancelButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cancelButton.setAlignmentX(LEFT_ALIGNMENT);
        cancelButton.addActionListener(e -> cancelRender());
        panel.add(cancelButton);
        panel.add(Box.createVerticalStrut(12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(6));

        statusLabel = new JLabel("Pronto para renderizar");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(20));

        // 5. Secao de Exportacao
        JLabel exportHeader = new JLabel("Exportacao:");
        exportHeader.setFont(exportHeader.getFont().deriveFont(Font.BOLD, 12f));
        exportHeader.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(exportHeader);
        panel.add(Box.createVerticalStrut(8));

        savePngButton = new JButton("Salvar Foto em PNG");
        savePngButton.setEnabled(false);
        savePngButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        savePngButton.setAlignmentX(LEFT_ALIGNMENT);
        savePngButton.addActionListener(e -> saveRenderedImage("png"));
        panel.add(savePngButton);
        panel.add(Box.createVerticalStrut(4));

        saveJpegButton = new JButton("Salvar Foto em JPEG");
        saveJpegButton.setEnabled(false);
        saveJpegButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        saveJpegButton.setAlignmentX(LEFT_ALIGNMENT);
        saveJpegButton.addActionListener(e -> saveRenderedImage("jpg"));
        panel.add(saveJpegButton);
        panel.add(Box.createVerticalStrut(4));

        copyClipboardButton = new JButton("Copiar para Area de Transferencia");
        copyClipboardButton.setEnabled(false);
        copyClipboardButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        copyClipboardButton.setAlignmentX(LEFT_ALIGNMENT);
        copyClipboardButton.addActionListener(e -> copyImageToClipboard());
        panel.add(copyClipboardButton);
        panel.add(Box.createVerticalStrut(8));

        exportSvgButton = new JButton("Exportar Planta em SVG");
        exportSvgButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        exportSvgButton.setAlignmentX(LEFT_ALIGNMENT);
        exportSvgButton.addActionListener(e -> exportPlanToSvg());
        panel.add(exportSvgButton);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(new Color(0x3F, 0x3F, 0x46), 1));

        previewCanvas = new PreviewCanvas();
        panel.add(previewCanvas, BorderLayout.CENTER);

        return panel;
    }

    private void startRender() {
        ResolutionPreset resolution = (ResolutionPreset) resolutionCombo.getSelectedItem();
        RenderQuality quality = (RenderQuality) qualityCombo.getSelectedItem();

        renderButton.setEnabled(false);
        cancelButton.setEnabled(true);
        savePngButton.setEnabled(false);
        saveJpegButton.setEnabled(false);
        copyClipboardButton.setEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText("Iniciando renderizacao...");

        renderService.renderPhotoAsync(
                home,
                home.getCamera(),
                resolution,
                quality,
                status -> SwingUtilities.invokeLater(() -> handleStatusChange(status)),
                progress -> SwingUtilities.invokeLater(() -> progressBar.setValue(progress))
        ).thenAccept(image -> SwingUtilities.invokeLater(() -> {
            this.currentRenderedImage = image;
            previewCanvas.setImage(image);
            savePngButton.setEnabled(true);
            saveJpegButton.setEnabled(true);
            copyClipboardButton.setEnabled(true);
        })).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Erro na renderizacao: " + ex.getMessage());
                renderButton.setEnabled(true);
                cancelButton.setEnabled(false);
            });
            return null;
        });
    }

    private void cancelRender() {
        renderService.cancelRender();
        cancelButton.setEnabled(false);
        renderButton.setEnabled(true);
        statusLabel.setText("Renderizacao cancelada");
    }

    private void handleStatusChange(RenderStatus status) {
        switch (status) {
            case PREPARING:
                statusLabel.setText("Preparando cena e iluminacao...");
                break;
            case RENDERING:
                statusLabel.setText("Renderizando fotorealismo...");
                break;
            case COMPLETED:
                statusLabel.setText("Renderizacao concluida com sucesso");
                renderButton.setEnabled(true);
                cancelButton.setEnabled(false);
                break;
            case CANCELLED:
                statusLabel.setText("Cancelado pelo usuario");
                renderButton.setEnabled(true);
                cancelButton.setEnabled(false);
                break;
            case ERROR:
                statusLabel.setText("Ocorreu um erro no motor de renderizacao");
                renderButton.setEnabled(true);
                cancelButton.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void saveRenderedImage(String format) {
        if (currentRenderedImage == null) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Imagem Renderizada");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                format.toUpperCase() + " Image", format));
        fileChooser.setSelectedFile(new File("square-render." + format));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith("." + format)) {
                selectedFile = new File(selectedFile.getAbsolutePath() + "." + format);
            }
            try {
                RenderStudioService.saveImageToFile(currentRenderedImage, selectedFile, format);
                JOptionPane.showMessageDialog(this, "Imagem salva com sucesso em:\n" + selectedFile.getAbsolutePath(),
                        "Exportacao Concluida", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Falha ao gravar imagem: " + ex.getMessage(),
                        "Erro de Gravacao", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyImageToClipboard() {
        if (currentRenderedImage != null) {
            RenderStudioService.copyToClipboard(currentRenderedImage);
            statusLabel.setText("Imagem copiada para a area de transferencia");
        }
    }

    private void exportPlanToSvg() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Planta em SVG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Scalable Vector Graphics (*.svg)", "svg"));
        fileChooser.setSelectedFile(new File("planta-arquitetural.svg"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".svg")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".svg");
            }
            try {
                SvgPlanExporter.exportToFile(home, selectedFile);
                JOptionPane.showMessageDialog(this, "Planta vetorial salva com sucesso em:\n" + selectedFile.getAbsolutePath(),
                        "Exportacao SVG Concluida", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Falha ao exportar SVG: " + ex.getMessage(),
                        "Erro de Exportacao", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatHourLabel(double hour) {
        int h = (int) hour;
        int m = (int) Math.round((hour - h) * 60.0);
        String period;
        if (h >= 5 && h < 12) {
            period = "Manha";
        } else if (h >= 12 && h < 18) {
            period = "Tarde";
        } else {
            period = "Noite";
        }
        return String.format("Horario Solar: %02d:%02d (%s)", h, m, period);
    }

    public BufferedImage getCurrentRenderedImage() {
        return currentRenderedImage;
    }

    public RenderStudioService getRenderService() {
        return renderService;
    }

    private static class PreviewCanvas extends JPanel {
        private BufferedImage image;

        public PreviewCanvas() {
            setBackground(new Color(0x18, 0x18, 0x1B));
        }

        public void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setColor(new Color(0x71, 0x71, 0x7A));
                String msg = "Nenhuma foto renderizada. Configure os parametros e clique em 'Renderizar Foto'.";
                int textWidth = g2d.getFontMetrics().stringWidth(msg);
                g2d.drawString(msg, Math.max(20, (getWidth() - textWidth) / 2), getHeight() / 2);
                return;
            }

            int w = getWidth();
            int h = getHeight();
            int imgW = image.getWidth();
            int imgH = image.getHeight();

            double scale = Math.min((double) w / imgW, (double) h / imgH);
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int drawX = (w - drawW) / 2;
            int drawY = (h - drawH) / 2;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, drawX, drawY, drawW, drawH, null);
        }
    }
}


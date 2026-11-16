package br.com.square.ui.materials;

import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.CollectionEvent;
import com.eteks.sweethome3d.model.CollectionListener;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.TexturesCatalog;
import com.eteks.sweethome3d.model.TexturesCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.IconManager;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Painel de catalogo dedicado a Materiais, Texturas e Acabamentos.
 * Permite buscar, filtrar por categoria e aplicar texturas diretamente
 * a pecas de mobilia, paredes e pisos selecionados com suporte a undo/redo.
 */
public class MaterialsCatalogPanel extends JPanel {

    private final Home home;
    private final UserPreferences preferences;
    private final HomeController controller;
    private final UndoableEditSupport undoSupport;

    private final JTextField searchField;
    private final JComboBox<String> categoryComboBox;
    private final DefaultListModel<CatalogTexture> listModel;
    private final JList<CatalogTexture> textureList;
    private final JButton applyButton;
    private final JLabel statusLabel;

    public MaterialsCatalogPanel(Home home, UserPreferences preferences, HomeController controller) {
        super(new BorderLayout(0, 6));
        this.home = home;
        this.preferences = preferences;
        this.controller = controller;
        this.undoSupport = controller != null ? controller.getUndoableEditSupport() : null;

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // 1. Barra de busca e categorias no topo
        JPanel topPanel = new JPanel(new BorderLayout(0, 4));

        this.searchField = new JTextField();
        this.searchField.putClientProperty("JTextField.placeholderText", "Buscar materiais...");
        this.searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTextures();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTextures();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTextures();
            }
        });

        this.categoryComboBox = new JComboBox<String>();
        this.categoryComboBox.addItem("Todas as Categorias");
        populateCategories();
        this.categoryComboBox.addActionListener(e -> filterTextures());

        topPanel.add(this.searchField, BorderLayout.NORTH);
        topPanel.add(this.categoryComboBox, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 2. Lista de texturas com miniaturas
        this.listModel = new DefaultListModel<CatalogTexture>();
        this.textureList = new JList<CatalogTexture>(this.listModel);
        this.textureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.textureList.setCellRenderer(new TextureCellRenderer());
        this.textureList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    applySelectedTexture();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(this.textureList);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        add(scrollPane, BorderLayout.CENTER);

        // 3. Barra inferior com acao e status
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));

        this.statusLabel = new JLabel("Selecione um item para aplicar");
        this.statusLabel.setFont(this.statusLabel.getFont().deriveFont(11f));

        this.applyButton = new JButton("Aplicar \u00E0 Sele\u00E7\u00E3o");
        this.applyButton.setEnabled(false);
        this.applyButton.addActionListener(e -> applySelectedTexture());

        this.textureList.addListSelectionListener(e -> {
            boolean hasSelection = !textureList.isSelectionEmpty();
            applyButton.setEnabled(hasSelection);
        });

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonBar.add(this.applyButton);

        bottomPanel.add(this.statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonBar, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Escutar atualizacoes do catalogo de texturas
        if (preferences != null && preferences.getTexturesCatalog() != null) {
            preferences.getTexturesCatalog().addTexturesListener(new CollectionListener<CatalogTexture>() {
                @Override
                public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
                    populateCategories();
                    filterTextures();
                }
            });
        }

        filterTextures();
    }

    private void populateCategories() {
        if (preferences == null || preferences.getTexturesCatalog() == null) {
            return;
        }
        String currentSelection = (String) categoryComboBox.getSelectedItem();
        categoryComboBox.removeAllItems();
        categoryComboBox.addItem("Todas as Categorias");

        for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
            categoryComboBox.addItem(category.getName());
        }

        if (currentSelection != null) {
            categoryComboBox.setSelectedItem(currentSelection);
        }
    }

    private void filterTextures() {
        listModel.clear();
        if (preferences == null || preferences.getTexturesCatalog() == null) {
            return;
        }

        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        boolean allCategories = selectedCategory == null || selectedCategory.equals("Todas as Categorias");

        for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
            if (allCategories || category.getName().equalsIgnoreCase(selectedCategory)) {
                for (CatalogTexture texture : category.getTextures()) {
                    if (query.isEmpty() || texture.getName().toLowerCase().contains(query)) {
                        listModel.addElement(texture);
                    }
                }
            }
        }
    }

    /**
     * Aplica a textura selecionada a peca de mobilia, parede ou piso selecionado no Home.
     */
    public void applySelectedTexture() {
        CatalogTexture catalogTexture = textureList.getSelectedValue();
        if (catalogTexture == null || home == null) {
            return;
        }

        List<Selectable> selectedItems = home.getSelectedItems();
        if (selectedItems.isEmpty()) {
            statusLabel.setText("Nenhum item selecionado na planta");
            return;
        }

        HomeTexture newTexture = new HomeTexture(catalogTexture);

        for (Selectable item : selectedItems) {
            if (item instanceof HomePieceOfFurniture) {
                applyTextureToFurniture((HomePieceOfFurniture) item, newTexture);
            } else if (item instanceof Wall) {
                applyTextureToWall((Wall) item, newTexture);
            } else if (item instanceof Room) {
                applyTextureToRoom((Room) item, newTexture);
            }
        }

        statusLabel.setText("Material aplicado com sucesso");
    }

    private void applyTextureToFurniture(final HomePieceOfFurniture piece, final HomeTexture newTexture) {
        final HomeTexture oldTexture = piece.getTexture();
        piece.setTexture(newTexture);

        if (undoSupport != null) {
            undoSupport.postEdit(new AbstractUndoableEdit() {
                @Override
                public String getPresentationName() {
                    return "Alterar material de " + piece.getName();
                }

                @Override
                public void undo() throws CannotUndoException {
                    super.undo();
                    piece.setTexture(oldTexture);
                }

                @Override
                public void redo() throws CannotRedoException {
                    super.redo();
                    piece.setTexture(newTexture);
                }
            });
        }
    }

    private void applyTextureToWall(final Wall wall, final HomeTexture newTexture) {
        final HomeTexture oldLeft = wall.getLeftSideTexture();
        final HomeTexture oldRight = wall.getRightSideTexture();

        wall.setLeftSideTexture(newTexture);
        wall.setRightSideTexture(newTexture);

        if (undoSupport != null) {
            undoSupport.postEdit(new AbstractUndoableEdit() {
                @Override
                public String getPresentationName() {
                    return "Alterar acabamento de parede";
                }

                @Override
                public void undo() throws CannotUndoException {
                    super.undo();
                    wall.setLeftSideTexture(oldLeft);
                    wall.setRightSideTexture(oldRight);
                }

                @Override
                public void redo() throws CannotRedoException {
                    super.redo();
                    wall.setLeftSideTexture(newTexture);
                    wall.setRightSideTexture(newTexture);
                }
            });
        }
    }

    private void applyTextureToRoom(final Room room, final HomeTexture newTexture) {
        final HomeTexture oldFloor = room.getFloorTexture();
        room.setFloorTexture(newTexture);

        if (undoSupport != null) {
            undoSupport.postEdit(new AbstractUndoableEdit() {
                @Override
                public String getPresentationName() {
                    return "Alterar piso de " + (room.getName() != null ? room.getName() : "c\u00F4modo");
                }

                @Override
                public void undo() throws CannotUndoException {
                    super.undo();
                    room.setFloorTexture(oldFloor);
                }

                @Override
                public void redo() throws CannotRedoException {
                    super.redo();
                    room.setFloorTexture(newTexture);
                }
            });
        }
    }

    public JList<CatalogTexture> getTextureList() {
        return textureList;
    }

    public DefaultListModel<CatalogTexture> getListModel() {
        return listModel;
    }

    /**
     * Renderizador de celula com miniatura da textura e nome.
     */
    private static class TextureCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof CatalogTexture) {
                CatalogTexture texture = (CatalogTexture) value;
                setText(texture.getName());
                setPreferredSize(new Dimension(180, 40));

                Icon icon = IconManager.getInstance().getIcon(texture.getImage(), 32, null);
                setIcon(icon);
            }

            return this;
        }
    }
}

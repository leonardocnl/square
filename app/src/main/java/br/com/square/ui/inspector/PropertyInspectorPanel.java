package br.com.square.ui.inspector;

import br.com.square.ui.theme.ThemeManager;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

/**
 * Painel lateral de inspecao e edicao de propriedades em tempo real (Figma-style).
 * Reage instantaneamente a selecao na planta 2D ou visualizacao 3D,
 * eliminando modais bloqueantes e suportando desfazer/refazer (Undo/Redo).
 */
public class PropertyInspectorPanel extends JPanel {

    private final Home home;
    private final HomeController controller;
    private final UserPreferences preferences;
    private final UndoableEditSupport undoSupport;

    private final JLabel headerTitle;
    private final JPanel contentContainer;
    private final JScrollPane scrollPane;

    private Selectable activeItem;
    private PropertyChangeListener activeItemListener;
    private final DecimalFormat decimalFormat;

    public PropertyInspectorPanel(Home home, HomeController controller, UserPreferences preferences) {
        this.home = home;
        this.controller = controller;
        this.preferences = preferences;
        this.undoSupport = controller.getUndoableEditSupport();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        this.decimalFormat = new DecimalFormat("#.##", symbols);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(280, 0));
        setMinimumSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Separator.foreground")));

        // Cabecalho do Inspetor
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        headerTitle = new JLabel("Propriedades");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 13f));
        headerPanel.add(headerTitle, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Conteudo com rolagem
        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        contentContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentContainer.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.add(contentContainer, BorderLayout.NORTH);

        scrollPane = new JScrollPane(scrollWrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        setupSelectionListener();
        updateInspectorForSelection(home.getSelectedItems());

        ThemeManager.addThemeChangeListener(new ThemeManager.ThemeChangeListener() {
            public void themeChanged(ThemeManager.ThemeMode newMode, boolean isDark) {
                setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Separator.foreground")));
                revalidate();
                repaint();
            }
        });
    }

    private void setupSelectionListener() {
        this.home.addSelectionListener(new SelectionListener() {
            public void selectionChanged(SelectionEvent ev) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateInspectorForSelection(home.getSelectedItems());
                    }
                });
            }
        });
    }

    private void detachActiveItemListener() {
        if (activeItem != null && activeItemListener != null) {
            if (activeItem instanceof Wall) {
                ((Wall) activeItem).removePropertyChangeListener(activeItemListener);
            } else if (activeItem instanceof HomePieceOfFurniture) {
                ((HomePieceOfFurniture) activeItem).removePropertyChangeListener(activeItemListener);
            } else if (activeItem instanceof Room) {
                ((Room) activeItem).removePropertyChangeListener(activeItemListener);
            }
            activeItemListener = null;
        }
        activeItem = null;
    }

    public void updateInspectorForSelection(List<Selectable> selectedItems) {
        detachActiveItemListener();
        contentContainer.removeAll();

        if (selectedItems == null || selectedItems.isEmpty()) {
            buildEmptySelectionView();
        } else if (selectedItems.size() == 1) {
            Selectable item = selectedItems.get(0);
            activeItem = item;

            if (item instanceof Wall) {
                buildWallView((Wall) item);
            } else if (item instanceof HomePieceOfFurniture) {
                buildFurnitureView((HomePieceOfFurniture) item);
            } else if (item instanceof Room) {
                buildRoomView((Room) item);
            } else if (item instanceof DimensionLine) {
                buildDimensionLineView((DimensionLine) item);
            } else if (item instanceof Label) {
                buildLabelView((Label) item);
            } else {
                buildGenericItemView(item);
            }
        } else {
            buildMultipleSelectionView(selectedItems);
        }

        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private void buildEmptySelectionView() {
        headerTitle.setText("Projeto");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Resumo do Projeto");
        addReadonlyField(panel, gbc, "Paredes", String.valueOf(home.getWalls().size()));
        addReadonlyField(panel, gbc, "M\u00F3veis", String.valueOf(home.getFurniture().size()));
        addReadonlyField(panel, gbc, "C\u00F4modos", String.valueOf(home.getRooms().size()));

        contentContainer.add(panel);
    }

    private void buildMultipleSelectionView(List<Selectable> items) {
        headerTitle.setText("Sele\u00E7\u00E3o M\u00FAltipla (" + items.size() + ")");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Itens Selecionados");
        addReadonlyField(panel, gbc, "Total de itens", String.valueOf(items.size()));

        contentContainer.add(panel);
    }

    private void buildWallView(final Wall wall) {
        headerTitle.setText("Parede");

        activeItemListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (activeItem == wall) {
                            updateInspectorForSelection(home.getSelectedItems());
                        }
                    }
                });
            }
        };
        wall.addPropertyChangeListener(activeItemListener);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Dimens\u00F5es");
        addFloatField(panel, gbc, "Comprimento (cm)", wall.getLength(), new FloatValueConsumer() {
            public void accept(float val) {
                // Modifica o comprimento ajustando o ponto final proporcionalmente
                float angle = (float) Math.atan2(wall.getYEnd() - wall.getYStart(), wall.getXEnd() - wall.getXStart());
                final float oldXEnd = wall.getXEnd();
                final float oldYEnd = wall.getYEnd();
                final float newXEnd = wall.getXStart() + (float) Math.cos(angle) * val;
                final float newYEnd = wall.getYStart() + (float) Math.sin(angle) * val;

                executeEdit("Alterar comprimento da parede", new Runnable() {
                    public void run() {
                        wall.setXEnd(newXEnd);
                        wall.setYEnd(newYEnd);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setXEnd(oldXEnd);
                        wall.setYEnd(oldYEnd);
                    }
                });
            }
        });

        addFloatField(panel, gbc, "Espessura (cm)", wall.getThickness(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = wall.getThickness();
                executeEdit("Alterar espessura da parede", new Runnable() {
                    public void run() {
                        wall.setThickness(val);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setThickness(oldVal);
                    }
                });
            }
        });

        Float wallHeight = wall.getHeight();
        float currentHeight = wallHeight != null ? wallHeight : preferences.getNewWallHeight();
        addFloatField(panel, gbc, "Altura (cm)", currentHeight, new FloatValueConsumer() {
            public void accept(final float val) {
                final Float oldVal = wall.getHeight();
                executeEdit("Alterar altura da parede", new Runnable() {
                    public void run() {
                        wall.setHeight(val);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setHeight(oldVal);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Posi\u00E7\u00E3o");
        addFloatField(panel, gbc, "X Inicial", wall.getXStart(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = wall.getXStart();
                executeEdit("Mover ponto inicial da parede", new Runnable() {
                    public void run() {
                        wall.setXStart(val);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setXStart(oldVal);
                    }
                });
            }
        });
        addFloatField(panel, gbc, "Y Inicial", wall.getYStart(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = wall.getYStart();
                executeEdit("Mover ponto inicial da parede", new Runnable() {
                    public void run() {
                        wall.setYStart(val);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setYStart(oldVal);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Acabamento");
        addColorChooserField(panel, gbc, "Lado Esquerdo", wall.getLeftSideColor(), new ColorConsumer() {
            public void accept(final Integer color) {
                final Integer oldColor = wall.getLeftSideColor();
                executeEdit("Alterar cor do lado esquerdo da parede", new Runnable() {
                    public void run() {
                        wall.setLeftSideColor(color);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setLeftSideColor(oldColor);
                    }
                });
            }
        });

        addColorChooserField(panel, gbc, "Lado Direito", wall.getRightSideColor(), new ColorConsumer() {
            public void accept(final Integer color) {
                final Integer oldColor = wall.getRightSideColor();
                executeEdit("Alterar cor do lado direito da parede", new Runnable() {
                    public void run() {
                        wall.setRightSideColor(color);
                    }
                }, new Runnable() {
                    public void run() {
                        wall.setRightSideColor(oldColor);
                    }
                });
            }
        });

        contentContainer.add(panel);
    }

    private void buildFurnitureView(final HomePieceOfFurniture piece) {
        headerTitle.setText("M\u00F3vel: " + piece.getName());

        activeItemListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (activeItem == piece) {
                            updateInspectorForSelection(home.getSelectedItems());
                        }
                    }
                });
            }
        };
        piece.addPropertyChangeListener(activeItemListener);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Identifica\u00E7\u00E3o");
        addTextField(panel, gbc, "Nome", piece.getName(), new StringConsumer() {
            public void accept(final String val) {
                final String oldVal = piece.getName();
                executeEdit("Renomear m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setName(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setName(oldVal);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Dimens\u00F5es");
        addFloatField(panel, gbc, "Largura (cm)", piece.getWidth(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getWidth();
                executeEdit("Alterar largura do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setWidth(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setWidth(oldVal);
                    }
                });
            }
        });

        addFloatField(panel, gbc, "Profundidade (cm)", piece.getDepth(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getDepth();
                executeEdit("Alterar profundidade do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setDepth(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setDepth(oldVal);
                    }
                });
            }
        });

        addFloatField(panel, gbc, "Altura (cm)", piece.getHeight(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getHeight();
                executeEdit("Alterar altura do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setHeight(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setHeight(oldVal);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Posi\u00E7\u00E3o e Rota\u00E7\u00E3o");
        addFloatField(panel, gbc, "Posi\u00E7\u00E3o X (cm)", piece.getX(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getX();
                executeEdit("Mover m\u00F3vel X", new Runnable() {
                    public void run() {
                        piece.setX(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setX(oldVal);
                    }
                });
            }
        });

        addFloatField(panel, gbc, "Posi\u00E7\u00E3o Y (cm)", piece.getY(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getY();
                executeEdit("Mover m\u00F3vel Y", new Runnable() {
                    public void run() {
                        piece.setY(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setY(oldVal);
                    }
                });
            }
        });

        addFloatField(panel, gbc, "Eleva\u00E7\u00E3o (cm)", piece.getElevation(), new FloatValueConsumer() {
            public void accept(final float val) {
                final float oldVal = piece.getElevation();
                executeEdit("Alterar eleva\u00E7\u00E3o do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setElevation(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setElevation(oldVal);
                    }
                });
            }
        });

        float degrees = (float) Math.toDegrees(piece.getAngle());
        addFloatField(panel, gbc, "\u00C2ngulo (\u00B0)", degrees, new FloatValueConsumer() {
            public void accept(float val) {
                final float newAngle = (float) Math.toRadians(val);
                final float oldAngle = piece.getAngle();
                executeEdit("Rotacionar m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setAngle(newAngle);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setAngle(oldAngle);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Apar\u00EAncia");
        addColorChooserField(panel, gbc, "Cor", piece.getColor(), new ColorConsumer() {
            public void accept(final Integer color) {
                final Integer oldColor = piece.getColor();
                executeEdit("Alterar cor do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setColor(color);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setColor(oldColor);
                    }
                });
            }
        });

        addCheckboxField(panel, gbc, "Vis\u00EDvel", piece.isVisible(), new BooleanConsumer() {
            public void accept(final boolean val) {
                final boolean oldVal = piece.isVisible();
                executeEdit("Alterar visibilidade do m\u00F3vel", new Runnable() {
                    public void run() {
                        piece.setVisible(val);
                    }
                }, new Runnable() {
                    public void run() {
                        piece.setVisible(oldVal);
                    }
                });
            }
        });

        contentContainer.add(panel);
    }

    private void buildRoomView(final Room room) {
        headerTitle.setText("C\u00F4modo: " + (room.getName() != null ? room.getName() : "Sem nome"));

        activeItemListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (activeItem == room) {
                            updateInspectorForSelection(home.getSelectedItems());
                        }
                    }
                });
            }
        };
        room.addPropertyChangeListener(activeItemListener);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Identifica\u00E7\u00E3o");
        addTextField(panel, gbc, "Nome", room.getName() != null ? room.getName() : "", new StringConsumer() {
            public void accept(final String val) {
                final String oldVal = room.getName();
                executeEdit("Renomear c\u00F4modo", new Runnable() {
                    public void run() {
                        room.setName(val.isEmpty() ? null : val);
                    }
                }, new Runnable() {
                    public void run() {
                        room.setName(oldVal);
                    }
                });
            }
        });

        addSectionHeader(panel, gbc, "Dimens\u00F5es");
        float areaSqMeters = room.getArea() / 10000f;
        addReadonlyField(panel, gbc, "\u00C1rea", String.format(Locale.US, "%.2f m\u00B2", areaSqMeters));

        addSectionHeader(panel, gbc, "Acabamento");
        addColorChooserField(panel, gbc, "Cor do Ch\u00E3o", room.getFloorColor(), new ColorConsumer() {
            public void accept(final Integer color) {
                final Integer oldColor = room.getFloorColor();
                executeEdit("Alterar cor do ch\u00E3o do c\u00F4modo", new Runnable() {
                    public void run() {
                        room.setFloorColor(color);
                    }
                }, new Runnable() {
                    public void run() {
                        room.setFloorColor(oldColor);
                    }
                });
            }
        });

        addColorChooserField(panel, gbc, "Cor do Teto", room.getCeilingColor(), new ColorConsumer() {
            public void accept(final Integer color) {
                final Integer oldColor = room.getCeilingColor();
                executeEdit("Alterar cor do teto do c\u00F4modo", new Runnable() {
                    public void run() {
                        room.setCeilingColor(color);
                    }
                }, new Runnable() {
                    public void run() {
                        room.setCeilingColor(oldColor);
                    }
                });
            }
        });

        contentContainer.add(panel);
    }

    private void buildDimensionLineView(final DimensionLine line) {
        headerTitle.setText("Cota / Dimens\u00E3o");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Dimens\u00E3o");
        addReadonlyField(panel, gbc, "Comprimento", String.format(Locale.US, "%.1f cm", line.getLength()));

        contentContainer.add(panel);
    }

    private void buildLabelView(final Label label) {
        headerTitle.setText("Texto / R\u00F3tulo");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Conte\u00FAdo");
        addTextField(panel, gbc, "Texto", label.getText(), new StringConsumer() {
            public void accept(final String val) {
                final String oldVal = label.getText();
                executeEdit("Alterar texto", new Runnable() {
                    public void run() {
                        label.setText(val);
                    }
                }, new Runnable() {
                    public void run() {
                        label.setText(oldVal);
                    }
                });
            }
        });

        contentContainer.add(panel);
    }

    private void buildGenericItemView(Selectable item) {
        headerTitle.setText("Elemento");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        GridBagConstraints gbc = createGbc();

        addSectionHeader(panel, gbc, "Informa\u00E7\u00F5es");
        addReadonlyField(panel, gbc, "Tipo", item.getClass().getSimpleName());

        contentContainer.add(panel);
    }

    // --- Componentes e Auxiliares de UI ---

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 0, 3, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        return gbc;
    }

    private void addSectionHeader(JPanel panel, GridBagConstraints gbc, String title) {
        if (gbc.gridy > 0) {
            gbc.insets = new Insets(10, 0, 4, 0);
            panel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
            gbc.gridy++;
        }
        gbc.insets = new Insets(6, 0, 4, 0);
        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(lbl, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(3, 0, 3, 0);
    }

    private void addReadonlyField(JPanel panel, GridBagConstraints gbc, String labelText, String valueText) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel lbl = new JLabel(labelText);
        JLabel val = new JLabel(valueText);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void addTextField(JPanel panel, GridBagConstraints gbc, String labelText, final String initialVal, final StringConsumer consumer) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel lbl = new JLabel(labelText);
        final JTextField txt = new JTextField(initialVal);
        txt.setColumns(10);
        txt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!txt.getText().equals(initialVal)) {
                    consumer.accept(txt.getText());
                }
            }
        });
        txt.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (!txt.getText().equals(initialVal)) {
                    consumer.accept(txt.getText());
                }
            }
        });

        row.add(lbl, BorderLayout.WEST);
        row.add(txt, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void addFloatField(JPanel panel, GridBagConstraints gbc, String labelText, final float initialVal, final FloatValueConsumer consumer) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel lbl = new JLabel(labelText);
        final JTextField txt = new JTextField(decimalFormat.format(initialVal));
        txt.setColumns(7);
        txt.setHorizontalAlignment(JTextField.RIGHT);

        ActionListener commitAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Number num = decimalFormat.parse(txt.getText().trim());
                    float parsed = num.floatValue();
                    if (Math.abs(parsed - initialVal) > 0.001f) {
                        consumer.accept(parsed);
                    }
                } catch (ParseException ex) {
                    txt.setText(decimalFormat.format(initialVal));
                }
            }
        };

        txt.addActionListener(commitAction);
        txt.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                try {
                    Number num = decimalFormat.parse(txt.getText().trim());
                    float parsed = num.floatValue();
                    if (Math.abs(parsed - initialVal) > 0.001f) {
                        consumer.accept(parsed);
                    }
                } catch (ParseException ex) {
                    txt.setText(decimalFormat.format(initialVal));
                }
            }
        });

        row.add(lbl, BorderLayout.WEST);
        row.add(txt, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void addCheckboxField(JPanel panel, GridBagConstraints gbc, String labelText, final boolean initialVal, final BooleanConsumer consumer) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel lbl = new JLabel(labelText);
        final JCheckBox chk = new JCheckBox("", initialVal);
        chk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                consumer.accept(chk.isSelected());
            }
        });

        row.add(lbl, BorderLayout.WEST);
        row.add(chk, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void addColorChooserField(JPanel panel, GridBagConstraints gbc, String labelText, final Integer initialColor, final ColorConsumer consumer) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel lbl = new JLabel(labelText);

        final JButton btnColor = new JButton();
        btnColor.setPreferredSize(new Dimension(28, 20));
        if (initialColor != null) {
            btnColor.setBackground(new Color(initialColor));
            btnColor.setText("");
        } else {
            btnColor.setText("Padr\u00E3o");
        }

        btnColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color current = initialColor != null ? new Color(initialColor) : Color.LIGHT_GRAY;
                Color chosen = JColorChooser.showDialog(PropertyInspectorPanel.this, "Escolher Cor - " + labelText, current);
                if (chosen != null) {
                    consumer.accept(chosen.getRGB());
                }
            }
        });

        row.add(lbl, BorderLayout.WEST);
        row.add(btnColor, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void executeEdit(final String presentationName, final Runnable redoTask, final Runnable undoTask) {
        redoTask.run();
        if (undoSupport != null) {
            undoSupport.postEdit(new AbstractUndoableEdit() {
                @Override
                public void undo() throws CannotUndoException {
                    super.undo();
                    undoTask.run();
                }

                @Override
                public void redo() throws CannotRedoException {
                    super.redo();
                    redoTask.run();
                }

                @Override
                public String getPresentationName() {
                    return presentationName;
                }
            });
        }
    }

    // --- Interfaces Funcionais Auxiliares ---

    private interface FloatValueConsumer {
        void accept(float value);
    }

    private interface StringConsumer {
        void accept(String value);
    }

    private interface BooleanConsumer {
        void accept(boolean value);
    }

    private interface ColorConsumer {
        void accept(Integer rgb);
    }
}


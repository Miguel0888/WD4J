package app.ui;

import app.dto.TestAction;
import wd4j.helper.RecorderService;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ActionTable extends JTable {
    private final ActionTableModel tableModel;
    private final JPopupMenu columnMenu;

    public ActionTable(ActionTableModel tableModel) {
        super(tableModel);
        this.tableModel = tableModel;

        // 🛠️ Spaltensteuerungs-Menü vorbereiten
        columnMenu = new JPopupMenu();
        configureColumns();
        setUpEditors();
    }

    /** 🔧 Konfiguriert die Spalten und fügt das Header-Menü hinzu */
    private void configureColumns() {
        TableColumnModel columnModel = getColumnModel();
        List<String> columnNames = Arrays.asList("Aktion", "Locator-Typ", "Selektor", "Wert", "Wartezeit");

        for (int i = 1; i < columnModel.getColumnCount(); i++) { // 0 ist die Checkbox-Spalte
            TableColumn column = columnModel.getColumn(i);
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(columnNames.get(i - 1), true);

            final int columnIndex = i;
            menuItem.addActionListener(e -> {
                if (menuItem.isSelected()) {
                    addColumn(column);
                } else {
                    removeColumn(column);
                }
            });

            columnMenu.add(menuItem);
        }

        // 🟢 Button im Header setzen für Spaltensteuerung
        columnModel.getColumn(0).setHeaderRenderer(new ButtonHeaderRenderer(columnMenu));
    }

    /** 🟢 Setzt die Spalteneditoren für DropDowns */
    private void setUpEditors() {
        TableColumnModel columnModel = getColumnModel();

        // 🟢 Checkbox-Editor setzen (Boolean-Werte)
        columnModel.getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        columnModel.getColumn(0).setCellRenderer(getDefaultRenderer(Boolean.class));

        // Aktionen DropDown
        JComboBox<String> actionComboBox = new JComboBox<>(new String[]{"click", "input", "screenshot"});
        columnModel.getColumn(1).setCellEditor(new DefaultCellEditor(actionComboBox));

        // Locator-Typen DropDown
        JComboBox<String> locatorTypeComboBox = new JComboBox<>(new String[]{"css", "xpath", "id", "text", "role", "label", "placeholder", "altText"});
        columnModel.getColumn(2).setCellEditor(new DefaultCellEditor(locatorTypeComboBox));

        // Selektor DropDown dynamisch befüllen
        JComboBox<String> selectorComboBox = new JComboBox<>();
        columnModel.getColumn(3).setCellEditor(new DefaultCellEditor(selectorComboBox));

        // Selektoren dynamisch nachladen
        getSelectionModel().addListSelectionListener(e -> {
            int row = getSelectedRow();
            if (row >= 0) {
                TestAction action = tableModel.getActions().get(row);
                List<String> suggestions = RecorderService.getInstance().getSelectorAlternatives(action.getSelectedSelector());
                selectorComboBox.removeAllItems();
                for (String suggestion : suggestions) {
                    selectorComboBox.addItem(suggestion);
                }
            }
        });

        columnModel.getColumn(4).setCellEditor(new DefaultCellEditor(new JTextField()));

        // Spaltenausrichtung anpassen (zentriert für "Wartezeit")
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        columnModel.getColumn(5).setCellRenderer(centerRenderer);

        // 🛠️ MouseListener für Klicks im Header hinzufügen
        JTableHeader header = getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int column = columnAtPoint(evt.getPoint());
                if (column == 0) { // Falls der Button-Header (Schraubenschlüssel) geklickt wurde
                    columnMenu.show(header, evt.getX(), evt.getY()); // Popup-Menü an Mausposition öffnen
                }
            }
        });
    }

    /** 🔧 Custom Renderer für den Header mit Button */
    static class ButtonHeaderRenderer extends JLabel implements TableCellRenderer {
        private final JPopupMenu columnMenu;

        public ButtonHeaderRenderer(JPopupMenu columnMenu) {
            this.columnMenu = columnMenu;
            setText("\uD83D\uDD27"); // 🔧 Schraubenschlüssel-Symbol
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBackground(new Color(230, 230, 230));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setToolTipText("Spalten anzeigen/ausblenden");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    public void updateTableStructure() {
        tableModel.updateColumnNames(); // 🔄 Zuerst die Spalten im Model aktualisieren
        setModel(tableModel); // 🚀 Model neu setzen, damit JTable sich aktualisiert

        // 🔥 Header mit 🔧 Schraubenschlüssel erneut setzen
        TableColumn firstColumn = getColumnModel().getColumn(0);
        firstColumn.setHeaderRenderer(new ButtonHeaderRenderer(columnMenu));
        firstColumn.setPreferredWidth(30);  // 🔥 Feste Breite für Checkbox-Spalte
        firstColumn.setMaxWidth(40);        // 🔥 Maximalbreite begrenzen
        firstColumn.setMinWidth(30);        // 🔥 Minimalbreite setzen
    }


}

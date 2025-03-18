package app.ui;

import app.dto.TestAction;
import wd4j.helper.RecorderService;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionTableModel extends AbstractTableModel {
    private final List<TestAction> actions = new ArrayList<>();
    private final String[] columnNames;

    public ActionTableModel(String[] columnNames) {
        this.columnNames = columnNames;
    }

    @Override
    public int getRowCount() {
        return actions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class; // Checkbox-Spalte als Boolean
        if (columnIndex == 4) return Integer.class; // Wartezeit als Integer
        return String.class; // Andere Spalten als String
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: return action.isSelected(); // Checkbox-Wert (Boolean)
            case 1: return action.getAction();
            case 2: return action.getLocatorType();
            case 3: return action.getSelectedSelector();
            case 4: return action.getValue();
            case 5: return action.getTimeout();
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: action.setSelected((Boolean) value); break; // Checkbox-Status setzen
            case 1: action.setAction((String) value); break;
            case 2: action.setLocatorType((String) value); break;
            case 3: action.setSelectedSelector((String) value); break;
            case 4: action.setValue((String) value); break;
            case 5: action.setTimeout((Integer) value); break;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public List<TestAction> getActions() {
        return actions;
    }

    public void addAction(TestAction action) {
        actions.add(action);
        fireTableRowsInserted(actions.size() - 1, actions.size() - 1);
    }

    public void removeAction(int rowIndex) {
        actions.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    /** 🟢 Setzt die Spalteneditoren für DropDowns */
    public void setUpEditors(JTable table) {
        // 🟢 Checkbox-Editor setzen (Boolean-Werte)
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        table.getColumnModel().getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));

        // Aktionen DropDown
        JComboBox<String> actionComboBox = new JComboBox<>(new String[]{"click", "input", "screenshot"});
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(actionComboBox));

        // Locator-Typen DropDown
        JComboBox<String> locatorTypeComboBox = new JComboBox<>(new String[]{"css", "xpath", "id", "text", "role", "label", "placeholder", "altText"});
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(locatorTypeComboBox));

        // Selektor DropDown dynamisch befüllen
        JComboBox<String> selectorComboBox = new JComboBox<>();
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(selectorComboBox));

        // Selektoren dynamisch nachladen
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                TestAction action = getActions().get(row);
                List<String> suggestions = RecorderService.getInstance().getSelectorAlternatives(action.getSelectedSelector());
                selectorComboBox.removeAllItems();
                for (String suggestion : suggestions) {
                    selectorComboBox.addItem(suggestion);
                }
            }
        });

        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JTextField()));

        // Spaltenausrichtung anpassen (zentriert für "Wartezeit")
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // 🛠️ Spaltensteuerungs-Menü vorbereiten
        TableColumnModel columnModel = table.getColumnModel();
        JPopupMenu columnMenu = new JPopupMenu();
        List<String> columnNames = Arrays.asList("Aktion", "Locator-Typ", "Selektor", "Wert", "Wartezeit");

        for (int i = 1; i < columnModel.getColumnCount(); i++) { // 0 ist die Checkbox-Spalte
            TableColumn column = columnModel.getColumn(i);
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(columnNames.get(i - 1), true);

            final int columnIndex = i;
            menuItem.addActionListener(e -> {
                if (menuItem.isSelected()) {
                    table.addColumn(column);
                } else {
                    table.removeColumn(column);
                }
            });

            columnMenu.add(menuItem);
        }

        // 🟢 Button im Header setzen für Spaltensteuerung
        columnModel.getColumn(0).setHeaderRenderer(new ButtonHeaderRenderer(columnMenu));
        TableColumn firstColumn = table.getColumnModel().getColumn(0);
        firstColumn.setPreferredWidth(30);  // 🔥 Setze die Breite auf 30 Pixel (anpassen, falls nötig)
        firstColumn.setMaxWidth(40);        // 🔥 Maximalbreite begrenzen
        firstColumn.setMinWidth(30);        // 🔥 Minimalbreite setzen

        // 🛠️ MouseListener für Klicks im Header hinzufügen
        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int column = table.columnAtPoint(evt.getPoint());
                if (column == 0) { // Falls der Button-Header geklickt wurde
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


    public void setRowData(List<TestAction> when) {
        actions.clear();
        actions.addAll(when);
        fireTableDataChanged();
    }
}

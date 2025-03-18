package app.ui;

import app.dto.TestAction;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ActionTableModel extends AbstractTableModel {
    private final List<TestAction> actions = new ArrayList<>();
    private List<String> columnNames;
    private JPopupMenu columnMenu;

    public ActionTableModel(List<String> columnNames) {
        this.columnNames = new ArrayList<>(columnNames);
    }

    void updateColumnNames() {
        Set<String> dynamicKeys = actions.stream()
                .filter(action -> action.getExtractedValues() != null)
                .flatMap(action -> action.getExtractedValues().keySet().stream())
                .distinct()
                .filter(key -> !columnNames.contains(key)) // Nur neue Keys hinzufügen
                .collect(Collectors.toSet());

        if (!dynamicKeys.isEmpty()) {
            List<String> updatedColumnNames = new ArrayList<>(columnNames);
            updatedColumnNames.addAll(dynamicKeys);
            columnNames = updatedColumnNames;
            fireTableStructureChanged(); // 🔄 Nur Datenmodell neu rendern
        }
    }

    @Override
    public int getRowCount() {
        return actions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
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
            default:  // Dynamische Spalten
                String dynamicKey = columnNames.get(columnIndex);
                if (action.getExtractedValues() != null) {
                    return action.getExtractedValues().getOrDefault(dynamicKey, ""); // Falls Key nicht existiert, leeren String zurückgeben
                }
                return ""; // Falls extractedValues null ist
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
            default:
                String dynamicKey = columnNames.get(columnIndex);
                action.getExtractedValues().put(dynamicKey, (String) value);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public List<TestAction> getActions() {
        return actions;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public void addAction(TestAction action) {
        System.out.println("Neue Aktion hinzugefügt: " + action); // Debugging-Ausgabe
        actions.add(action);
        updateColumnNames(); // 🔥 Neue Spalten prüfen und ggf. hinzufügen
        fireTableRowsInserted(actions.size() - 1, actions.size() - 1);
    }


    public void removeAction(int rowIndex) {
        actions.remove(rowIndex);
        updateColumnNames();
        fireTableRowsDeleted(rowIndex, rowIndex);
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
        updateColumnNames();
        fireTableDataChanged();
    }
}

package app.ui;

import app.model.TestAction;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.*;
import java.util.stream.Collectors;

public class ActionTableModel extends AbstractTableModel {
    private final List<TestAction> actions = new ArrayList<>();
    private final List<TableColumn> columns = new ArrayList<>();

    public ActionTableModel() {
        // 🔩 Erste Spalte ist der Schraubenschlüssel für Einstellungen
        addColumn("⚙");  // Symbol bleibt erhalten
        addColumn("Aktion");
        addColumn("Locator-Typ");
        addColumn("Selektor");
        addColumn("Wert");
        addColumn("Wartezeit");
    }

    /** 🛠 Spalte hinzufügen */
    private void addColumn(String name) {
        TableColumn column = new TableColumn(columns.size(), 75);
        column.setHeaderValue(name);
        columns.add(column);
    }

    /** 🔄 Aktualisiert Spalten mit dynamischen Werten */
    void updateColumnNames() {
        Set<String> dynamicKeys = actions.stream()
                .filter(action -> action.getExtractedValues() != null)
                .flatMap(action -> action.getExtractedValues().keySet().stream())
                .distinct()
                .filter(key -> columns.stream().noneMatch(col -> nameEquals(col, key)))
                .collect(Collectors.toSet());

        if (!dynamicKeys.isEmpty()) {
            dynamicKeys.forEach(this::addColumn);
            fireTableStructureChanged();
        }
    }

    private boolean nameEquals(TableColumn col, String key) {
        return Objects.equals(col.getHeaderValue(), key);
    }

    @Override
    public int getRowCount() {
        return actions.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return (String) columns.get(column).getHeaderValue();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: return action.isSelected();
            case 1: return action.getAction();
            case 2: return action.getLocatorType();
            case 3: return action.getSelectedSelector();
            case 4: return action.getValue();
            case 5: return action.getTimeout();
            default:  // Dynamische Spalten
                String dynamicKey = getColumnName(columnIndex);
                if (action.getExtractedValues() != null) {
                    return action.getExtractedValues().getOrDefault(dynamicKey, "");
                }
                return "";
        }
    }

    public List<TestAction> getActions() {
        return actions;
    }

    public void addAction(TestAction action) {
        actions.add(action);
        updateColumnNames();
        fireTableRowsInserted(actions.size() - 1, actions.size() - 1);
    }

    public void removeAction(int rowIndex) {
        actions.remove(rowIndex);
        updateColumnNames();
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public void setRowData(List<TestAction> when) {
        actions.clear();
        actions.addAll(when);
        updateColumnNames();
        fireTableDataChanged();
    }
}

package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;

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
        addColumn("Typ");
        addColumn("Aktion");
        addColumn("Locator-Typ");
        addColumn("Selektor");
        addColumn("Wert");
        addColumn("XPath");
        addColumn("CSS");
        addColumn("Element-ID");
        addColumn("CSS-Klassen");
        addColumn("Pagination");
        addColumn("Input-Name");
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
                .flatMap(action -> {
                    List<String> keys = new ArrayList<>();
                    if (action.getExtractedValues() != null) keys.addAll(action.getExtractedValues().keySet());
                    if (action.getExtractedAttributes() != null) keys.addAll(action.getExtractedAttributes().keySet());
                    if (action.getExtractedAriaRoles() != null) keys.addAll(action.getExtractedAriaRoles().keySet());
                    if (action.getExtractedTestIds() != null) keys.addAll(action.getExtractedTestIds().keySet());
                    return keys.stream();
                })
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
//        return columnIndex != 0;
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class; // Checkbox-Spalte
        if (columnIndex == 1) return TestAction.ActionType.class; // Dein Enum
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: return action.isSelected();
            case 1: return action.getType();
            case 2: return action.getAction();
            case 3: return action.getLocatorType();
            case 4: return action.getSelectedSelector();
            case 5: return action.getValue();
            case 6: return action.getLocators().getOrDefault("xpath", "");                  // ✅ XPath
            case 7: return action.getLocators().getOrDefault("css", "");                    // ✅ CSS
            case 8: return action.getExtractedAttributes().getOrDefault("elementId", "");   // ✅ Element-ID
            case 9: return action.getExtractedAttributes().getOrDefault("classes", "");     // ✅ CSS-Klassen
            case 10: return action.getExtractedAttributes().getOrDefault("pagination", "");  // ✅ Pagination
            case 11: return action.getExtractedAttributes().getOrDefault("inputName", "");  // ✅ Input-Name
            case 12: return action.getTimeout();
            default:  // Dynamische Spalten
                // Dynamische Spalten nach den festen Spalten (ab Index 11)
                String dynamicKey = getColumnName(columnIndex);

                // Prüfe dynamische Werte aus den verschiedenen Maps
                if (action.getExtractedValues() != null && action.getExtractedValues().containsKey(dynamicKey)) {
                    return action.getExtractedValues().get(dynamicKey);
                }
                if (action.getExtractedAttributes() != null && action.getExtractedAttributes().containsKey(dynamicKey)) {
                    return action.getExtractedAttributes().get(dynamicKey);
                }
                if (action.getExtractedAriaRoles() != null && action.getExtractedAriaRoles().containsKey(dynamicKey)) {
                    return action.getExtractedAriaRoles().get(dynamicKey);
                }
                if (action.getExtractedTestIds() != null && action.getExtractedTestIds().containsKey(dynamicKey)) {
                    return action.getExtractedTestIds().get(dynamicKey);
                }

                return "";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        TestAction action = actions.get(rowIndex);
        switch (columnIndex) {
            case 0: action.setSelected((Boolean) aValue); break;
            case 1: action.setType((TestAction.ActionType) aValue); break;
            case 2: action.setAction((String) aValue); break;
            case 3: action.setLocatorType((String) aValue); break;
            case 4: action.setSelectedSelector((String) aValue); break;
            case 5: action.setValue((String) aValue); break;
            case 6: action.getLocators().put("xpath", (String) aValue); break;
            case 7: action.getLocators().put("css", (String) aValue); break;
            case 8: action.getExtractedAttributes().put("elementId", (String) aValue); break;
            case 9: action.getExtractedAttributes().put("classes", (String) aValue); break;
            case 10: action.getExtractedAttributes().put("pagination", (String) aValue); break;
            case 11: action.getExtractedAttributes().put("inputName", (String) aValue); break;
            case 12: action.setTimeout(Integer.parseInt(aValue.toString())); break;
            default:
                String key = getColumnName(columnIndex);
                if (action.getExtractedValues().containsKey(key)) {
                    action.getExtractedValues().put(key, (String) aValue);
                } else if (action.getExtractedAttributes().containsKey(key)) {
                    action.getExtractedAttributes().put(key, (String) aValue);
                } else if (action.getExtractedAriaRoles().containsKey(key)) {
                    action.getExtractedAriaRoles().put(key, (String) aValue);
                } else if (action.getExtractedTestIds().containsKey(key)) {
                    action.getExtractedTestIds().put(key, (String) aValue);
                }
                break;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
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

    public void insertActionAt(int index, TestAction action) {
        actions.add(index, action);
        fireTableRowsInserted(index, index);
    }
}

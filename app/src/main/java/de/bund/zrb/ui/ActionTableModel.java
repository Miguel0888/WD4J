package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.*;
import java.util.stream.Collectors;

public class ActionTableModel extends AbstractTableModel {
    private final List<TestAction> actions = new ArrayList<>();
    private final List<TableColumn> columns = new ArrayList<>();

    // 🔑 Zentrales Mapping: interne Keys -> Überschrift
    private static final LinkedHashMap<String, String> FIXED_COLUMNS = new LinkedHashMap<>();
    static {
        FIXED_COLUMNS.put("elementId", "Element-ID");
        FIXED_COLUMNS.put("classes", "CSS-Klassen");
        FIXED_COLUMNS.put("pagination", "Pagination");
        FIXED_COLUMNS.put("inputName", "Input-Name");
    }

    public ActionTableModel() {
        // 🔩 Erste festen Spalten
        addColumn("⚙");
        addColumn("Typ");
        addColumn("Aktion");
        addColumn("Locator-Typ");
        addColumn("Selektor");
        addColumn("Wert");
        addColumn("XPath");
        addColumn("CSS");

        // 🔩 Mapping-Spalten aus FIXED_COLUMNS
        FIXED_COLUMNS.values().forEach(this::addColumn);

        addColumn("Wartezeit");
    }

    private void addColumn(String name) {
        TableColumn column = new TableColumn(columns.size(), 75);
        column.setHeaderValue(name);
        columns.add(column);
    }

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
                .filter(key -> !FIXED_COLUMNS.containsKey(key))
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
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class;
        if (columnIndex == 1) return TestAction.ActionType.class;
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
            case 6: return action.getLocators().getOrDefault("xpath", "");
            case 7: return action.getLocators().getOrDefault("css", "");
            default:
                int mappingStart = 8;
                int mappingEnd = mappingStart + FIXED_COLUMNS.size();
                if (columnIndex >= mappingStart && columnIndex < mappingEnd) {
                    String key = getFixedKeyByIndex(columnIndex - mappingStart);
                    return action.getExtractedAttributes().getOrDefault(key, "");
                } else if (columnIndex == mappingEnd) {
                    return action.getTimeout();
                } else {
                    String dynamicKey = getColumnName(columnIndex);
                    if (action.getExtractedValues().containsKey(dynamicKey)) return action.getExtractedValues().get(dynamicKey);
                    if (action.getExtractedAttributes().containsKey(dynamicKey)) return action.getExtractedAttributes().get(dynamicKey);
                    if (action.getExtractedAriaRoles().containsKey(dynamicKey)) return action.getExtractedAriaRoles().get(dynamicKey);
                    if (action.getExtractedTestIds().containsKey(dynamicKey)) return action.getExtractedTestIds().get(dynamicKey);
                    return "";
                }
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
            default:
                int mappingStart = 8;
                int mappingEnd = mappingStart + FIXED_COLUMNS.size();
                if (columnIndex >= mappingStart && columnIndex < mappingEnd) {
                    String key = getFixedKeyByIndex(columnIndex - mappingStart);
                    action.getExtractedAttributes().put(key, (String) aValue);
                } else if (columnIndex == mappingEnd) {
                    action.setTimeout(Integer.parseInt(aValue.toString()));
                } else {
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
                }
                break;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    private String getFixedKeyByIndex(int index) {
        return new ArrayList<>(FIXED_COLUMNS.keySet()).get(index);
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
        updateColumnNames();
        fireTableRowsInserted(index, index);
    }
}

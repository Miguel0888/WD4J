package de.bund.zrb.ui.giveneditor;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TableModel for After/Expectation assertions:
 * Columns: [0] Enabled(Boolean) | [1] Name(String) | [2] Expression(String) | [3] ValidatorType(String) | [4] ValidatorValue(String) | [5] Description(String)
 *
 * Keeps parallel backings:
 * - expressions:   Map<name, expr>
 * - enabledFlags:  Map<name, Boolean>
 * - validatorTypes: Map<name, validatorType>
 * - validatorValues: Map<name, validatorValue>
 * - descriptions:  Map<name, desc>
 *
 * Supports optional pinned first row (name + expr locked; other cells editable except name/expr).
 */
public class AssertionTableModel extends AbstractTableModel {

    private final Map<String,String> expressions;
    private final Map<String,Boolean> enabledFlags;
    private final Map<String,String> descriptions;
    private final Map<String,String> validatorTypes;
    private final Map<String,String> validatorValues;

    private final List<String> keys = new ArrayList<String>();

    private final boolean includePinnedRow;
    private final String pinnedKey;

    public AssertionTableModel(Map<String,String> expressions,
                               Map<String,Boolean> enabledFlags,
                               Map<String,String> descriptions,
                               Map<String,String> validatorTypes,
                               Map<String,String> validatorValues,
                               boolean includePinnedRow,
                               String pinnedKey) {
        this.expressions = (expressions != null) ? expressions : new LinkedHashMap<String,String>();
        this.enabledFlags = (enabledFlags != null) ? enabledFlags : new LinkedHashMap<String,Boolean>();
        this.descriptions = (descriptions != null) ? descriptions : new LinkedHashMap<String,String>();
        this.validatorTypes = (validatorTypes != null) ? validatorTypes : new LinkedHashMap<String,String>();
        this.validatorValues = (validatorValues != null) ? validatorValues : new LinkedHashMap<String,String>();
        this.includePinnedRow = includePinnedRow;
        this.pinnedKey = pinnedKey;

        // Normalize enabled + description + validator entries
        for (String k : this.expressions.keySet()) {
            if (!this.enabledFlags.containsKey(k)) this.enabledFlags.put(k, Boolean.TRUE);
            if (!this.descriptions.containsKey(k)) this.descriptions.put(k, "");
            if (!this.validatorTypes.containsKey(k)) this.validatorTypes.put(k, ""); // empty => legacy boolean mode
            if (!this.validatorValues.containsKey(k)) this.validatorValues.put(k, "");
        }

        // Build ordered keys (pinned first if present)
        keys.addAll(this.expressions.keySet());
        if (includePinnedRow && pinnedKey != null) {
            int idx = keys.indexOf(pinnedKey);
            if (idx >= 0) {
                keys.remove(idx);
            }
            keys.add(0, pinnedKey);
        }
    }

    @Override public int getRowCount() { return keys.size(); }
    @Override public int getColumnCount() { return 6; }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0: return "Enabled";
            case 1: return "Name";
            case 2: return "Expression";
            case 3: return "ValidatorType";
            case 4: return "ValidatorValue";
            case 5: return "Description";
            default: return super.getColumnName(column);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) return Boolean.class;
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);
        switch (columnIndex) {
            case 0: return asBool(enabledFlags.get(key));
            case 1: return key;
            case 2: return expressions.get(key);
            case 3: return validatorTypes.get(key);
            case 4: return validatorValues.get(key);
            case 5: return descriptions.get(key);
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        boolean pinned = includePinnedRow && rowIndex == 0 && keyEqualsPinned(rowIndex);
        if (columnIndex == 0) return true;               // Enabled always editable
        if (columnIndex == 1) return !pinned;            // Name locked if pinned
        if (columnIndex == 2) return !pinned;            // Expr locked if pinned
        if (columnIndex == 3) return true;               // ValidatorType always editable (even pinned)
        if (columnIndex == 4) return true;               // ValidatorValue always editable
        if (columnIndex == 5) return true;               // Description always editable
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String oldKey = keys.get(rowIndex);
        String sval = (aValue == null) ? "" : String.valueOf(aValue);

        switch (columnIndex) {
            case 0: // Enabled
                enabledFlags.put(oldKey, asBool(aValue));
                break;

            case 1: { // Name (rename)
                if (includePinnedRow && rowIndex == 0 && keyEqualsPinned(rowIndex)) return;

                String newKey = sval.trim();
                if (newKey.length() == 0 || newKey.equals(oldKey)) break;
                if (expressions.containsKey(newKey)) break; // do not allow duplicates

                // Move expression
                String expr = expressions.remove(oldKey);
                expressions.put(newKey, expr);

                // Move enabled
                Boolean en = enabledFlags.remove(oldKey);
                enabledFlags.put(newKey, asBool(en));

                // Move description
                String desc = descriptions.remove(oldKey);
                descriptions.put(newKey, desc != null ? desc : "");

                // Move validator type/value
                String vt = validatorTypes.remove(oldKey);
                validatorTypes.put(newKey, vt != null ? vt : "");
                String vv = validatorValues.remove(oldKey);
                validatorValues.put(newKey, vv != null ? vv : "");

                keys.set(rowIndex, newKey);
                break;
            }

            case 2: // Expression
                expressions.put(oldKey, sval);
                break;
            case 3: // ValidatorType
                validatorTypes.put(oldKey, sval);
                break;
            case 4: // ValidatorValue
                validatorValues.put(oldKey, sval);
                break;
            case 5: // Description
                descriptions.put(oldKey, sval);
                break;
        }

        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void addEmptyRow() {
        String base = "assertion";
        String cand = base;
        int i = 2;
        while (expressions.containsKey(cand)) {
            cand = base + "_" + i;
            i++;
        }
        expressions.put(cand, "");
        enabledFlags.put(cand, Boolean.TRUE);
        descriptions.put(cand, "");
        validatorTypes.put(cand, "");
        validatorValues.put(cand, "");

        int insertIndex = includePinnedRow ? 1 : keys.size();
        keys.add(insertIndex, cand);
        fireTableRowsInserted(insertIndex, insertIndex);
    }

    public void removeRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= keys.size()) return;
        if (!canRemoveRow(rowIndex)) return;

        String k = keys.remove(rowIndex);
        expressions.remove(k);
        enabledFlags.remove(k);
        descriptions.remove(k);
        validatorTypes.remove(k);
        validatorValues.remove(k);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public boolean canRemoveRow(int rowIndex) {
        return !(includePinnedRow && rowIndex == 0 && keyEqualsPinned(rowIndex));
    }

    // --- helpers ---

    private boolean keyEqualsPinned(int rowIndex) {
        String k = keys.get(rowIndex);
        return (pinnedKey != null && pinnedKey.equals(k));
    }

    private static boolean asBool(Object v) {
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v == null) return false;
        String s = String.valueOf(v).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
    }
}

package de.bund.zrb.ui.util;

import javax.swing.*;
import java.util.StringJoiner;

/** Build text for different table copy scopes. */
public final class TableCopyBuilder {
    private final JTable table;
    private final CellStringProvider provider;

    public TableCopyBuilder(JTable table, CellStringProvider provider) {
        if (table == null) throw new IllegalArgumentException("table must not be null");
        this.table = table;
        this.provider = (provider == null) ? CellStringProvider.DEFAULT : provider;
    }

    /** Build text for a single cell. */
    public String buildCell(int row, int col) {
        return provider.toCellString(table, row, col);
    }

    /** Build tab-separated row (from all visible columns). */
    public String buildRow(int row) {
        StringJoiner sj = new StringJoiner("\t");
        for (int c = 0; c < table.getColumnCount(); c++) {
            sj.add(provider.toCellString(table, row, c));
        }
        return sj.toString();
    }

    /** Build newline-separated column (from all visible rows). */
    public String buildColumn(int col) {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (int r = 0; r < table.getRowCount(); r++) {
            sj.add(provider.toCellString(table, r, col));
        }
        return sj.toString();
    }

    /** Build TSV for current selection; falls back to complete table if nothing selected. */
    public String buildSelectionOrAll() {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();

        if (rows != null && rows.length > 0 && cols != null && cols.length > 0) {
            StringBuilder out = new StringBuilder();
            for (int ri = 0; ri < rows.length; ri++) {
                int r = rows[ri];
                for (int ci = 0; ci < cols.length; ci++) {
                    int c = cols[ci];
                    out.append(provider.toCellString(table, r, c));
                    if (ci < cols.length - 1) out.append('\t');
                }
                if (ri < rows.length - 1) out.append(System.lineSeparator());
            }
            return out.toString();
        }

        // Fallback: copy all (visible) as TSV
        StringBuilder all = new StringBuilder();
        for (int r = 0; r < table.getRowCount(); r++) {
            all.append(buildRow(r));
            if (r < table.getRowCount() - 1) all.append(System.lineSeparator());
        }
        return all.toString();
    }
}

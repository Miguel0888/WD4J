package de.bund.zrb.ui.util;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;

/** Install right-click context menu for copy actions on a JTable. */
public final class TableCopyContextMenu {

    // Store context cell (view indices) at menu invocation time
    private static final String CTX_CELL_PROP = "de.bund.zrb.ui.util.TableCopyContextMenu.ctxCell";

    /** Install context menu with copy actions; use default cell string provider. */
    public static void install(final JTable table) {
        install(table, CellStringProvider.DEFAULT);
    }

    /** Install context menu with copy actions and custom cell string provider. */
    public static void install(final JTable table, final CellStringProvider provider) {
        final TableCopyBuilder builder = new TableCopyBuilder(table, provider);
        final JPopupMenu menu = new JPopupMenu();

        final JMenuItem miCell       = new JMenuItem("Zelle kopieren");
        final JMenuItem miCellRx     = new JMenuItem("Zelle kopieren (RegEx)");
        final JMenuItem miCellRxAdd  = new JMenuItem("Zelle (RegEx) → Registry…");
        final JMenuItem miRow        = new JMenuItem("Zeile kopieren (TSV)");
        final JMenuItem miCol        = new JMenuItem("Spalte kopieren (Zeilen)");
        final JMenuItem miSelTSV     = new JMenuItem("Auswahl kopieren (TSV)");
        final JMenuItem miAllTSV     = new JMenuItem("Alles kopieren (TSV)");

        // ---- Actions (resolve cell from stored context; fallback to selection)

        miCell.addActionListener(e -> {
            Cell vc = resolveContextCell(table);
            if (!vc.isValid()) {
                beep(table);
                return;
            }
            ClipboardSupport.putString(builder.buildCell(vc.row, vc.col));
        });

        miCellRx.addActionListener(e -> {
            Cell vc = resolveContextCell(table);
            if (!vc.isValid()) {
                beep(table);
                return;
            }
            String cell = builder.buildCell(vc.row, vc.col);
            String rx = RegexFromTextBuilder.buildAnchoredRegex(cell);
            ClipboardSupport.putString(rx);
        });

        miCellRxAdd.addActionListener(e -> {
            Cell vc = resolveContextCell(table);
            if (!vc.isValid()) {
                beep(table);
                return;
            }
            String cell = builder.buildCell(vc.row, vc.col);
            String rx = RegexFromTextBuilder.buildAnchoredRegex(cell);

            RegexAddDialog.Decision d = RegexAddDialog.ask(table, rx, Integer.valueOf(vc.col));
            if (!d.confirmed) return;

            RegexRegistryFacade.Result res = RegexRegistryFacade.addRegex(d.target, rx);
            if (res == RegexRegistryFacade.Result.ADDED) {
                JOptionPane.showMessageDialog(table, "RegEx wurde hinzugefügt.", "Registry", JOptionPane.INFORMATION_MESSAGE);
            } else if (res == RegexRegistryFacade.Result.DUPLICATE) {
                JOptionPane.showMessageDialog(table, "RegEx existiert bereits in der gewählten Liste.", "Registry", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(table, "Leerer RegEx – nichts hinzugefügt.", "Registry", JOptionPane.WARNING_MESSAGE);
            }
        });

        miRow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { beep(table); return; }
            ClipboardSupport.putString(builder.buildRow(row));
        });

        miCol.addActionListener(e -> {
            int col = table.getSelectedColumn();
            if (col < 0) { beep(table); return; }
            ClipboardSupport.putString(builder.buildColumn(col));
        });

        miSelTSV.addActionListener(e -> ClipboardSupport.putString(builder.buildSelectionOrAll()));

        miAllTSV.addActionListener(e -> {
            // Copy full table as TSV irrespective of selection
            boolean hadSelection = table.getSelectedRowCount() > 0 || table.getSelectedColumnCount() > 0;
            int[] rows = table.getSelectedRows();
            int[] cols = table.getSelectedColumns();
            table.clearSelection();
            ClipboardSupport.putString(builder.buildSelectionOrAll());
            // Optional: restore selection if you prefer
            if (hadSelection) {
                restoreSelection(table, rows, cols);
            }
        });

        menu.add(miCell);
        menu.add(miCellRx);
        menu.add(miCellRxAdd);
        menu.addSeparator();
        menu.add(miRow);
        menu.add(miCol);
        menu.addSeparator();
        menu.add(miSelTSV);
        menu.add(miAllTSV);

        // ---- Show menu on platform trigger: remember cell and enforce selection
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }

            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                Point p = e.getPoint();
                int r = table.rowAtPoint(p);
                int c = table.columnAtPoint(p);
                if (r >= 0 && c >= 0) {
                    // Remember invocation cell in view coords
                    storeContextCell(table, r, c);

                    // Make that cell selected (row + column)
                    ensureCellSelection(table, r, c);
                } else {
                    // Clear stored context if not on a cell
                    storeContextCell(table, -1, -1);
                }

                table.requestFocusInWindow();
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // ---- Keyboard: Ctrl+C copies selection as TSV (Excel-like)
        KeyStroke copyKs = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(copyKs, "copy-tsv");
        table.getActionMap().put("copy-tsv", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                ClipboardSupport.putString(builder.buildSelectionOrAll());
            }
        });

        // ---- Keyboard: open context menu via ContextMenu key or Shift+F10
        KeyStroke ctx1 = KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0);
        KeyStroke ctx2 = KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(ctx1, "open-menu");
        table.getInputMap(JComponent.WHEN_FOCUSED).put(ctx2, "open-menu");
        table.getActionMap().put("open-menu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                int r = table.getSelectionModel().getLeadSelectionIndex();
                int c = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
                if (r < 0 || c < 0) {
                    r = table.getSelectedRow();
                    c = table.getSelectedColumn();
                }
                if (r < 0) r = 0;
                if (c < 0) c = 0;

                Rectangle cellRect = table.getCellRect(r, c, true);
                Point pt = new Point(cellRect.x + cellRect.width / 2, cellRect.y + cellRect.height / 2);
                storeContextCell(table, r, c);
                ensureCellSelection(table, r, c);
                menu.show(table, pt.x, pt.y);
            }
        });
    }

    // ---- Helpers

    /** Ensure cell selection is enabled and select given cell. */
    private static void ensureCellSelection(JTable table, int row, int col) {
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);

        if (!table.isRowSelected(row)) {
            table.getSelectionModel().setSelectionInterval(row, row);
        }
        ListSelectionModel colSel = table.getColumnModel().getSelectionModel();
        if (!colSel.isSelectedIndex(col)) {
            colSel.setSelectionInterval(col, col);
        }
    }

    /** Store invocation cell (view indices) on the table. */
    private static void storeContextCell(JTable table, int row, int col) {
        table.putClientProperty(CTX_CELL_PROP, new Cell(row, col));
    }

    /** Resolve invocation cell from client property; fallback to current selection. */
    private static Cell resolveContextCell(JTable table) {
        Object o = table.getClientProperty(CTX_CELL_PROP);
        if (o instanceof Cell) {
            Cell vc = (Cell) o;
            if (vc.isValid()) return vc;
        }
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();
        return new Cell(r, c);
    }

    private static void restoreSelection(JTable table, int[] rows, int[] cols) {
        if (rows != null && rows.length > 0) {
            table.getSelectionModel().setValueIsAdjusting(true);
            table.clearSelection();
            for (int r : rows) table.addRowSelectionInterval(r, r);
            table.getSelectionModel().setValueIsAdjusting(false);
        }
        if (cols != null && cols.length > 0) {
            ListSelectionModel cm = table.getColumnModel().getSelectionModel();
            cm.setValueIsAdjusting(true);
            cm.clearSelection();
            for (int c : cols) cm.addSelectionInterval(c, c);
            cm.setValueIsAdjusting(false);
        }
    }

    private static void beep(Component c) {
        Toolkit.getDefaultToolkit().beep();
        // Optionally show a lightweight hint – keep it quiet to avoid noise
        // JOptionPane.showMessageDialog(c, "Keine Zelle ausgewählt.", "Hinweis", JOptionPane.WARNING_MESSAGE);
    }

    /** Value object for a view cell coordinate. */
    private static final class Cell {
        final int row;
        final int col;
        Cell(int row, int col) { this.row = row; this.col = col; }
        boolean isValid() { return row >= 0 && col >= 0; }
    }

    private TableCopyContextMenu() { }
}

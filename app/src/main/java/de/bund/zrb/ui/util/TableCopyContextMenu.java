package de.bund.zrb.ui.util;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;

/**
 * Install right-click context menu for copy actions on a JTable.
 */
public final class TableCopyContextMenu {

    /**
     * Install context menu with copy actions; use default cell string provider.
     */
    public static void install(final JTable table) {
        install(table, CellStringProvider.DEFAULT);
    }

    /**
     * Install context menu with copy actions and custom cell string provider.
     */
    public static void install(final JTable table, final CellStringProvider provider) {
        final TableCopyBuilder builder = new TableCopyBuilder(table, provider);
        final JPopupMenu menu = new JPopupMenu();

        final JMenuItem miCell = new JMenuItem("Zelle kopieren");
        final JMenuItem miCellRx = new JMenuItem("Zelle kopieren (RegEx)");
        final JMenuItem miCellRxAdd = new JMenuItem("Zelle (RegEx) → Registry…"); // <— neu
        final JMenuItem miRow = new JMenuItem("Zeile kopieren (TSV)");
        final JMenuItem miCol = new JMenuItem("Spalte kopieren (Zeilen)");
        final JMenuItem miSelTSV = new JMenuItem("Auswahl kopieren (TSV)");
        final JMenuItem miAllTSV = new JMenuItem("Alles kopieren (TSV)");

        miCell.addActionListener(e -> {
            Point p = table.getMousePosition();
            int row = (p != null) ? table.rowAtPoint(p) : table.getSelectedRow();
            int col = (p != null) ? table.columnAtPoint(p) : table.getSelectedColumn();
            if (row >= 0 && col >= 0) {
                ClipboardSupport.putString(builder.buildCell(row, col));
            }
        });

        miCellRx.addActionListener(e -> {
            Point p = table.getMousePosition();
            int row = (p != null) ? table.rowAtPoint(p) : table.getSelectedRow();
            int col = (p != null) ? table.columnAtPoint(p) : table.getSelectedColumn();
            if (row >= 0 && col >= 0) {
                String cell = builder.buildCell(row, col);
                String rx = RegexFromTextBuilder.buildAnchoredRegex(cell);
                ClipboardSupport.putString(rx);
            }
        });

        // --- RegEx direkt in Registry ablegen (mit Dialog)
        miCellRxAdd.addActionListener(e -> {
            Point p = table.getMousePosition();
            int row = (p != null) ? table.rowAtPoint(p) : table.getSelectedRow();
            int col = (p != null) ? table.columnAtPoint(p) : table.getSelectedColumn();
            if (row < 0 || col < 0) return;

            String cell = builder.buildCell(row, col);
            String rx = RegexFromTextBuilder.buildAnchoredRegex(cell);

            RegexAddDialog.Decision d = RegexAddDialog.ask(table, rx, Integer.valueOf(col));
            if (!d.confirmed) return;

            RegexRegistryFacade.Result res = RegexRegistryFacade.addRegex(d.target, rx);
            if (res == RegexRegistryFacade.Result.ADDED) {
                JOptionPane.showMessageDialog(
                        table,
                        "RegEx wurde hinzugefügt.",
                        "Registry",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else if (res == RegexRegistryFacade.Result.DUPLICATE) {
                JOptionPane.showMessageDialog(
                        table,
                        "RegEx existiert bereits in der gewählten Liste.",
                        "Registry",
                        JOptionPane.WARNING_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        table,
                        "Leerer RegEx – nichts hinzugefügt.",
                        "Registry",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        miRow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) ClipboardSupport.putString(builder.buildRow(row));
        });
        miCol.addActionListener(e -> {
            int col = table.getSelectedColumn();
            if (col >= 0) ClipboardSupport.putString(builder.buildColumn(col));
        });
        miSelTSV.addActionListener(e -> ClipboardSupport.putString(builder.buildSelectionOrAll()));
        miAllTSV.addActionListener(e -> {
            table.clearSelection();
            ClipboardSupport.putString(builder.buildSelectionOrAll());
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

        // Show menu on platform-appropriate trigger
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int r = table.rowAtPoint(e.getPoint());
                    int c = table.columnAtPoint(e.getPoint());
                    if (r >= 0 && c >= 0) {
                        if (!table.isRowSelected(r)) table.getSelectionModel().setSelectionInterval(r, r);
                        if (!table.getColumnModel().getSelectionModel().isSelectedIndex(c)) {
                            table.getColumnModel().getSelectionModel().setSelectionInterval(c, c);
                        }
                    }
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // Keyboard: Ctrl+C → copy selection TSV (wie Excel)
        KeyStroke copyKs = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK);
        table.getInputMap(JComponent.WHEN_FOCUSED).put(copyKs, "copy-tsv");
        table.getActionMap().put("copy-tsv", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ClipboardSupport.putString(builder.buildSelectionOrAll());
            }
        });
    }

    private TableCopyContextMenu() {
    }
}

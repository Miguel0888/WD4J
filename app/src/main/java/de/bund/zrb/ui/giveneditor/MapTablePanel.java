// src/main/java/de/bund/zrb/ui/giveneditor/MapTablePanel.java
package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;

public class MapTablePanel extends JPanel {

    public MapTablePanel(final Map<String,String> backing, final String scopeName) {
        super(new BorderLayout());

        final MapTableModel model = new MapTableModel(backing);
        final JTable table = new JTable(model);

        // Table UX
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Ensure at least 3-line row height
        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));

        // Column sizing for Expression
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(480);
        }

        // ---- Suppliers verdrahten ----
        // TODO: Hier echte Quellen einhängen:
        //  - varSupplier: Liste aller Variablennamen
        //  - fnSupplier:  Map<Funktionsname, DescribedItem> (getDescription() liefert Beschreibung)
        //  - rxSupplier:  Map<RegexName, DescribedItem>     (getDescription() liefert Beschreibung)
        java.util.function.Supplier<java.util.List<String>> varSupplier =
                new java.util.function.Supplier<java.util.List<String>>() {
                    @Override public java.util.List<String> get() {
                        return java.util.Collections.<String>emptyList();
                    }
                };

        java.util.function.Supplier<java.util.Map<String, DescribedItem>> fnSupplier =
                new java.util.function.Supplier<java.util.Map<String, DescribedItem>>() {
                    @Override public java.util.Map<String, DescribedItem> get() {
                        return java.util.Collections.<String, DescribedItem>emptyMap();
                    }
                };

        java.util.function.Supplier<java.util.Map<String, DescribedItem>> rxSupplier =
                new java.util.function.Supplier<java.util.Map<String, DescribedItem>>() {
                    @Override public java.util.Map<String, DescribedItem> get() {
                        return java.util.Collections.<String, DescribedItem>emptyMap();
                    }
                };

        // Attach ExpressionCellEditor to Expression column (index 1)
        ExpressionCellEditor exprEditor = new ExpressionCellEditor(varSupplier, fnSupplier, rxSupplier);
        table.getColumnModel().getColumn(1).setCellEditor(exprEditor);

        // Lightweight 3-line monospaced renderer
        table.getColumnModel().getColumn(1).setCellRenderer(new MultiLineMonoRenderer());

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                model.addEmptyRow();
            }
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    model.removeRow(row);
                }
            }
        });

        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                int exprCol = 1;
                table.editCellAt(row, exprCol);
                Component editorComp = table.getEditorComponent();
                if (editorComp != null) {
                    editorComp.requestFocusInWindow();
                }
            }
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(
                        MapTablePanel.this,
                        "Gespeichert.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** Lightweight multi-line monospaced renderer to display ~3 lines. */
    static final class MultiLineMonoRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        MultiLineMonoRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false); // keep code-ish wrapping
            setOpaque(true);
            setRows(3); // hint three lines
            setBorder(null);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            // Keep row height consistent with renderer preferred height if needed
            int fmH = getFontMetrics(getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) {
                table.setRowHeight(desired);
            }
            return this;
        }
    }
}

package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Show assertions as rows with: [Enabled ✓] | Name | Expression
 * - Row 0 can be pinned (name + value locked, enabled editable).
 * - Name/Expression editieren wie bei MapTablePanel (ExpressionCellEditor).
 */
public class AssertionTablePanel extends JPanel {

    public AssertionTablePanel(final Map<String, String> backingExpressions,
                               final Map<String, Boolean> backingEnabled,
                               final String scopeName,
                               final String pinnedKey,
                               final String pinnedValue) {
        super(new BorderLayout());

        final boolean includePinnedRow = (pinnedKey != null);

        // Ensure pinned exists
        boolean needImmediateSave = false;
        if (includePinnedRow && !backingExpressions.containsKey(pinnedKey)) {
            backingExpressions.put(pinnedKey, pinnedValue != null ? pinnedValue : "");
            backingEnabled.put(pinnedKey, Boolean.TRUE);
            needImmediateSave = true;
        }

        final AssertionTableModel model =
                new AssertionTableModel(backingExpressions, backingEnabled, includePinnedRow, pinnedKey);

        final JTable table = new JTable(model) {

            private final ExpressionCellEditor exprEditor =
                    new ExpressionCellEditor(
                            MapTablePanelFactories.varSupplierForAssertions(),
                            MapTablePanelFactories.fnSupplier(),
                            MapTablePanelFactories.rxSupplier()
                    );

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Enabled checkbox uses default editor
                if (column == 0) return super.getCellEditor(row, column);

                // Name column: lock when pinned
                if (column == 1 && includePinnedRow && row == 0) return null;

                // Expression column: lock when pinned
                if (column == 2 && includePinnedRow && row == 0) return null;

                // Expression editor for column 2
                if (column == 2) return exprEditor;

                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                // Pinned row: lock look (name + expression)
                if (includePinnedRow && row == 0) {
                    if (column == 1) return new LockedGrayLabelRenderer();
                    if (column == 2) return new LockedGrayMonoRenderer();
                }
                // Normal expression cells monospaced (reuse your renderer)
                if (column == 2) {
                    return new MapTablePanel.MultiLineMonoRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };

        // UX
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));
        if (table.getColumnModel().getColumnCount() > 2) {
            table.getColumnModel().getColumn(2).setPreferredWidth(500);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " After-Assertion hinzufügen");
        addBtn.addActionListener(e -> model.addEmptyRow());

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                if (!model.canRemoveRow(row)) {
                    JOptionPane.showMessageDialog(
                            AssertionTablePanel.this,
                            "Gepinnte Assertion kann nicht gelöscht werden.",
                            "Hinweis",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                model.removeRow(row);
            }
        });

        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            table.editCellAt(row, 2);
            Component editorComp = table.getEditorComponent();
            if (editorComp != null) editorComp.requestFocusInWindow();
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> TestRegistry.getInstance().save());

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }
    }

    // ---------------- Renderers (lokal, keine Fremdabhängigkeit) ----------------

    /** Render lock look for pinned Name (gray + italic, JLabel-basierend). */
    static final class LockedGrayLabelRenderer extends JLabel implements TableCellRenderer {
        LockedGrayLabelRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            setFont(table.getFont().deriveFont(Font.ITALIC));
            setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    /** Render lock look for pinned Expression (gray + monospaced, JTextArea). */
    static final class LockedGrayMonoRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.ITALIC, 12);
        LockedGrayMonoRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false);
            setOpaque(true);
            setRows(3);
            setBorder(null);
            setEditable(false);
            setEnabled(false);
        }
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(Color.GRAY);
                setBackground(table.getBackground());
            }
            int fmH = getFontMetrics(getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) {
                table.setRowHeight(desired);
            }
            return this;
        }
    }
}

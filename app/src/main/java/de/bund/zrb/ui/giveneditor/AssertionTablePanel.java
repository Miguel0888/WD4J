package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Assertions table UI with four columns:
 * [Enabled ✓] | Name | Expression | Description
 *
 * Behavior:
 * - Pinned row (index 0 when pinnedKey != null):
 *   - Enabled: editable (allow disable default/pinned assertion)
 *   - Name:    locked (gray/italic)
 *   - Expr:    locked (gray/italic)
 *   - Desc:    editable (manager-friendly text)
 * - Expression column uses ExpressionCellEditor (same UX as MapTablePanel).
 * - Description is a plain editable text cell (default editor).
 */
public class AssertionTablePanel extends JPanel {

    public AssertionTablePanel(final Map<String, String> backingExpressions,
                               final Map<String, Boolean> backingEnabled,
                               final Map<String, String> backingDescriptions, // NEW
                               final String scopeName,
                               final String pinnedKey,
                               final String pinnedValue) {
        super(new BorderLayout());

        final boolean includePinnedRow = (pinnedKey != null);

        // Ensure pinned exists
        boolean needImmediateSave = false;
        if (includePinnedRow && !backingExpressions.containsKey(pinnedKey)) {
            backingExpressions.put(pinnedKey, pinnedValue != null ? pinnedValue : "");
            if (backingEnabled != null) backingEnabled.put(pinnedKey, Boolean.TRUE);
            if (backingDescriptions != null && !backingDescriptions.containsKey(pinnedKey)) {
                backingDescriptions.put(pinnedKey, ""); // allow editable description
            }
            needImmediateSave = true;
        }

        // Fallback if null provided
        final Map<String,String> descMap = (backingDescriptions != null)
                ? backingDescriptions
                : new LinkedHashMap<String,String>();

        final AssertionTableModel model =
                new AssertionTableModel(backingExpressions, backingEnabled, descMap, includePinnedRow, pinnedKey);

        final JTable table = new JTable(model) {

            private final ExpressionCellEditor exprEditor =
                    new ExpressionCellEditor(
                            MapTablePanelFactories.varSupplierForAssertions(),
                            MapTablePanelFactories.fnSupplier(),
                            MapTablePanelFactories.rxSupplier()
                    );

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Column 0: Enabled -> default boolean editor
                if (column == 0) return super.getCellEditor(row, column);

                // Column 1: Name -> lock when pinned
                if (column == 1 && includePinnedRow && row == 0) return null;

                // Column 2: Expression -> lock when pinned; otherwise Expression editor
                if (column == 2) {
                    if (includePinnedRow && row == 0) return null;
                    return exprEditor;
                }

                // Column 3: Description -> always editable text
                if (column == 3) return super.getCellEditor(row, column);

                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                // Pinned look for name + expr
                if (includePinnedRow && row == 0 && (column == 1 || column == 2)) {
                    return new GrayItalicLockedRenderer();
                }
                // Expression monospaced
                if (column == 2) {
                    return new MapTablePanel.MultiLineMonoRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };

        // Table UX
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));

        // Sizes
        if (table.getColumnModel().getColumnCount() > 3) {
            table.getColumnModel().getColumn(2).setPreferredWidth(480); // Expression
            table.getColumnModel().getColumn(3).setPreferredWidth(320); // Description
        }
        table.getColumnModel().getColumn(0).setMaxWidth(90); // Enabled

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

    /** Gray/italic locked look for pinned cells (name/expr). */
    static final class GrayItalicLockedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
            return c;
        }
    }
}

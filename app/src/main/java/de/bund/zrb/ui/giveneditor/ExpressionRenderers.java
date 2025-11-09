package de.bund.zrb.ui.giveneditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import de.bund.zrb.ui.expressions.ExpressionHighlighterUtils;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;

/**
 * Shared renderers for expression cells (DRY extraction).
 */
public class ExpressionRenderers {

    public static final class ExpressionRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final RSyntaxTextArea ta;

        public ExpressionRenderer() {
            super(new BorderLayout());
            ta = new RSyntaxTextArea();
            ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            ta.setCodeFoldingEnabled(false);
            ta.setBracketMatchingEnabled(true);
            ta.setAnimateBracketMatching(true);
            ta.setAutoIndentEnabled(false);
            ta.setTabsEmulated(true);
            ta.setTabSize(2);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(false);
            ta.setRows(3);
            ta.setEditable(false);
            ta.setFocusable(false);
            ta.setBorder(null);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            // ensure highlighter is layered
            ((DefaultHighlighter) ta.getHighlighter()).setDrawsLayeredHighlights(true);

            this.add(ta, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "" : String.valueOf(value);
            ta.setText(text);

            if (isSelected) {
                ta.setForeground(table.getSelectionForeground());
                ta.setBackground(table.getSelectionBackground());
            } else {
                ta.setForeground(table.getForeground());
                ta.setBackground(table.getBackground());
            }

            // Use shared util
            ExpressionHighlighterUtils.applyRainbowHighlights(ta);

            int fmH = ta.getFontMetrics(ta.getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) table.setRowHeight(desired);

            return this;
        }
    }

    public static final class PinnedExpressionRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final RSyntaxTextArea ta;

        public PinnedExpressionRenderer() {
            super(new BorderLayout());
            ta = new RSyntaxTextArea();
            ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            ta.setCodeFoldingEnabled(false);
            ta.setBracketMatchingEnabled(true);
            ta.setAnimateBracketMatching(true);
            ta.setAutoIndentEnabled(false);
            ta.setTabsEmulated(true);
            ta.setTabSize(2);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(false);
            ta.setRows(3);
            ta.setEditable(false);
            ta.setEnabled(false);
            ta.setFocusable(false);
            ta.setBorder(null);
            ta.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 12));

            ((DefaultHighlighter) ta.getHighlighter()).setDrawsLayeredHighlights(true);

            this.add(ta, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "" : String.valueOf(value);
            ta.setText(text);

            if (isSelected) {
                ta.setForeground(table.getSelectionForeground());
                ta.setBackground(table.getSelectionBackground());
            } else {
                ta.setForeground(Color.GRAY);
                ta.setBackground(table.getBackground());
            }

            ExpressionHighlighterUtils.applyRainbowHighlights(ta);

            int fmH = ta.getFontMetrics(ta.getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) table.setRowHeight(desired);

            return this;
        }
    }
}

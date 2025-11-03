// src/main/java/de/bund/zrb/ui/celleditors/ExpressionCellEditor.java
package de.bund.zrb.ui.celleditors;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static org.fife.ui.autocomplete.Util.startsWithIgnoreCase;

/**
 * RSyntaxTextArea-based TableCellEditor with context-aware AutoCompletion for {{ ... }}.
 * - Java 8 compatible
 * - Variables: plain
 * - Functions: bold, insert only "(" (so auto-popup opens immediately for args)
 * - Regex presets: italic
 * - Descriptions via DescribedItem for functions/regex
 * - No snippets with ${cursor}
 * - 3-line editor height
 */
public class ExpressionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {

    private final RSyntaxTextArea textArea = new RSyntaxTextArea(6, 40);
    private final AutoCompletion autoCompletion;
    private final ContextualProvider provider;

    private final Supplier<List<String>> variableNamesSupplier;
    private final Supplier<Map<String, DescribedItem>> functionItemsSupplier;
    private final Supplier<Map<String, DescribedItem>> regexItemsSupplier;

    public ExpressionCellEditor(Supplier<List<String>> variableNamesSupplier,
                                Supplier<Map<String, DescribedItem>> functionItemsSupplier,
                                Supplier<Map<String, DescribedItem>> regexItemsSupplier) {

        this.variableNamesSupplier = variableNamesSupplier != null
                ? variableNamesSupplier
                : new Supplier<List<String>>() { @Override public List<String> get() { return java.util.Collections.<String>emptyList(); } };

        this.functionItemsSupplier = functionItemsSupplier != null
                ? functionItemsSupplier
                : new Supplier<Map<String, DescribedItem>>() { @Override public Map<String, DescribedItem> get() { return java.util.Collections.<String, DescribedItem>emptyMap(); } };

        this.regexItemsSupplier = regexItemsSupplier != null
                ? regexItemsSupplier
                : new Supplier<Map<String, DescribedItem>>() { @Override public Map<String, DescribedItem> get() { return java.util.Collections.<String, DescribedItem>emptyMap(); } };

        configureEditorArea(textArea);

        provider = new ContextualProvider(this.variableNamesSupplier, this.functionItemsSupplier, this.regexItemsSupplier);

        // Auto-Activation inkl. '{', '(' und ';' – damit direkt nach "{{" und "(" die Liste aufpoppt
        provider.setAutoActivationRules(true, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_{(;*");

        autoCompletion = new AutoCompletion(provider);
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setAutoActivationDelay(120);
        autoCompletion.setParameterAssistanceEnabled(false); // wir nutzen kein Param-Template
        autoCompletion.setTriggerKey(KeyStroke.getKeyStroke("control SPACE"));
        autoCompletion.setAutoCompleteSingleChoices(false); // nie sofort einfügen
        autoCompletion.setShowDescWindow(true);
        autoCompletion.install(textArea);

        installEnterBehavior(textArea);
    }

    // ----- UI setup -----

    private static void configureEditorArea(RSyntaxTextArea ta) {
        ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        ta.setCodeFoldingEnabled(false);
        ta.setBracketMatchingEnabled(true);
        ta.setAnimateBracketMatching(true);
        ta.setAutoIndentEnabled(true);
        ta.setTabsEmulated(true);
        ta.setTabSize(2);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);

        ta.setRows(3);
        int lineH = ta.getFontMetrics(ta.getFont()).getHeight();
        ta.setPreferredSize(new Dimension(0, 3 * lineH + 8));
    }

    private static void installEnterBehavior(final RSyntaxTextArea ta) {
        final InputMap im = ta.getInputMap();
        final ActionMap am = ta.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "commit-or-newline");
        am.put("commit-or-newline", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(); // Java 8
                int mods = e != null ? e.getModifiers() : 0;
                boolean ctrlOrCmd = (mods & menuMask) != 0;
                boolean shift = (mods & java.awt.event.InputEvent.SHIFT_MASK) != 0;
                if (ctrlOrCmd || shift) {
                    ta.replaceSelection("\n");
                } else {
                    ta.transferFocus();
                }
            }
        });
    }

    // ----- Provider mit Kontext + eigenem Styling -----

    private static final class ContextualProvider extends DefaultCompletionProvider {
        private final Supplier<List<String>> variableNamesSupplier;
        private final Supplier<Map<String, DescribedItem>> functionItemsSupplier;
        private final Supplier<Map<String, DescribedItem>> regexItemsSupplier;

        ContextualProvider(Supplier<List<String>> variableNamesSupplier,
                           Supplier<Map<String, DescribedItem>> functionItemsSupplier,
                           Supplier<Map<String, DescribedItem>> regexItemsSupplier) {
            this.variableNamesSupplier = variableNamesSupplier;
            this.functionItemsSupplier = functionItemsSupplier;
            this.regexItemsSupplier = regexItemsSupplier;
        }

        @Override
        public String getAlreadyEnteredText(JTextComponent comp) {
            Block b = findActiveBlock(comp);
            if (!b.active) return "";
            return safeScanIdentifierPrefix(b.body, b.cursorInBody); // robust gegen leere Präfixe
        }

        @Override
        @SuppressWarnings("unchecked")
        public List getCompletions(JTextComponent comp) {
            List out = new ArrayList();
            Block b = findActiveBlock(comp);
            if (!b.active) {
                return out; // außerhalb von {{...}} → nichts automatisch vorschlagen
            }

            String prefix = safeScanIdentifierPrefix(b.body, b.cursorInBody);
            Kind kind = determineKind(b.body, b.cursorInBody);

            if (kind == Kind.FUNCTION_NAME) {
                addFunctionCompletions(out, prefix);
            } else if (kind == Kind.ARGUMENT) {
                addVariableCompletions(out, prefix);
                addRegexCompletions(out, prefix);
            } else { // VARIABLE oder Fallback
                addVariableCompletions(out, prefix);
                // optional Template-Marker *fn (fett in Liste)
                Map<String, DescribedItem> fmap = functionItemsSupplier.get();
                List<String> fnNames = sortedKeys(fmap.keySet());
                for (int i = 0; i < fnNames.size(); i++) {
                    String marker = "*" + fnNames.get(i);
                    if (startsWithIgnoreCase(marker, prefix)) {
                        out.add(new StyledBasicCompletion(this, marker, null, StyledBasicCompletion.Style.BOLD));
                    }
                }
            }
            return out;
        }

        // ---- Completions ----

        private void addFunctionCompletions(List out, String prefix) {
            Map<String, DescribedItem> map = functionItemsSupplier.get();
            List<String> names = sortedKeys(map.keySet());
            for (int i = 0; i < names.size(); i++) {
                String fn = names.get(i);
                if (!startsWithIgnoreCase(fn, prefix)) continue;
                String desc = null;
                DescribedItem di = map.get(fn);
                if (di != null) desc = di.getDescription();
                out.add(new InsertOpeningParenFunction(this, fn, desc));
            }
        }

        private void addRegexCompletions(List out, String prefix) {
            Map<String, DescribedItem> map = regexItemsSupplier.get();
            List<String> names = sortedKeys(map.keySet());
            for (int i = 0; i < names.size(); i++) {
                String rx = names.get(i);
                if (!startsWithIgnoreCase(rx, prefix)) continue;
                String desc = null;
                DescribedItem di = map.get(rx);
                if (di != null) desc = di.getDescription();
                out.add(new StyledBasicCompletion(this, rx, desc, StyledBasicCompletion.Style.ITALIC));
            }
        }

        private void addVariableCompletions(List out, String prefix) {
            List<String> vars = variableNamesSupplier.get();
            for (int i = 0; i < vars.size(); i++) {
                String v = vars.get(i);
                if (startsWithIgnoreCase(v, prefix)) {
                    out.add(new StyledBasicCompletion(this, v, null, StyledBasicCompletion.Style.PLAIN));
                }
            }
        }

        private List<String> sortedKeys(java.util.Set<String> keys) {
            List<String> list = new ArrayList<String>(keys);
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
            return list;
        }

        // ---- Kontext-Erkennung ----

        private Kind determineKind(String body, int caret) {
            String left = body.substring(0, Math.min(caret, body.length()));
            if (insideQuotes(left)) return Kind.NONE;

            int firstOpenParen = indexOfTopLevel(left, '(');
            if (firstOpenParen < 0) {
                String trimmed = left.trim();
                if (trimmed.length() == 0) return Kind.FUNCTION_NAME;        // direkt nach "{{"
                if (!trimmed.contains(" ") && !trimmed.contains("}")) return Kind.FUNCTION_NAME; // Wortanfang
                return Kind.VARIABLE;
            }
            return Kind.ARGUMENT; // nach '('
        }

        private boolean insideQuotes(String s) {
            boolean in = false; char q = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (!in && (c == '\'' || c == '"')) { in = true; q = c; }
                else if (in && c == q) { in = false; }
            }
            return in;
        }

        private int indexOfTopLevel(String s, char target) {
            boolean inQuote = false; char q = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (inQuote) { if (c == q) inQuote = false; continue; }
                if (c == '\'' || c == '"') { inQuote = true; q = c; continue; }
                if (c == target) return i;
            }
            return -1;
        }

        // ---- Aktiven {{...}}-Block + Präfix bestimmen ----

        private Block findActiveBlock(JTextComponent comp) {
            try {
                String all = comp.getDocument().getText(0, comp.getDocument().getLength());
                int caret = comp.getCaretPosition();

                int open = lastIndexOf(all, "{{", caret);
                int close = lastIndexOf(all, "}}", caret);

                boolean active = open >= 0 && (close < 0 || close < open);
                if (!active) return Block.inactive();

                int end = indexOfForward(all, "}}", open + 2);
                if (end < 0) end = all.length();

                String body = all.substring(open + 2, end);
                int cursorInBody = Math.max(0, Math.min(body.length(), caret - (open + 2)));
                return new Block(true, body, cursorInBody);
            } catch (BadLocationException ex) {
                return Block.inactive();
            }
        }

        private int lastIndexOf(String s, String needle, int upto) {
            int stop = Math.min(upto, s.length());
            if (stop <= 0) return -1;
            return s.lastIndexOf(needle, stop - 1);
        }

        private int indexOfForward(String s, String needle, int from) {
            if (from < 0) from = 0;
            return s.indexOf(needle, from);
        }

        private String safeScanIdentifierPrefix(String text, int caretInText) {
            if (text == null || text.length() == 0 || caretInText <= 0) {
                return "";
            }
            int i = Math.min(caretInText, text.length()) - 1;
            StringBuilder sb = new StringBuilder();
            while (i >= 0) {
                char ch = text.charAt(i);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '*') {
                    sb.insert(0, ch);
                    i--;
                } else {
                    break;
                }
            }
            return sb.toString();
        }
    }

    // ----- Styled completion items -----

    /** Plain/italic/bold label via HTML; description in summary window. */
    private static final class StyledBasicCompletion extends BasicCompletion {
        enum Style { PLAIN, ITALIC, BOLD }
        private final Style style;
        private final String plainText;

        StyledBasicCompletion(CompletionProvider provider, String replacementText, String description, Style style) {
            super(provider, replacementText);
            this.style = style;
            this.plainText = replacementText;
            if (description != null && description.length() > 0) {
                setShortDescription(description);
            }
        }

        @Override
        public String getInputText() {
            if (style == Style.BOLD)   return "<html><b>" + escapeHtml(plainText) + "</b></html>";
            if (style == Style.ITALIC) return "<html><i>" + escapeHtml(plainText) + "</i></html>";
            return escapeHtml(plainText);
        }

        private String escapeHtml(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#39;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Function completion that displays bold label and inserts only "("
     * (so auto-activation triggers argument suggestions immediately).
     */
    private static final class InsertOpeningParenFunction extends FunctionCompletion {
        private final String name;

        InsertOpeningParenFunction(CompletionProvider provider, String functionName, String description) {
            super(provider, functionName, null /* no return type */);
            this.name = functionName;
            if (description != null && description.length() > 0) {
                setShortDescription(description);
            }
        }

        @Override
        public String getInputText() {
            return "<html><b>" + name + "</b></html>";
        }

        @Override
        public ParameterizedCompletionInsertionInfo getInsertionInfo(JTextComponent tc, boolean replaceTabsWithSpaces) {
            // Insert only "(" and place caret after it. Auto-activation includes '(' so popup opens.
            ParameterizedCompletionInsertionInfo info = new ParameterizedCompletionInsertionInfo();

            String insert = "(";
            int dot = tc.getCaretPosition();

            Position maxPos = null;
            try {
                maxPos = tc.getDocument().createPosition(dot + insert.length());
            } catch (BadLocationException ble) {
                // ignore
            }
            info.setCaretRange(dot + insert.length(), maxPos);
            info.setTextToInsert(insert);
            info.setDefaultEndOffs(dot + insert.length());
            info.addReplacementLocation(dot + insert.length(), dot + insert.length());
            info.setInitialSelection(dot + insert.length(), dot + insert.length());

            return info;
        }
    }

    // ----- Helpers -----

    private static final class Block {
        final boolean active;
        final String body;
        final int cursorInBody;
        Block(boolean active, String body, int cursorInBody) {
            this.active = active; this.body = body; this.cursorInBody = cursorInBody;
        }
        static Block inactive() { return new Block(false, "", 0); }
    }

    private enum Kind { FUNCTION_NAME, ARGUMENT, VARIABLE, NONE }

    // ----- TableCellEditor -----

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        textArea.selectAll();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { textArea.requestFocusInWindow(); }
        });
        return textArea;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }
}

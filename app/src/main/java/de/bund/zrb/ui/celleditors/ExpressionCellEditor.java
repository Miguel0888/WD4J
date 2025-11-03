// src/main/java/de/bund/zrb/ui/celleditors/ExpressionCellEditor.java
package de.bund.zrb.ui.celleditors;

import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static org.fife.ui.autocomplete.Util.startsWithIgnoreCase;

/**
 * RSyntaxTextArea-based TableCellEditor with context-aware AutoCompletion for {{ ... }}.
 * - Java 8 compatible
 * - Shows dropdown (no auto-commit)
 * - Inserts functions as fn(arg) with '; ' as separator
 * - 3-line editor height
 */
public class ExpressionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {

    private final RSyntaxTextArea textArea = new RSyntaxTextArea(6, 40);
    private final AutoCompletion autoCompletion;
    private final ContextualProvider provider;

    private final Supplier<List<String>> variableNamesSupplier;
    private final Supplier<List<String>> regexNamesSupplier;

    public ExpressionCellEditor(Supplier<List<String>> variableNamesSupplier,
                                Supplier<List<String>> regexNamesSupplier) {

        this.variableNamesSupplier = variableNamesSupplier != null
                ? variableNamesSupplier
                : new Supplier<List<String>>() { @Override public List<String> get() { return java.util.Collections.<String>emptyList(); } };

        this.regexNamesSupplier = regexNamesSupplier != null
                ? regexNamesSupplier
                : new Supplier<List<String>>() { @Override public List<String> get() { return java.util.Collections.<String>emptyList(); } };

        configureEditorArea(textArea);

        // Custom provider overriding parameter list chars
        provider = new ContextualProvider(this.variableNamesSupplier, this.regexNamesSupplier);

        // Auto-activation incl. '(' and ';'
        provider.setAutoActivationRules(true, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_({;*");

        // a few generic snippets (selection happens contextually)
        provider.addCompletion(new ShorthandCompletion(provider, "{{", "{{${cursor}}}}", "Mustache-Variable"));
        provider.addCompletion(new ShorthandCompletion(provider, "regex", "{{REGEX:${cursor}}}", "Regex-Platzhalter"));

        autoCompletion = new AutoCompletion(provider);
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setAutoActivationDelay(120);
        autoCompletion.setParameterAssistanceEnabled(true);
        autoCompletion.setTriggerKey(KeyStroke.getKeyStroke("control SPACE"));
        autoCompletion.setAutoCompleteSingleChoices(false); // do not insert automatically
        autoCompletion.setShowDescWindow(true);
        autoCompletion.install(textArea);

        installEnterBehavior(textArea);
    }

    // ----- UI setup -----

    private static void configureEditorArea(RSyntaxTextArea ta) {
        ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA); // pragmatic highlighting
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

    // ----- Provider with context + custom parameter list chars -----

    private static final class ContextualProvider extends DefaultCompletionProvider {
        private final Supplier<List<String>> variableNamesSupplier;
        private final Supplier<List<String>> regexNamesSupplier;

        ContextualProvider(Supplier<List<String>> variableNamesSupplier,
                           Supplier<List<String>> regexNamesSupplier) {
            this.variableNamesSupplier = variableNamesSupplier;
            this.regexNamesSupplier = regexNamesSupplier;
        }

        // >>> Override parameter list characters to control insertion as fn(arg1; arg2)
        @Override public char getParameterListStart()    { return '('; }
        @Override public String getParameterListSeparator() { return "; "; }
        @Override public char getParameterListEnd()      { return ')'; }

        @Override
        public String getAlreadyEnteredText(JTextComponent comp) {
            Block b = findActiveBlock(comp);
            if (!b.active) return "";
            return scanIdentifierPrefix(b.body, b.cursorInBody);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List getCompletions(JTextComponent comp) {
            List out = new ArrayList();
            Block b = findActiveBlock(comp);
            if (!b.active) {
                return out; // outside of {{...}} → no autos
            }

            String prefix = scanIdentifierPrefix(b.body, b.cursorInBody);
            Kind kind = determineKind(b.body, b.cursorInBody);

            if (kind == Kind.FUNCTION_NAME) {
                // function list (prefix-filtered)
                List<String> fns = new ArrayList<String>(ExpressionRegistryImpl.getInstance().getKeys());
                Collections.sort(fns, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < fns.size(); i++) {
                    String fn = fns.get(i);
                    if (startsWithIgnoreCase(fn, prefix)) {
                        out.add(buildFunctionCompletion(this, fn));
                    }
                }
            } else if (kind == Kind.ARGUMENT) {
                // variables + regex presets + optional {{...}} snippet
                addVariables(out, prefix);
                addRegexPresets(out, prefix);
                if (prefix.length() == 0) {
                    out.add(new ShorthandCompletion(this, "{{", "{{${cursor}}}}", "Mustache-Variable"));
                }
            } else { // VARIABLE or fallback
                addVariables(out, prefix);
                // optional template markers *fn
                List<String> fns = new ArrayList<String>(ExpressionRegistryImpl.getInstance().getKeys());
                Collections.sort(fns, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < fns.size(); i++) {
                    String marker = "*" + fns.get(i);
                    if (startsWithIgnoreCase(marker, prefix)) {
                        out.add(new BasicCompletion(this, marker, "Template", "Template-Funktion"));
                    }
                }
            }

            return out;
        }

        // -- Build completions --

        private Completion buildFunctionCompletion(CompletionProvider p, String fn) {
            FunctionCompletion c = new FunctionCompletion(p, fn, "String");
            // one dummy param so caret lands inside parentheses and calltip shows
            List<ParameterizedCompletion.Parameter> params =
                    new ArrayList<ParameterizedCompletion.Parameter>(1);
            params.add(new ParameterizedCompletion.Parameter("String", "arg"));
            c.setParams(params);
            c.setShortDescription("Funktion aus ExpressionRegistry");
            return c;
        }

        private void addVariables(List out, String prefix) {
            List<String> vars = variableNamesSupplier.get();
            for (int i = 0; i < vars.size(); i++) {
                String v = vars.get(i);
                if (startsWithIgnoreCase(v, prefix)) {
                    out.add(new BasicCompletion(this, v, "Variable", "Variable"));
                }
            }
        }

        private void addRegexPresets(List out, String prefix) {
            List<String> rx = regexNamesSupplier.get();
            for (int i = 0; i < rx.size(); i++) {
                String r = rx.get(i);
                if (startsWithIgnoreCase(r, prefix)) {
                    out.add(new BasicCompletion(this, r, "Regex-Preset", "Vordefinierte Regex"));
                }
            }
        }

        // -- Context detection (simple & robust) --

        private Kind determineKind(String body, int caret) {
            String left = body.substring(0, Math.min(caret, body.length()));
            if (insideQuotes(left)) return Kind.NONE;

            int firstOpenParen = indexOfTopLevel(left, '(');
            if (firstOpenParen < 0) {
                String trimmed = left.trim();
                if (trimmed.length() == 0) return Kind.FUNCTION_NAME;
                if (!trimmed.contains(" ") && !trimmed.contains("}")) return Kind.FUNCTION_NAME;
                return Kind.VARIABLE;
            }
            return Kind.ARGUMENT; // after '(' (also after ';' heuristically)
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

        // -- Active block + prefix scan --

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
            return s.lastIndexOf(needle, stop - 1);
        }

        private int indexOfForward(String s, String needle, int from) {
            return s.indexOf(needle, from);
        }

        private String scanIdentifierPrefix(String text, int caretInText) {
            int i = Math.max(0, Math.min(caretInText, text.length()) - 1);
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

    private static final class Block {
        final boolean active;
        final String body;
        final int cursorInBody;
        Block(boolean active, String body, int cursorInBody) {
            this.active = active;
            this.body = body;
            this.cursorInBody = cursorInBody;
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

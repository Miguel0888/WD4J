package de.bund.zrb.ui.expressions;

import de.bund.zrb.runtime.ExpressionRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Provide an inline expression editor for GivenCondition.
 *
 * Intent:
 * - Let the tester type expressions like:
 *      Es existiert eine {{Belegnummer}}.
 *      Der Benutzer hat einen OTP-Code {{OTP({{username}})}}.
 *
 * UX goals:
 * - Toolbar in one row: [QuickInsert][{{][}}] ... [ⓘ]
 * - Medium sized editor (6 rows, ~70 columns) instead of full-screen monster.
 * - Rainbow highlighting for delimiters {{ }}, ( ) with soft pastel colors.
 *
 * Scope:
 * - Only UI. Parsing/evaluation is done outside.
 */
public class ExpressionInputPanel extends JPanel {

    // UI parts
    private final RSyntaxTextArea editor;
    private final JLabel statusLabel;
    private final JComboBox<String> quickInsertBox;
    private final JButton btnOpenBraces;
    private final JButton btnCloseBraces;
    private final JLabel helpLabel;

    // Highlight infra
    private final DefaultHighlighter highlighter;

    // soft rainbow base colors
    private final Color[] rainbowBase = new Color[]{
            new Color(255, 105, 97),   // coral red-ish
            new Color(255, 179, 71),   // orange
            new Color(255, 249, 128),  // yellow
            new Color(144, 238, 144),  // green
            new Color(135, 206, 250),  // blue
            new Color(221, 160, 221)   // purple
    };

    public ExpressionInputPanel() {
        super(new BorderLayout(6, 6));

        // ───────────────── Toolbar: QuickInsert + {{ }} + Info ─────────────────
        JPanel toolbarRow = new JPanel(new BorderLayout(4, 0));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        quickInsertBox = new JComboBox<String>();
        quickInsertBox.setEditable(false);
        quickInsertBox.setPrototypeDisplayValue("OTP({{username}})");
        quickInsertBox.setToolTipText("Füge vordefinierte Funktion/Variable ein");
        fillQuickInsertBox(quickInsertBox);

        quickInsertBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object sel = quickInsertBox.getSelectedItem();
                if (sel != null) {
                    insertTextAtCaret(sel.toString());
                }
            }
        });

        btnOpenBraces = new JButton("{{");
        btnOpenBraces.setMargin(new Insets(2, 6, 2, 6));
        btnOpenBraces.setFocusable(false);
        btnOpenBraces.setToolTipText("Beginne Platzhalter/Funktion");
        btnOpenBraces.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTextAtCaret("{{");
            }
        });

        btnCloseBraces = new JButton("}}");
        btnCloseBraces.setMargin(new Insets(2, 6, 2, 6));
        btnCloseBraces.setFocusable(false);
        btnCloseBraces.setToolTipText("Schließe Platzhalter/Funktion");
        btnCloseBraces.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTextAtCaret("}}");
            }
        });

        leftTools.add(quickInsertBox);
        leftTools.add(btnOpenBraces);
        leftTools.add(btnCloseBraces);

        JPanel rightHelp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        helpLabel = new JLabel("\u24D8"); // ⓘ
        helpLabel.setForeground(new Color(0x1976D2)); // blue accent
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.BOLD, 14f));
        helpLabel.setToolTipText(
                "<html>" +
                        "<b>Syntax-Hilfe</b><br>" +
                        "• Freitext: Es existiert eine {{Belegnummer}}.<br>" +
                        "• Variable: {{username}}<br>" +
                        "• Systemwert: {{Uhrzeit}}<br>" +
                        "• Funktion: OTP({{username}})<br>" +
                        "• Verschachtelt: Signatur(OTP({{username}}))<br>" +
                        "• Aufgelöst erst zur Laufzeit (spätester Zeitpunkt)." +
                        "</html>"
        );
        rightHelp.add(helpLabel);

        toolbarRow.add(leftTools, BorderLayout.CENTER);
        toolbarRow.add(rightHelp, BorderLayout.EAST);

        // ───────────────── Editor-Bereich mittelgroß ─────────────────
        editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        editor.setCodeFoldingEnabled(true);
        editor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editor.setLineWrap(false);
        editor.setWrapStyleWord(false);

        // kleinere Default-Höhe:
        editor.setRows(6);      // vorher 10
        editor.setColumns(70);  // vorher 80

        RTextScrollPane scrollPane = new RTextScrollPane(editor);

        // wir geben jetzt moderate Mindest-/Preferred-Größen
        Dimension minSize = new Dimension(500, 140);
        Dimension prefSize = new Dimension(650, 160);
        scrollPane.setMinimumSize(minSize);
        scrollPane.setPreferredSize(prefSize);

        // Wrapper, damit GridBag was Greifbares hat
        JPanel editorRegion = new JPanel(new BorderLayout());
        editorRegion.add(scrollPane, BorderLayout.CENTER);
        editorRegion.setMinimumSize(minSize);
        editorRegion.setPreferredSize(prefSize);

        // Rainbow-Highlighter
        highlighter = (DefaultHighlighter) editor.getHighlighter();
        highlighter.setDrawsLayeredHighlights(true);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshRainbow(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshRainbow(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshRainbow(); }
        });

        // ───────────────── Status-Zeile ─────────────────
        statusLabel = new JLabel(" ");
        statusLabel.setOpaque(true);
        setStatusNeutral("Kein Ausdruck");

        // ───────────────── Zusammenbauen ─────────────────
        add(toolbarRow, BorderLayout.NORTH);
        add(editorRegion, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    // ----- Public API for GivenConditionEditorTab -----

    public RSyntaxTextArea getEditor() {
        return editor;
    }

    public String getExpressionText() {
        return editor.getText();
    }

    public void setExpressionText(String text) {
        editor.setText(text == null ? "" : text);
        editor.setCaretPosition(editor.getText().length());
        refreshRainbow();
    }

    public void setStatusOk(String msg) {
        statusLabel.setBackground(new Color(0xC8E6C9));
        statusLabel.setForeground(new Color(0x2E7D32));
        statusLabel.setText(msg != null ? msg : "OK");
    }

    public void setStatusError(String msg) {
        statusLabel.setBackground(new Color(0xFFCDD2));
        statusLabel.setForeground(new Color(0xC62828));
        statusLabel.setText(msg != null ? msg : "Fehler");
    }

    public void setStatusNeutral(String msg) {
        statusLabel.setBackground(new Color(0xFFF9C4));
        statusLabel.setForeground(new Color(0x5D4037));
        statusLabel.setText(msg != null ? msg : "…");
    }

    // ----- Toolbar helpers -----

    private void fillQuickInsertBox(JComboBox<String> box) {
        addIfAbsent(box, "OTP({{username}})");
        addIfAbsent(box, "{{username}}");
        addIfAbsent(box, "{{Uhrzeit}}");

        // add dynamic functions from registry (if available)
        try {
            ExpressionRegistry reg = ExpressionRegistryImpl.getInstance();
            List<String> keys = new ArrayList<String>(reg.getKeys());
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                addIfAbsent(box, k + "(...)");
            }
        } catch (Throwable ignore) {
            // registry might not be initialized yet
        }
    }

    private void addIfAbsent(JComboBox<String> box, String value) {
        for (int i = 0; i < box.getItemCount(); i++) {
            Object it = box.getItemAt(i);
            if (value.equals(it)) return;
        }
        box.addItem(value);
    }

    private void insertTextAtCaret(String snippet) {
        if (snippet == null || snippet.length() == 0) return;
        int pos = editor.getCaretPosition();
        try {
            editor.getDocument().insertString(pos, snippet, null);
            editor.setCaretPosition(pos + snippet.length());
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    // ----- Rainbow highlighting -----

    private void refreshRainbow() {
        highlighter.removeAllHighlights();

        String text = editor.getText();
        if (text == null || text.length() == 0) {
            return;
        }

        List<BracketPair> pairs = collectPairs(text);

        for (int i = 0; i < pairs.size(); i++) {
            BracketPair p = pairs.get(i);

            Color base = rainbowBase[p.depth % rainbowBase.length];
            Color bg   = new Color(base.getRed(), base.getGreen(), base.getBlue(), 60);
            Color line = new Color(base.getRed(), base.getGreen(), base.getBlue(), 180);

            Highlighter.HighlightPainter painter = new SoftRainbowPainter(bg, line);

            try {
                highlighter.addHighlight(p.openStart, p.openEnd, painter);
                if (p.closeStart >= 0 && p.closeEnd >= 0) {
                    highlighter.addHighlight(p.closeStart, p.closeEnd, painter);
                }
            } catch (BadLocationException ignore) {
            }
        }
    }

    private List<BracketPair> collectPairs(String text) {
        List<BracketPair> result = new ArrayList<BracketPair>();

        Stack<OpenToken> curlyStack = new Stack<OpenToken>();
        Stack<OpenToken> parenStack = new Stack<OpenToken>();

        int i = 0;
        while (i < text.length()) {

            // "{{"
            if (i + 1 < text.length()
                    && text.charAt(i) == '{'
                    && text.charAt(i + 1) == '{') {

                OpenToken tok = new OpenToken(OpenToken.KIND_CURLY, i, i + 2, curlyStack.size());
                curlyStack.push(tok);
                i += 2;
                continue;
            }

            // "}}"
            if (i + 1 < text.length()
                    && text.charAt(i) == '}'
                    && text.charAt(i + 1) == '}') {

                if (!curlyStack.isEmpty()) {
                    OpenToken open = curlyStack.pop();
                    result.add(new BracketPair(
                            open.depth,
                            open.posStart, open.posEnd,
                            i, i + 2
                    ));
                }
                i += 2;
                continue;
            }

            // "("
            if (text.charAt(i) == '(') {
                OpenToken tok = new OpenToken(OpenToken.KIND_PAREN, i, i + 1, parenStack.size());
                parenStack.push(tok);
                i += 1;
                continue;
            }

            // ")"
            if (text.charAt(i) == ')') {
                if (!parenStack.isEmpty()) {
                    OpenToken open = parenStack.pop();
                    result.add(new BracketPair(
                            open.depth,
                            open.posStart, open.posEnd,
                            i, i + 1
                    ));
                }
                i += 1;
                continue;
            }

            i += 1;
        }

        // Markiere auch ungeschlossene opener
        while (!curlyStack.isEmpty()) {
            OpenToken open = curlyStack.pop();
            result.add(new BracketPair(
                    open.depth,
                    open.posStart, open.posEnd,
                    -1, -1
            ));
        }
        while (!parenStack.isEmpty()) {
            OpenToken open = parenStack.pop();
            result.add(new BracketPair(
                    open.depth,
                    open.posStart, open.posEnd,
                    -1, -1
            ));
        }

        return result;
    }

    private static class SoftRainbowPainter extends DefaultHighlighter.DefaultHighlightPainter {

        private final Color fillColor;
        private final Color lineColor;

        SoftRainbowPainter(Color fillColor, Color lineColor) {
            super(fillColor);
            this.fillColor = fillColor;
            this.lineColor = lineColor;
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1,
                                Shape bounds, JTextComponent c, View view) {

            try {
                Shape shape = view.modelToView(
                        offs0, Position.Bias.Forward,
                        offs1, Position.Bias.Backward,
                        bounds
                );
                Rectangle r = (shape instanceof Rectangle)
                        ? (Rectangle) shape
                        : shape.getBounds();

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(fillColor);
                    int arc = 6;
                    int padY = 1;
                    int padX = 1;
                    g2.fillRoundRect(
                            r.x - padX,
                            r.y + padY,
                            r.width + (padX * 2),
                            r.height - (padY * 2),
                            arc,
                            arc
                    );

                    g2.setColor(lineColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    int y = r.y + r.height - 2;
                    g2.drawLine(r.x - padX, y, r.x + r.width + padX, y);
                } finally {
                    g2.dispose();
                }
                return r;
            } catch (BadLocationException e) {
                return super.paintLayer(g, offs0, offs1, bounds, c, view);
            }
        }
    }

    private static class OpenToken {
        static final int KIND_CURLY = 1;
        static final int KIND_PAREN = 2;

        final int kind;
        final int posStart;
        final int posEnd;
        final int depth;

        OpenToken(int kind, int posStart, int posEnd, int depth) {
            this.kind = kind;
            this.posStart = posStart;
            this.posEnd = posEnd;
            this.depth = depth;
        }
    }

    private static class BracketPair {
        final int depth;
        final int openStart;
        final int openEnd;
        final int closeStart;
        final int closeEnd;

        BracketPair(int depth, int openStart, int openEnd, int closeStart, int closeEnd) {
            this.depth = depth;
            this.openStart = openStart;
            this.openEnd = openEnd;
            this.closeStart = closeStart;
            this.closeEnd = closeEnd;
        }
    }
}

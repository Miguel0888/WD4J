package de.bund.zrb.ui.expressions;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide a reusable Swing panel for editing dynamic Given/When expressions.
 *
 * Intent:
 * - Let test authors write expressions like:
 *      "Es existiert eine {{Belegnummer}}."
 *      "{{OTP({{username}})}}"
 *      "{{Uhrzeit}}"
 *
 * - Offer UX helpers:
 *      * Insert "{{" and "}}" via buttons.
 *      * Show inline help via a blue question mark icon (tooltip on hover).
 *      * Highlight all brace pairs with rainbow nesting colors.
 *
 * Responsibilities:
 * - Contain the editor (RSyntaxTextArea).
 * - Manage brace highlighting on every change.
 * - Offer public getters/setters for integration in higher-level forms.
 *
 * Do not:
 * - Parse or validate expressions here.
 * - Bind this panel to scenario state directly.
 *
 * Usage:
 *   ExpressionInputPanel p = new ExpressionInputPanel();
 *   someParent.add(p);
 *   String rawExpr = p.getExpressionText();
 */
public class ExpressionInputPanel extends JPanel {

    private final RSyntaxTextArea editor;
    private final JButton btnInsertOpen;
    private final JButton btnInsertClose;
    private final JLabel helpIcon;
    private final RTextScrollPane scrollPane;

    // Keep track of current highlight tags so they can be removed before repaint
    private final List<Object> rainbowHighlights = new ArrayList<Object>();

    // Define rainbow colors for nesting depth
    private final Color[] rainbowColors = new Color[] {
            new Color(255,  99,  71),   // tomato / reddish
            new Color(255, 165,   0),   // orange
            new Color(255, 215,   0),   // gold / yellow-ish
            new Color( 60, 179, 113),   // medium sea green
            new Color( 30, 144, 255),   // dodger blue
            new Color(138,  43, 226)    // blueviolet
    };

    public ExpressionInputPanel() {
        super(new BorderLayout(6, 6));

        // --- Editor vorbereiten --------------------------------------------
        editor = new RSyntaxTextArea(3, 40);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        editor.setCodeFoldingEnabled(false);
        editor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editor.setTabSize(2);
        editor.setAntiAliasingEnabled(true);

        // Provide gentle gray placeholder if empty (visual guidance for first-time users)
        editor.setText("Es existiert eine {{Belegnummer}}.");

        scrollPane = new RTextScrollPane(editor);
        scrollPane.setFoldIndicatorEnabled(false);
        scrollPane.setLineNumbersEnabled(false);

        // --- Buttons für {{ und }} -----------------------------------------
        btnInsertOpen = new JButton("{{");
        btnInsertOpen.setFocusable(false);
        btnInsertOpen.setMargin(new Insets(2, 6, 2, 6));
        btnInsertOpen.setToolTipText("Füge '{{' an der Cursor-Position ein");

        btnInsertClose = new JButton("}}");
        btnInsertClose.setFocusable(false);
        btnInsertClose.setMargin(new Insets(2, 6, 2, 6));
        btnInsertClose.setToolTipText("Füge '}}' an der Cursor-Position ein");

        btnInsertOpen.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                insertAtCaret("{{");
            }
        });

        btnInsertClose.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                insertAtCaret("}}");
            }
        });

        // --- Help Icon (blaues Fragezeichen) --------------------------------
        helpIcon = new JLabel("?");
        helpIcon.setOpaque(true);
        helpIcon.setHorizontalAlignment(SwingConstants.CENTER);
        helpIcon.setForeground(Color.WHITE);
        helpIcon.setBackground(new Color(30, 144, 255)); // dodger blue
        helpIcon.setBorder(BorderFactory.createLineBorder(new Color(30, 144, 255), 2, true));
        helpIcon.setFont(helpIcon.getFont().deriveFont(Font.BOLD, 12f));
        helpIcon.setPreferredSize(new Dimension(20, 20));
        helpIcon.setToolTipText(buildHelpTooltipHtml());

        // make it round-ish
        helpIcon.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                helpIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent e) {
                helpIcon.setCursor(Cursor.getDefaultCursor());
            }
        });

        // --- Rechte Seitenleiste (Buttons + Help) ---------------------------
        JPanel rightBar = new JPanel();
        rightBar.setLayout(new BoxLayout(rightBar, BoxLayout.Y_AXIS));
        rightBar.add(btnInsertOpen);
        rightBar.add(Box.createVerticalStrut(4));
        rightBar.add(btnInsertClose);
        rightBar.add(Box.createVerticalStrut(10));
        rightBar.add(helpIcon);
        rightBar.add(Box.createVerticalGlue());

        // --- Untertitel / Hilfetext unterhalb des Editors -------------------
        JLabel hintLabel = new JLabel("Variablen mit {{Name}}, Funktionen wie {{OTP({{username}})}}, Text bleibt normal.");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        hintLabel.setForeground(Color.DARK_GRAY);

        // --- Status Label für Validität (optional, nur Anzeige) -------------
        // Du kannst das später von außen setzen, z. B. "✔ gültig" / "❌ Fehler"
        // Für jetzt geben wir nur einen neutralen Startzustand.
        statusLabel = new JLabel("Status: -");
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setForeground(Color.GRAY);

        JPanel bottomInfoPanel = new JPanel(new BorderLayout(6, 2));
        bottomInfoPanel.add(hintLabel, BorderLayout.NORTH);
        bottomInfoPanel.add(statusLabel, BorderLayout.SOUTH);

        // --- Zusammenbauen --------------------------------------------------
        JPanel editorWithRightBar = new JPanel(new BorderLayout(4, 4));
        editorWithRightBar.add(scrollPane, BorderLayout.CENTER);
        editorWithRightBar.add(rightBar, BorderLayout.EAST);

        add(editorWithRightBar, BorderLayout.CENTER);
        add(bottomInfoPanel, BorderLayout.SOUTH);

        // --- Listener für Rainbow-Highlight + Status -----------------------
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onEditorChanged(); }
            public void removeUpdate(DocumentEvent e) { onEditorChanged(); }
            public void changedUpdate(DocumentEvent e) { onEditorChanged(); }
        });

        // Initial highlight
        applyRainbowHighlight();
    }

    // Expose status label so container can also update validity info if gewünscht
    private final JLabel statusLabel;

    /**
     * Get current expression text content.
     */
    public String getExpressionText() {
        return editor.getText();
    }

    /**
     * Set expression text content programmatically.
     * (Use this when loading existing GivenCondition data.)
     */
    public void setExpressionText(String txt) {
        if (txt == null) {
            txt = "";
        }
        editor.setText(txt);
        applyRainbowHighlight();
    }

    /**
     * Set status message and color to give user feedback (e.g. parser status).
     * Example usage:
     *     panel.setStatusOk("Ausdruck ist gültig");
     *     panel.setStatusError("Unbekannte Variable X");
     */
    public void setStatusOk(String msg) {
        statusLabel.setText("Status: \u2714 " + msg); // ✔
        statusLabel.setForeground(new Color(0,128,0));
    }

    public void setStatusError(String msg) {
        statusLabel.setText("Status: \u274C " + msg); // ❌
        statusLabel.setForeground(Color.RED);
    }

    public void setStatusNeutral(String msg) {
        statusLabel.setText("Status: " + msg);
        statusLabel.setForeground(Color.GRAY);
    }

    /**
     * Insert a snippet into the editor at the caret position.
     * Keep caret after the inserted text.
     */
    private void insertAtCaret(String snippet) {
        if (snippet == null) return;
        int pos = editor.getCaretPosition();
        try {
            editor.getDocument().insertString(pos, snippet, null);
            editor.setCaretPosition(pos + snippet.length());
        } catch (BadLocationException ex) {
            // ignore, caret out of range should never happen
        }
    }

    /**
     * React to text changes in the editor.
     * Do not call expensive parsing here (that happens in outer tabs).
     * Only redo rainbow highlight locally.
     */
    private void onEditorChanged() {
        applyRainbowHighlight();
        // Outer container (GivenConditionEditorTab) kann zusätzlich parse + setStatusOk/Error aufrufen,
        // z. B. via DocumentListener, um Validität anzuzeigen.
    }

    /**
     * Apply rainbow highlight for nested {{ ... }} pairs.
     *
     * Concept:
     * - Scan all chars.
     * - Treat "{{" as "push new nesting level".
     * - Treat "}}" as "pop nesting level".
     * - For each brace token, add a highlight with a color based on nesting depth.
     *
     * Simplification:
     * - We highlight only the braces themselves ("{{" and "}}"), not the full region.
     * - Nested braces get different (cycled) colors.
     */
    private void applyRainbowHighlight() {
        Highlighter hl = editor.getHighlighter();

        // Remove previous rainbow highlights
        for (int i = 0; i < rainbowHighlights.size(); i++) {
            Object tag = rainbowHighlights.get(i);
            hl.removeHighlight(tag);
        }
        rainbowHighlights.clear();

        String text = editor.getText();
        if (text == null || text.length() == 0) {
            return;
        }

        // Find all brace tokens and collect them with depth info
        // We handle tokens as 2-char sequences "{{" and "}}".
        class BraceToken {
            int startOffset;
            int endOffset;
            int depth;
            BraceToken(int s, int e, int d) {
                this.startOffset = s;
                this.endOffset = e;
                this.depth = d;
            }
        }

        List<BraceToken> tokens = new ArrayList<BraceToken>();
        Stack<Integer> depthStack = new Stack<Integer>();
        int depth = 0;

        int i = 0;
        while (i < text.length() - 1) {
            char c1 = text.charAt(i);
            char c2 = text.charAt(i + 1);

            if (c1 == '{' && c2 == '{') {
                // Opening
                depthStack.push(Integer.valueOf(depth));
                depth = depth + 1;
                if (depth < 0) depth = 0; // safety

                tokens.add(new BraceToken(i, i + 2, depth));

                i += 2;
                continue;
            }

            if (c1 == '}' && c2 == '}') {
                // Closing
                // Closing corresponds to current depth, then pop
                if (depth < 1) {
                    depth = 1;
                }
                tokens.add(new BraceToken(i, i + 2, depth));

                if (!depthStack.isEmpty()) {
                    depthStack.pop();
                }
                depth = Math.max(0, depth - 1);

                i += 2;
                continue;
            }

            i++;
        }

        // Paint each token using rainbow color based on depth
        for (int j = 0; j < tokens.size(); j++) {
            BraceToken t = tokens.get(j);
            Color col = rainbowColors[(t.depth - 1 + rainbowColors.length) % rainbowColors.length];
            Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                    new Color(col.getRed(), col.getGreen(), col.getBlue(), 60)
            );
            try {
                Object tag = hl.addHighlight(t.startOffset, t.endOffset, painter);
                rainbowHighlights.add(tag);
            } catch (BadLocationException ex) {
                // ignore invalid offsets
            }
        }
    }

    /**
     * Build tooltip HTML. Keep explanation short and approachable for test authors.
     */
    private String buildHelpTooltipHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<b>Syntax-Hilfe</b><br>");
        sb.append("<br>");
        sb.append("• <b>Variable</b>: {{Belegnummer}}<br>");
        sb.append("  Liest zur Laufzeit einen Wert (z. B. Referenznummer).<br>");
        sb.append("<br>");
        sb.append("• <b>Funktion</b>: {{OTP({{username}})}}<br>");
        sb.append("  Ruft eine registrierte Funktion (z. B. OTP) auf.<br>");
        sb.append("  Parameter können wiederum Variablen sein.<br>");
        sb.append("<br>");
        sb.append("• <b>Text</b>: Alles außerhalb von {{...}} bleibt Literal-Text.<br>");
        sb.append("<br>");
        sb.append("Verschachtelung ist erlaubt, z. B.:<br>");
        sb.append("  {{wrap({{OTP({{username}})}})}}<br>");
        sb.append("<br>");
        sb.append("Tipp: Verwende die Buttons '{{' / '}}' um sauber zu arbeiten.");
        sb.append("</html>");
        return sb.toString();
    }

    public RSyntaxTextArea getEditor() {
        // Return internal editor so outer code can attach a DocumentListener
        return editor;
    }
}

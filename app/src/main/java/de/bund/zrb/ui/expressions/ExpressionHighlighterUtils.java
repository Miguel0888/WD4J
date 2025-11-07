package de.bund.zrb.ui.expressions;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Utility zum Finden von Bracket-Paaren ({{}} und ()) und zum Anwenden der SoftRainbow-Highlights
 * in einem JTextComponent (z.B. RSyntaxTextArea). Wird von Editor und Renderer wiederverwendet.
 */
public class ExpressionHighlighterUtils {

    public static class BracketPair {
        public final int depth;
        public final int openStart;
        public final int openEnd;
        public final int closeStart;
        public final int closeEnd;
        public BracketPair(int depth, int openStart, int openEnd, int closeStart, int closeEnd) {
            this.depth = depth; this.openStart = openStart; this.openEnd = openEnd; this.closeStart = closeStart; this.closeEnd = closeEnd;
        }
    }

    private static final Color[] rainbowBase = new Color[]{
            new Color(255, 105, 97),
            new Color(255, 179, 71),
            new Color(255, 249, 128),
            new Color(144, 238, 144),
            new Color(135, 206, 250),
            new Color(221, 160, 221)
    };

    private static class SoftRainbowPainter extends DefaultHighlighter.DefaultHighlightPainter {
        private final Color fillColor; private final Color lineColor;
        SoftRainbowPainter(Color fillColor, Color lineColor) { super(fillColor); this.fillColor = fillColor; this.lineColor = lineColor; }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            try {
                Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                Rectangle r = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(fillColor);
                    int arc = 6; int padY = 1; int padX = 1;
                    g2.fillRoundRect(r.x - padX, r.y + padY, r.width + (padX * 2), r.height - (padY * 2), arc, arc);
                    g2.setColor(lineColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    int y = r.y + r.height - 2;
                    g2.drawLine(r.x - padX, y, r.x + r.width + padX, y);
                } finally { g2.dispose(); }
                return r;
            } catch (BadLocationException e) {
                return super.paintLayer(g, offs0, offs1, bounds, c, view);
            }
        }
    }

    public static List<BracketPair> collectPairs(String text) {
        List<BracketPair> result = new ArrayList<>();
        Stack<Integer> curlyPos = new Stack<>();
        Stack<Integer> parenPos = new Stack<>();
        int i = 0;
        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '{' && text.charAt(i + 1) == '{') {
                curlyPos.push(i);
                i += 2; continue;
            }
            if (i + 1 < text.length() && text.charAt(i) == '}' && text.charAt(i + 1) == '}') {
                if (!curlyPos.isEmpty()) {
                    int open = curlyPos.pop();
                    result.add(new BracketPair(curlyPos.size(), open, open + 2, i, i + 2));
                }
                i += 2; continue;
            }
            if (text.charAt(i) == '(') { parenPos.push(i); i += 1; continue; }
            if (text.charAt(i) == ')') {
                if (!parenPos.isEmpty()) {
                    int open = parenPos.pop();
                    result.add(new BracketPair(parenPos.size(), open, open + 1, i, i + 1));
                }
                i += 1; continue;
            }
            i += 1;
        }
        while (!curlyPos.isEmpty()) {
            int open = curlyPos.pop();
            result.add(new BracketPair(curlyPos.size(), open, open + 2, -1, -1));
        }
        while (!parenPos.isEmpty()) {
            int open = parenPos.pop();
            result.add(new BracketPair(parenPos.size(), open, open + 1, -1, -1));
        }
        return result;
    }

    public static void applyRainbowHighlights(JTextComponent tc) {
        Highlighter h = tc.getHighlighter();
        if (h == null) return;
        try {
            h.removeAllHighlights();
        } catch (Exception ignore) {}
        String text = tc.getText();
        if (text == null || text.isEmpty()) return;
        List<BracketPair> pairs = collectPairs(text);
        for (BracketPair p : pairs) {
            Color base = rainbowBase[p.depth % rainbowBase.length];
            Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(), 60);
            Color line = new Color(base.getRed(), base.getGreen(), base.getBlue(), 180);
            DefaultHighlighter.DefaultHighlightPainter painter = new SoftRainbowPainter(bg, line);
            try {
                if (p.openStart >= 0 && p.openEnd >= 0) h.addHighlight(p.openStart, p.openEnd, painter);
                if (p.closeStart >= 0 && p.closeEnd >= 0) h.addHighlight(p.closeStart, p.closeEnd, painter);
            } catch (BadLocationException ignore) {}
        }
    }
}


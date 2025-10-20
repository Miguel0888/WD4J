package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;
// Optional: falls vorhanden
// import de.bund.zrb.model.ThenExpectation;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer für Testbaum-Knoten mit Typ-Icon (Suite/Case/Given/When/Then)
 * und Status-Overlay (✔/✖). Keine Bild-Ressourcen; Icons werden gezeichnet.
 */
public class TestTreeCellRenderer extends DefaultTreeCellRenderer {

    private enum NodeKind { SUITE, CASE, GIVEN, WHEN, THEN, OTHER }

    // Icon-Cache: key -> Icon (z. B. "BASE:SUITE" oder "OVER:PASS")
    private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();

    // Farben für Typen (Kreis-Icons)
    private static final Color SUITE_COLOR = new Color(0x2D6CDF);   // blau
    private static final Color CASE_COLOR  = new Color(0x8E44AD);   // violett
    private static final Color GIVEN_COLOR = new Color(0x16A085);   // türkis
    private static final Color WHEN_COLOR  = new Color(0xE67E22);   // orange
    private static final Color THEN_COLOR  = new Color(0x2ECC71);   // grün
    private static final Color OTHER_COLOR = new Color(0x7F8C8D);   // grau

    private static final int BASE_SZ = 18;     // Größe des Basis-Icons
    private static final int OVER_SZ = 10;     // Größe des Status-Overlays

    private static final Color PASS_GREEN = new Color(0, 153, 0);
    private static final Color FAIL_RED   = new Color(204, 0, 0);

    /**
     * Liefert ein zusammengesetztes Icon aus Basis (Typ) und optionalem Overlay (Status).
     */
    private static Icon composite(Icon base, Icon overlay) {
        if (overlay == null) return base;
        final int w = Math.max(base.getIconWidth(), overlay.getIconWidth());
        final int h = Math.max(base.getIconHeight(), overlay.getIconHeight());
        return new Icon() {
            @Override public int getIconWidth() { return w; }
            @Override public int getIconHeight() { return h; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                base.paintIcon(c, g, x, y);
                // Overlay unten rechts
                int ox = x + base.getIconWidth() - overlay.getIconWidth();
                int oy = y + base.getIconHeight() - overlay.getIconHeight();
                overlay.paintIcon(c, g, ox, oy);
            }
        };
    }

    private static Icon baseIconFor(NodeKind kind) {
        String key = "BASE:" + kind.name();
        return ICON_CACHE.computeIfAbsent(key, k -> {
            Color fill; String text;
            switch (kind) {
                case SUITE: fill = SUITE_COLOR; text = "S"; break;
                case CASE:  fill = CASE_COLOR;  text = "C"; break;
                case GIVEN: fill = GIVEN_COLOR; text = "G"; break;
                case WHEN:  fill = WHEN_COLOR;  text = "W"; break;
                case THEN:  fill = THEN_COLOR;  text = "T"; break;
                default:    fill = OTHER_COLOR; text = "•"; break;
            }
            return makeCircleBadge(BASE_SZ, fill, text);
        });
    }

    private static Icon overlayFor(TestNode.Status status) {
        if (status == TestNode.Status.PASSED) {
            return ICON_CACHE.computeIfAbsent("OVER:PASS", k -> makeCheckOverlay(OVER_SZ, PASS_GREEN));
        } else if (status == TestNode.Status.FAILED) {
            return ICON_CACHE.computeIfAbsent("OVER:FAIL", k -> makeCrossOverlay(OVER_SZ, FAIL_RED));
        }
        return null; // UNSET → kein Overlay
    }

    private static Icon makeCircleBadge(int size, Color fill, String text) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.translate(x, y);

                    // Kreis
                    g.setColor(fill);
                    g.fillOval(0, 0, size, size);
                    // Rand
                    g.setColor(fill.darker());
                    g.drawOval(0, 0, size, size);

                    // Text
                    g.setColor(Color.WHITE);
                    Font f = c.getFont().deriveFont(Font.BOLD, size * 0.62f);
                    g.setFont(f);
                    FontMetrics fm = g.getFontMetrics();
                    int tw = fm.stringWidth(text);
                    int th = fm.getAscent();
                    int tx = (size - tw) / 2;
                    int ty = (size + th) / 2 - 2;
                    g.drawString(text, tx, ty);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    private static Icon makeCheckOverlay(int size, Color color) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.translate(x, y);
                    // Hintergrundkreis
                    g.setColor(new Color(255, 255, 255, 220));
                    g.fillOval(0, 0, size, size);
                    g.setColor(color.darker());
                    g.drawOval(0, 0, size, size);
                    // Haken
                    g.setStroke(new BasicStroke(Math.max(2, size / 6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(color);
                    int x1 = (int)(size * 0.20), y1 = (int)(size * 0.55);
                    int x2 = (int)(size * 0.45), y2 = (int)(size * 0.80);
                    int x3 = (int)(size * 0.85), y3 = (int)(size * 0.25);
                    g.drawLine(x1, y1, x2, y2);
                    g.drawLine(x2, y2, x3, y3);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    private static Icon makeCrossOverlay(int size, Color color) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.translate(x, y);
                    // Hintergrundkreis
                    g.setColor(new Color(255, 255, 255, 220));
                    g.fillOval(0, 0, size, size);
                    g.setColor(color.darker());
                    g.drawOval(0, 0, size, size);
                    // Kreuz
                    g.setStroke(new BasicStroke(Math.max(2, size / 6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(color);
                    int p = (int)(size * 0.22);
                    int q = size - p;
                    g.drawLine(p, p, q, q);
                    g.drawLine(p, q, q, p);
                } finally {
                    g.dispose();
                }
            }
        };
    }

    private static NodeKind detectKind(TestNode node) {
        Object m = node.getModelRef();
        if (m instanceof TestSuite) return NodeKind.SUITE;
        if (m instanceof TestCase)  return NodeKind.CASE;
        if (m instanceof GivenCondition) return NodeKind.GIVEN;
        if (m instanceof TestAction) return NodeKind.WHEN;
        // if (m instanceof ThenExpectation) return NodeKind.THEN; // einkommentieren, wenn vorhanden
        // Heuristik: Text beginnt mit "THEN"?
        String text = String.valueOf(node);
        if (text != null && text.trim().toUpperCase().startsWith("THEN")) return NodeKind.THEN;
        return NodeKind.OTHER;
    }

    /** {@inheritDoc} */
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value,
            boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {

        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof TestNode) {
            TestNode node = (TestNode) value;

            // Typ-Icon + Status-Overlay
            NodeKind kind = detectKind(node);
            Icon base = baseIconFor(kind);
            Icon overlay = overlayFor(node.getStatus());
            label.setIcon(composite(base, overlay));

            // Textfarbe nach Status
            if (node.getStatus() == TestNode.Status.PASSED) {
                label.setForeground(new Color(0, 128, 0));
            } else if (node.getStatus() == TestNode.Status.FAILED) {
                label.setForeground(new Color(192, 0, 0));
            } else {
                label.setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
            }
        } else {
            // Fallback: Standard-Icons/-Farben
            label.setIcon(null);
            label.setForeground(sel ? getTextSelectionColor() : getTextNonSelectionColor());
        }

        return label;
    }
}

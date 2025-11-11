package de.bund.zrb.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * JTabbedPane-Erweiterung mit rechtsbündiger Zusatz-Komponente im Tab-Bereich (z. B. Hilfe-Button).
 * Die Zusatz-Komponente wird in derselben Zeile wie die Tabs dargestellt, nicht als eigener Tab.
 */
public class JTabbedPaneWithHelp extends JTabbedPane {
    private JComponent helpComponent;
    private Rectangle helpBounds = new Rectangle();
    private int rightMargin = 6;

    public JTabbedPaneWithHelp() {
        super();
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    /**
     * Setzt/ersetzt die rechtsbündige Zusatz-Komponente (z. B. runder Hilfe-Button).
     */
    public void setHelpComponent(JComponent c) {
        this.helpComponent = c;
        if (helpComponent != null) {
            helpComponent.setFocusable(false);
            helpComponent.setOpaque(false);
        }
        revalidate();
        repaint();
    }

    public JComponent getHelpComponent() { return helpComponent; }

    public void setRightMargin(int px) { rightMargin = Math.max(0, px); }

    @Override
    public void doLayout() {
        super.doLayout();
        computeHelpBounds();
    }

    private void computeHelpBounds() {
        if (helpComponent == null) return;
        Dimension pref = helpComponent.getPreferredSize();
        Insets ins = getInsets();
        int w = getWidth();
        int x = w - pref.width - rightMargin - ins.right;
        int y = ins.top + 2;
        if (getTabCount() > 0) {
            try {
                Rectangle r0 = getBoundsAt(0);
                if (r0 != null) {
                    y = r0.y + (r0.height - pref.height) / 2;
                }
            } catch (Exception ignore) {}
        }
        helpBounds.setBounds(x, y, pref.width, pref.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (helpComponent != null) {
            Graphics2D g2 = (Graphics2D) g.create(helpBounds.x, helpBounds.y, helpBounds.width, helpBounds.height);
            helpComponent.paint(g2);
            g2.dispose();
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (dispatchToHelp(e)) return;
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (dispatchToHelp(e)) return;
        super.processMouseMotionEvent(e);
    }

    private boolean dispatchToHelp(MouseEvent e) {
        if (helpComponent == null) return false;
        if (helpBounds.contains(e.getX(), e.getY())) {
            Point translated = new Point(e.getX() - helpBounds.x, e.getY() - helpBounds.y);
            MouseEvent sub = new MouseEvent(helpComponent, e.getID(), e.getWhen(), e.getModifiersEx(),
                    translated.x, translated.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
            helpComponent.dispatchEvent(sub);
            return true;
        }
        return false;
    }
}

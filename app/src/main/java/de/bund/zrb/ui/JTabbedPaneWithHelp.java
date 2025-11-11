package de.bund.zrb.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * JTabbedPane mit rechtsbündiger Zusatz-Komponente (z. B. Hilfe-Button)
 * im Tab-Bereich, ohne dass diese Komponente als Tab geführt wird.
 */
public class JTabbedPaneWithHelp extends JTabbedPane {

    /**
     * Spezieller Layout-Constraint, damit die Hilfe-Komponente
     * nicht als Tab behandelt wird.
     */
    private static final Object HELP_COMPONENT_CONSTRAINT = new Object();

    private JComponent helpComponent;
    private int rightMargin = 6;

    public JTabbedPaneWithHelp() {
        super();
        setOpaque(true);
    }

    /**
     * Set help component aligned to the right side of the tab area.
     * Component is not added as a tab.
     */
    public void setHelpComponent(JComponent component) {
        if (helpComponent != null) {
            super.remove(helpComponent);
            helpComponent = null;
        }

        if (component != null) {
            helpComponent = component;
            helpComponent.setFocusable(false);
            helpComponent.setOpaque(false);

            // Add with special constraint so JTabbedPane does not create a tab.
            super.addImpl(helpComponent, HELP_COMPONENT_CONSTRAINT, -1);

            // Ensure help is painted above tab contents.
            setComponentZOrder(helpComponent, 0);
        }

        revalidate();
        repaint();
    }

    public JComponent getHelpComponent() {
        return helpComponent;
    }

    /**
     * Set right margin in pixels between help component and right edge.
     */
    public void setRightMargin(int pixels) {
        if (pixels < 0) {
            pixels = 0;
        }
        this.rightMargin = pixels;
        revalidate();
        repaint();
    }

    public int getRightMargin() {
        return rightMargin;
    }

    /**
     * Stelle sicher, dass beim Layout die Hilfe korrekt positioniert wird.
     */
    @Override
    public void doLayout() {
        super.doLayout();
        layoutHelpComponent();
    }

    /**
     * Reposition help component when invalidated (resize, LAF changes, etc.).
     */
    @Override
    public void invalidate() {
        super.invalidate();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                layoutHelpComponent();
            }
        });
    }

    /**
     * Protect help component from being accidentally removed with tab APIs.
     */
    @Override
    public void remove(Component comp) {
        if (comp == helpComponent) {
            setHelpComponent(null);
        } else {
            super.remove(comp);
        }
    }

    /**
     * Entferne alle Tabs, aber belasse ggf. die Hilfe-Komponente.
     * (Optional, je nach gewünschtem Verhalten; hier bleibt Help erhalten.)
     */
    @Override
    public void removeAll() {
        // Temporarily store help component.
        JComponent help = helpComponent;
        helpComponent = null;

        super.removeAll();

        // Re-add help component if it existed.
        if (help != null) {
            helpComponent = help;
            super.addImpl(helpComponent, HELP_COMPONENT_CONSTRAINT, -1);
            setComponentZOrder(helpComponent, 0);
        }

        revalidate();
        repaint();
    }

    /**
     * Reposition help component in the tab area (top-right, same row as tabs).
     */
    private void layoutHelpComponent() {
        if (helpComponent == null || !helpComponent.isShowing()) {
            return;
        }

        Insets insets = getInsets();
        Dimension helpSize = helpComponent.getPreferredSize();

        int width = getWidth();
        int x = width - insets.right - rightMargin - helpSize.width;

        int y = insets.top + 2;

        if (getTabCount() > 0) {
            try {
                Rectangle firstTabBounds = getBoundsAt(0);
                if (firstTabBounds != null) {
                    // Align vertically centered within tab strip.
                    y = firstTabBounds.y + (firstTabBounds.height - helpSize.height) / 2;
                }
            } catch (Exception ex) {
                // Fallback: keep default y.
            }
        }

        if (y < insets.top) {
            y = insets.top;
        }

        helpComponent.setBounds(x, y, helpSize.width, helpSize.height);
        helpComponent.revalidate();
        helpComponent.repaint();
    }
}

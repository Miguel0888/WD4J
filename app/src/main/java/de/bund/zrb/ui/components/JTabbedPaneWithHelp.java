package de.bund.zrb.ui.components;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Composite-Komponente mit internem JTabbedPane und rechtsbündiger Help-Komponente
 * im Tab-Bereich (gleiche Höhe wie die Tabs, aber kein eigener Tab).
 *
 * Verwendung:
 * - Nutze JTabbedPaneWithHelp überall dort, wo bisher ein JTabbedPane stand.
 * - Alle wichtigen Tab-APIs werden durchgereicht.
 */
public class JTabbedPaneWithHelp extends JPanel {

    private final JTabbedPane tabbedPane;
    private JComponent helpComponent;
    private int rightMargin = 6;

    public JTabbedPaneWithHelp() {
        super(null); // Use custom layout in doLayout.
        this.tabbedPane = new JTabbedPane();
        // Add real tabbed pane as base layer.
        super.add(tabbedPane);
    }

    /**
     * Set help component aligned to the right side of the tab area.
     * Component is not treated as a tab.
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
            // Add help directly to this panel, not to the internal JTabbedPane.
            super.add(helpComponent);
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
     * Return the underlying JTabbedPane for advanced customization if needed.
     */
    public JTabbedPane getTabbedPaneDelegate() {
        return tabbedPane;
    }

    @Override
    public void doLayout() {
        Insets insets = getInsets();
        int width = getWidth();
        int height = getHeight();

        // Layout inner tabbed pane to fill the panel.
        int tpX = insets.left;
        int tpY = insets.top;
        int tpW = width - insets.left - insets.right;
        int tpH = height - insets.top - insets.bottom;

        tabbedPane.setBounds(tpX, tpY, Math.max(0, tpW), Math.max(0, tpH));

        layoutHelpComponent();
    }

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

    private void layoutHelpComponent() {
        if (helpComponent == null || !isShowing()) {
            return;
        }

        Dimension pref = helpComponent.getPreferredSize();
        Insets insets = getInsets();
        int panelWidth = getWidth();

        int x = panelWidth - insets.right - rightMargin - pref.width;
        if (x < insets.left) {
            x = insets.left;
        }

        int y = insets.top + 2;

        if (tabbedPane.getTabCount() > 0) {
            try {
                Rectangle r0 = tabbedPane.getBoundsAt(0);
                if (r0 != null) {
                    // Center help vertically in the tab strip (TOP placement).
                    y = insets.top + r0.y + (r0.height - pref.height) / 2;
                }
            } catch (Exception ex) {
                // Keep fallback y.
            }
        }

        if (y < insets.top) {
            y = insets.top;
        }

        helpComponent.setBounds(x, y, pref.width, pref.height);
        helpComponent.revalidate();
        helpComponent.repaint();
        // Ensure help is painted above the tabs.
        setComponentZOrder(helpComponent, 0);
        setComponentZOrder(tabbedPane, 1);
    }

    // ------------------------------------------------------------------------
    // Delegated JTabbedPane API (nur das, was dein Code aktuell verwendet).
    // ------------------------------------------------------------------------

    public void addChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        tabbedPane.removeChangeListener(listener);
    }

    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        tabbedPane.insertTab(title, icon, component, tip, index);
    }

    public void addTab(String title, Component component) {
        tabbedPane.addTab(title, component);
    }

    public void setTabComponentAt(int index, Component component) {
        tabbedPane.setTabComponentAt(index, component);
    }

    public Component getTabComponentAt(int index) {
        return tabbedPane.getTabComponentAt(index);
    }

    public void setEnabledAt(int index, boolean enabled) {
        tabbedPane.setEnabledAt(index, enabled);
    }

    public int getTabCount() {
        return tabbedPane.getTabCount();
    }

    public int getSelectedIndex() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelectedIndex(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public void setSelectedComponent(Component component) {
        tabbedPane.setSelectedComponent(component);
    }

    public int indexOfComponent(Component component) {
        return tabbedPane.indexOfComponent(component);
    }

    public Component getComponentAt(int index) {
        return tabbedPane.getComponentAt(index);
    }

    public void remove(int index) {
        tabbedPane.remove(index);
    }

    public void remove(Component component) {
        tabbedPane.remove(component);
    }

    public void removeAllTabs() {
        tabbedPane.removeAll();
    }

    public void setToolTipTextAt(int index, String text) {
        tabbedPane.setToolTipTextAt(index, text);
    }

    public void setIconAt(int index, Icon icon) {
        tabbedPane.setIconAt(index, icon);
    }

    public void setTitleAt(int index, String title) {
        tabbedPane.setTitleAt(index, title);
    }

    public void setTabLayoutPolicy(int policy) {
        tabbedPane.setTabLayoutPolicy(policy);
    }

    public void setTabPlacement(int placement) {
        tabbedPane.setTabPlacement(placement);
    }
}

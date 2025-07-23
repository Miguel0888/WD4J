package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public abstract class AbstractEditorTab<T> extends JPanel {
    private final T model;
    private final String title;

    public AbstractEditorTab(String title, T model) {
        super(new BorderLayout());
        this.title = title;
        this.model = model;

        // Initialize tab header asynchronously in EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Component parent = SwingUtilities.getWindowAncestor(AbstractEditorTab.this);
                if (parent instanceof JFrame) {
                    JTabbedPane tabbedPane = findTabbedPane((JFrame) parent);
                    if (tabbedPane != null) {
                        int index = tabbedPane.indexOfComponent(AbstractEditorTab.this);
                        if (index >= 0) {
                            JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            tabHeader.setOpaque(false);

                            JLabel label = new JLabel(" " + title + " ");
                            JButton closeButton = new JButton("✕");
                            closeButton.setForeground(Color.RED);
                            closeButton.setBorder(BorderFactory.createEmptyBorder());
                            closeButton.setContentAreaFilled(false);
                            closeButton.addActionListener(new AbstractAction() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    tabbedPane.remove(AbstractEditorTab.this);
                                }
                            });

                            tabHeader.add(label);
                            tabHeader.add(closeButton);

                            tabbedPane.setTabComponentAt(index, tabHeader);
                        }
                    }
                }
            }
        });
    }

    public T getModel() {
        return model;
    }

    public String getTabTitle() {
        return title;
    }

    // Suche nach dem TabbedPane innerhalb verschachtelter SplitPanes
    private JTabbedPane findTabbedPane(JFrame frame) {
        for (Component comp : frame.getContentPane().getComponents()) {
            if (comp instanceof JSplitPane) {
                JSplitPane outer = (JSplitPane) comp;
                Component right = outer.getRightComponent();
                if (right instanceof JSplitPane) {
                    JSplitPane inner = (JSplitPane) right;
                    Component center = inner.getLeftComponent();
                    if (center instanceof JPanel) {
                        JPanel panel = (JPanel) center;
                        for (Component c : panel.getComponents()) {
                            if (c instanceof JTabbedPane) {
                                return (JTabbedPane) c;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}

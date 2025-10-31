package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tab-Header mit Titel-Label + rotem X-Button zum Schließen.
 *
 * Neu:
 * - Rechtsklick auf den Header öffnet ein Popup-Menü
 *   mit Eintrag "Tab schließen", der das Gleiche macht wie das rote X.
 */
public class ClosableTabHeader extends JPanel {

    private final JTabbedPane parentTabbedPane;
    private final Component tabComponent;
    private final JLabel titleLabel;
    private final JButton closeButton;

    public ClosableTabHeader(JTabbedPane parentTabbedPane,
                             Component tabComponent,
                             String titleText) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        this.parentTabbedPane = parentTabbedPane;
        this.tabComponent = tabComponent;

        setOpaque(false);

        titleLabel = new JLabel(titleText);
        closeButton = new JButton("✕");
        closeButton.setForeground(Color.RED);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setFocusable(false);
        closeButton.setMargin(new Insets(0,4,0,4));

        closeButton.setToolTipText("Tab schließen");
        closeButton.addActionListener(e -> closeTab());

        add(titleLabel);
        add(closeButton);

        // Rechtsklick-Menü
        JPopupMenu popup = new JPopupMenu();
        JMenuItem closeItem = new JMenuItem("Tab schließen");
        closeItem.addActionListener(e -> closeTab());
        popup.add(closeItem);

        MouseAdapter popupTrigger = new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(ClosableTabHeader.this, e.getX(), e.getY());
                }
            }
            @Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
        };

        // Popup sowohl auf dem ganzen Header als auch auf dem Label/Button aktivieren
        this.addMouseListener(popupTrigger);
        titleLabel.addMouseListener(popupTrigger);
        closeButton.addMouseListener(popupTrigger);
    }

    private void closeTab() {
        int idx = parentTabbedPane.indexOfComponent(tabComponent);
        if (idx >= 0) {
            parentTabbedPane.removeTabAt(idx);
        }
    }

    public void setTitle(String newTitle) {
        titleLabel.setText(newTitle);
    }
}

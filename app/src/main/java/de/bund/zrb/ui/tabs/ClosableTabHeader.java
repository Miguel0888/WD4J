package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tab-Header mit Titel-Label + rotem X-Button zum Schließen.
 *
 * Neu:
 * - Klick auf den Header / Label wählt das zugehörige Tab aus.
 * - Optionaler onClose-Hook (Runnable) wird vor dem Entfernen des Tabs ausgeführt.
 */
public class ClosableTabHeader extends JPanel {

    private final JTabbedPane parentTabbedPane;
    private final Component tabComponent;
    private final JLabel titleLabel;
    private final JButton closeButton;
    private final Runnable onCloseHook;

    public ClosableTabHeader(JTabbedPane parentTabbedPane,
                             Component tabComponent,
                             String titleText) {
        this(parentTabbedPane, tabComponent, titleText, null);
    }

    public ClosableTabHeader(JTabbedPane parentTabbedPane,
                             Component tabComponent,
                             String titleText,
                             Runnable onCloseHook) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        this.parentTabbedPane = parentTabbedPane;
        this.tabComponent = tabComponent;
        this.onCloseHook = onCloseHook;

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

        // MouseListener: wenn auf Header oder Label geklickt wird, soll das zugehörige Tab selektiert werden.
        MouseAdapter selectOnClick = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                selectTab();
            }
            @Override public void mouseClicked(MouseEvent e) {
                // auch hier nochmal sicherstellen
                selectTab();
            }
        };
        this.addMouseListener(selectOnClick);
        titleLabel.addMouseListener(selectOnClick);
        // closeButton intentionally not bound to selectOnClick to avoid interfering with click area
    }

    private void selectTab() {
        int idx = parentTabbedPane.indexOfComponent(tabComponent);
        if (idx >= 0 && parentTabbedPane.getSelectedIndex() != idx) {
            parentTabbedPane.setSelectedIndex(idx);
        }
    }

    private void closeTab() {
        // Run hook first (if present)
        if (onCloseHook != null) {
            try {
                onCloseHook.run();
            } catch (Throwable ignore) { /* don't prevent closing */ }
        }
        int idx = parentTabbedPane.indexOfComponent(tabComponent);
        if (idx >= 0) {
            parentTabbedPane.removeTabAt(idx);
        }
    }

    public void setTitle(String newTitle) {
        titleLabel.setText(newTitle);
    }
}

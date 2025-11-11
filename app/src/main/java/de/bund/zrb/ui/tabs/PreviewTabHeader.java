package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Header für den Preview-Tab: zeigt links einen Pin-Button und daneben den Titel.
 * Der Pin-Button konvertiert den Preview-Tab in einen persistenten Tab (gleiches Verhalten wie
 * Rechtsklick → "In neuem Tab öffnen").
 *
 * Kein Close-Button – der Preview-Tab ist nicht closable.
 */
public class PreviewTabHeader extends JPanel {
    private final JTabbedPane parentTabbedPane;
    private final Component tabComponent;
    private final JLabel titleLabel;

    public interface PinHandler { void onPin(); }

    public PreviewTabHeader(JTabbedPane parentTabbedPane,
                            Component tabComponent,
                            String titleText,
                            PinHandler pinHandler) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        this.parentTabbedPane = parentTabbedPane;
        this.tabComponent = tabComponent;
        setOpaque(false);

        JButton pin = new JButton("📌");
        pin.setToolTipText("Anpinnen (als persistenten Tab öffnen)");
        pin.setBorderPainted(false);
        pin.setContentAreaFilled(false);
        pin.setFocusable(false);
        pin.setMargin(new Insets(0,4,0,4));
        pin.addActionListener(e -> {
            if (pinHandler != null) pinHandler.onPin();
            selectTab();
        });

        titleLabel = new JLabel(titleText);

        add(pin);
        add(titleLabel);

        // Header-Klick selektiert den Tab
        MouseAdapter selectOnClick = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { selectTab(); }
            @Override public void mouseClicked(MouseEvent e) { selectTab(); }
        };
        this.addMouseListener(selectOnClick);
        titleLabel.addMouseListener(selectOnClick);
        pin.addMouseListener(selectOnClick);
    }

    private void selectTab() {
        int idx = parentTabbedPane.indexOfComponent(tabComponent);
        if (idx >= 0 && parentTabbedPane.getSelectedIndex() != idx) {
            parentTabbedPane.setSelectedIndex(idx);
        }
    }

    public void setTitle(String newTitle) {
        titleLabel.setText(newTitle);
    }
}


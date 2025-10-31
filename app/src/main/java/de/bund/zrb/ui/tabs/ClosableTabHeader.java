package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Kleiner Tab-Header mit Titel-Label + rotem [x]-Button.
 *
 * editorTabs: das JTabbedPane, in dem der Tab steckt
 * component:  das Panel, das in diesem Tab als Content angezeigt wird
 * title:      Text, der angezeigt werden soll
 */
public class ClosableTabHeader extends JPanel {

    public ClosableTabHeader(final JTabbedPane editorTabs,
                             final Component component,
                             String title) {

        super(new FlowLayout(FlowLayout.LEFT, 4, 0));

        setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        JButton closeBtn = new JButton("✕"); // kleines X
        closeBtn.setMargin(new Insets(0, 4, 0, 4));
        closeBtn.setBorder(BorderFactory.createLineBorder(Color.RED));
        closeBtn.setForeground(Color.RED);
        closeBtn.setBackground(Color.WHITE);
        closeBtn.setOpaque(true);
        closeBtn.setFocusable(false);
        closeBtn.setToolTipText("Tab schließen");

        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = editorTabs.indexOfComponent(component);
                if (idx >= 0) {
                    editorTabs.removeTabAt(idx);
                }
            }
        });

        add(lbl);
        add(closeBtn);
    }
}

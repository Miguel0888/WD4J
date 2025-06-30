package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Right drawer: only a red record button.
 */
public class RightDrawer extends JPanel {

    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();

    public RightDrawer() {
        super(new BorderLayout());

        JButton recordButton = new JButton("\u2B24"); // gefüllter Kreis
        recordButton.setBackground(Color.RED);
        recordButton.setFocusPainted(false);

        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MenuCommand recordCommand = commandRegistry.getById("record.start").get();
                recordCommand.perform();
            }
        });

        add(recordButton, BorderLayout.NORTH);
    }
}

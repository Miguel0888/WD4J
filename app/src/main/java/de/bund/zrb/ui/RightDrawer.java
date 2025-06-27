package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.Command;
import de.bund.zrb.ui.commandframework.CommandContext;
import de.bund.zrb.ui.commandframework.CommandRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Right drawer: only a red record button.
 */
public class RightDrawer extends JPanel {

    private final CommandRegistry commandRegistry;

    public RightDrawer(CommandRegistry registry) {
        super(new BorderLayout());
        this.commandRegistry = registry;

        JButton recordButton = new JButton("\u2B24"); // gefüllter Kreis
        recordButton.setBackground(Color.RED);
        recordButton.setFocusPainted(false);

        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Command recordCommand = commandRegistry.getCommand("record.start");
                recordCommand.execute(new CommandContext());
            }
        });

        add(recordButton, BorderLayout.NORTH);
    }
}

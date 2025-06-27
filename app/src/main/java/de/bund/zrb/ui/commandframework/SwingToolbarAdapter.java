package de.bund.zrb.ui.commandframework;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Builds a Swing JToolBar from ToolbarConfig.
 */
public class SwingToolbarAdapter {

    private final CommandRegistry registry;

    public SwingToolbarAdapter(CommandRegistry registry) {
        this.registry = registry;
    }

    public JToolBar build(ToolbarConfig config) {
        JToolBar toolbar = new JToolBar();
        for (ToolbarButtonConfig buttonConfig : config.getButtons()) {
            JButton button = new JButton();
            button.setToolTipText(buttonConfig.getTooltip());
            Command command = registry.getCommand(buttonConfig.getId());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    command.execute(new CommandContext());
                }
            });
            toolbar.add(button);
        }
        return toolbar;
    }
}

package de.bund.zrb.ui.commands.tools;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.components.NotificationTestDialog;

import javax.swing.*;
import java.awt.*;

public class ShowGrowlTesterCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "debug.growlTester";
    }

    @Override
    public String getLabel() {
        return "Growl/Notification-Tester";
    }

    @Override
    public void perform() {
        Window parent = getActiveWindow();
        new NotificationTestDialog(parent).setVisible(true);
    }

    private Window getActiveWindow() {
        for (Window w : Window.getWindows()) {
            if (w.isActive()) return w;
        }
        return null;
    }
}

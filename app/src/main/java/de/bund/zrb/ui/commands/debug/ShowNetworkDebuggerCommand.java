package de.bund.zrb.ui.commands.debug;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.debug.NetworkDebuggerDialog;

import java.awt.*;

public class ShowNetworkDebuggerCommand extends ShortcutMenuCommand {
    @Override public String getId()   { return "debug.network"; }
    @Override public String getLabel(){ return "Network-Debugger"; }

    @Override public void perform() {
        Window parent = getActiveWindow();
        new NetworkDebuggerDialog(parent).setVisible(true);
    }

    private static Window getActiveWindow() {
        for (Window w : Window.getWindows()) if (w.isActive()) return w;
        return null;
    }
}

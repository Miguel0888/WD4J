package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.UserManagementDialog;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;

public class UserRegistryCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "users.manage";
    }

    @Override
    public String getLabel() {
        return "Zugangsdaten verwalten";
    }

    @Override
    public void perform() {
        SwingUtilities.invokeLater(() -> new UserManagementDialog(null).setVisible(true));
    }
}

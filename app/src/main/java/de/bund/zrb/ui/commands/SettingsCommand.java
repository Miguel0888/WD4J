package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "file.configure";
    }

    @Override
    public String getLabel() {
        return "Einstellungen...";
    }

    @Override
    public void perform() {
        // Aktuellen Wert laden
        Double websocketTimeout = SettingsService.getInstance().get("websocketTimeout", Double.class);
        double spinnerValue = websocketTimeout != null ? websocketTimeout : 30_000.0;

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                spinnerValue,
                0.0,                // min
                Double.MAX_VALUE,   // max
                100.0               // step
        );

        JSpinner websocketTimeoutSpinner = new JSpinner(spinnerModel);
        Dimension spinnerSize = websocketTimeoutSpinner.getPreferredSize();
        spinnerSize.width = 120;
        spinnerSize.height = 25;
        websocketTimeoutSpinner.setPreferredSize(spinnerSize);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Zeile: Label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("WebSocket Timeout (ms):"), gbc);

        // Zeile: Spinner
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(websocketTimeoutSpinner, gbc);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        int result = JOptionPane.showConfirmDialog(
                null,
                scrollPane,
                "Einstellungen",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            Map<String, Object> newSettings = new HashMap<String, Object>();

            double timeoutValue = (Double) websocketTimeoutSpinner.getValue();
            if (timeoutValue < 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Timeout darf nicht negativ sein.",
                        "Eingabefehler",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            newSettings.put("websocketTimeout", timeoutValue);

            newSettings.forEach(SettingsService.getInstance()::set);
        }
    }
}

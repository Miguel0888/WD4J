package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.ActionToolbar;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import java.awt.*;
import java.io.File;
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
        // --- existierende Werte laden ---
        Double websocketTimeout = SettingsService.getInstance().get("websocketTimeout", Double.class);
        double spinnerValue = websocketTimeout != null ? websocketTimeout : 30_000.0;

        String currentReportDir = SettingsService.getInstance().get("reportBaseDir", String.class);
        if (currentReportDir == null || currentReportDir.trim().isEmpty()) {
            currentReportDir = "C:/Reports";
        }

        // NEU: Context-Mode laden (Default: false = Page-Mode)
        Boolean contextModeSetting = SettingsService.getInstance().get("recording.contextMode", Boolean.class);
        boolean contextMode = contextModeSetting != null ? contextModeSetting.booleanValue() : true;

        // --- UI-Controls ---
        // WebSocket Timeout
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                spinnerValue, 0.0, Double.MAX_VALUE, 100.0
        );
        JSpinner websocketTimeoutSpinner = new JSpinner(spinnerModel);
        Dimension spinnerSize = websocketTimeoutSpinner.getPreferredSize();
        spinnerSize.width = 120;
        spinnerSize.height = 25;
        websocketTimeoutSpinner.setPreferredSize(spinnerSize);

        // Report-Verzeichnis
        JTextField reportDirField = new JTextField(currentReportDir, 28);
        JButton browseBtn = new JButton("…");
        browseBtn.setMargin(new Insets(2, 8, 2, 8));
        browseBtn.addActionListener(ev -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            File preset = new File(reportDirField.getText());
            if (preset.exists()) chooser.setCurrentDirectory(preset.isDirectory() ? preset : preset.getParentFile());
            int res = chooser.showOpenDialog(null);
            if (res == JFileChooser.APPROVE_OPTION) {
                reportDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel reportDirPanel = new JPanel(new BorderLayout(5,0));
        reportDirPanel.add(reportDirField, BorderLayout.CENTER);
        reportDirPanel.add(browseBtn, BorderLayout.EAST);

        // Recording-Modus (Checkbox ODER Radiobuttons)
        JCheckBox chkContextMode = new JCheckBox("Recording im Context-Mode (ein BrowserContext pro Benutzer)", contextMode);

        // --- Layout ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        int row = 0;

        // Row 1: Context-Mode
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1;
        contentPanel.add(chkContextMode, gbc);
        gbc.gridwidth = 1;

        // Row 2: WebSocket Timeout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        contentPanel.add(new JLabel("WebSocket Timeout (ms):"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1;
        contentPanel.add(websocketTimeoutSpinner, gbc);

        // Row 3: Report-Verzeichnis
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        contentPanel.add(new JLabel("Report-Verzeichnis:"), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(reportDirPanel, gbc);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(520, 260)); // leicht erhöht

        int result = JOptionPane.showConfirmDialog(
                null, scrollPane, "Einstellungen",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            Map<String, Object> newSettings = new HashMap<>();

            double timeoutValue = (Double) websocketTimeoutSpinner.getValue();
            if (timeoutValue < 0) {
                JOptionPane.showMessageDialog(
                        null, "Timeout darf nicht negativ sein.", "Eingabefehler", JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            String reportDir = reportDirField.getText().trim();
            if (reportDir.isEmpty()) {
                JOptionPane.showMessageDialog(
                        null, "Bitte ein Report-Verzeichnis angeben.", "Eingabefehler", JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            newSettings.put("websocketTimeout", timeoutValue);
            newSettings.put("reportBaseDir", reportDir);

            // NEU: Context-Mode speichern
            newSettings.put("recording.contextMode", chkContextMode.isSelected());

            newSettings.forEach(SettingsService.getInstance()::set);
        }
    }
}

package de.bund.zrb.ui.system;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog für Systemeinstellungen der Browser (Pfade, Profile, WS-Defaults).
 */
public class BrowserSystemSettingsDialog extends JDialog {

    private final JTextField tfDefaultUrl   = new JTextField(24);
    private final JSpinner   spDefaultPort  = new JSpinner(new SpinnerNumberModel(9222, 0, 65535, 1));
    private final JTextField tfWsEndpoint   = new JTextField(24);

    private final JTextField tfFirefoxPath  = new JTextField(32);
    private final JTextField tfChromiumPath = new JTextField(32);
    private final JTextField tfEdgePath     = new JTextField(32);

    private final JTextField tfFirefoxProf  = new JTextField(24);
    private final JTextField tfChromiumProf = new JTextField(24);
    private final JTextField tfEdgeProf     = new JTextField(24);

    public BrowserSystemSettingsDialog(Window owner) {
        super(owner, "Systemeinstellungen – Browser", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        loadFromSettings();
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10,12,10,12));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,8,6,8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int row = 0;
        // Defaults
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Default WS-URL:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfDefaultUrl, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Default Port:"), g);
        g.gridx = 1; g.gridy = row++; form.add(spDefaultPort, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("WS Endpoint:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfWsEndpoint, g);

        // Browser Pfade
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Firefox Pfad:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfFirefoxPath, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Chromium Pfad:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfChromiumPath, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Edge Pfad:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfEdgePath, g);

        // Profile
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Firefox Profil:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfFirefoxProf, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Chromium Profil:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfChromiumProf, g);
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Edge Profil:"), g);
        g.gridx = 1; g.gridy = row++; form.add(tfEdgeProf, g);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8,0));
        JButton btSave = new JButton("Übernehmen");
        JButton btClose= new JButton("Schließen");
        btSave.addActionListener(e -> saveToSettings());
        btClose.addActionListener(e -> dispose());
        footer.add(btSave);
        footer.add(btClose);

        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void loadFromSettings() {
        SettingsService s = SettingsService.getInstance();
        String defUrl = s.get("browser.defaultUrl", String.class);
        Integer defPort = s.get("browser.defaultPort", Integer.class);
        String wsEp = s.get("browser.wsEndpoint", String.class);
        tfDefaultUrl.setText(defUrl != null ? defUrl : "ws://127.0.0.1");
        spDefaultPort.setValue(defPort != null ? defPort : 9222);
        tfWsEndpoint.setText(wsEp != null ? wsEp : "/session");

        String ff = s.get("browser.firefox.path", String.class);
        String ch = s.get("browser.chromium.path", String.class);
        String ed = s.get("browser.edge.path", String.class);
        tfFirefoxPath.setText(ff != null ? ff : "C:/Program Files/Mozilla Firefox/firefox.exe");
        tfChromiumPath.setText(ch != null ? ch : "C:/Program Files/Google/Chrome/Application/chrome.exe");
        tfEdgePath.setText(ed != null ? ed : "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe");

        String ffp = s.get("browser.profile.firefox", String.class);
        String chp = s.get("browser.profile.chromium", String.class);
        String edp = s.get("browser.profile.edge", String.class);
        tfFirefoxProf.setText(ffp != null ? ffp : "C:/FirefoxProfile");
        tfChromiumProf.setText(chp != null ? chp : "C:/ChromeProfile");
        tfEdgeProf.setText(edp != null ? edp : "C:/EdgeProfile");
    }

    private void saveToSettings() {
        SettingsService s = SettingsService.getInstance();
        s.set("browser.defaultUrl", tfDefaultUrl.getText().trim());
        s.set("browser.defaultPort", ((Number)spDefaultPort.getValue()).intValue());
        s.set("browser.wsEndpoint", tfWsEndpoint.getText().trim());

        s.set("browser.firefox.path",  tfFirefoxPath.getText().trim());
        s.set("browser.chromium.path", tfChromiumPath.getText().trim());
        s.set("browser.edge.path",     tfEdgePath.getText().trim());

        s.set("browser.profile.firefox",  tfFirefoxProf.getText().trim());
        s.set("browser.profile.chromium", tfChromiumProf.getText().trim());
        s.set("browser.profile.edge",     tfEdgeProf.getText().trim());

        // Zusätzlich System Properties setzen, damit der Adapter sofort die neuen Werte nutzen kann
        System.setProperty("browser.defaultUrl", tfDefaultUrl.getText().trim());
        System.setProperty("browser.defaultPort", String.valueOf(((Number)spDefaultPort.getValue()).intValue()));
        System.setProperty("browser.wsEndpoint", tfWsEndpoint.getText().trim());
        System.setProperty("browser.firefox.path",  tfFirefoxPath.getText().trim());
        System.setProperty("browser.chromium.path", tfChromiumPath.getText().trim());
        System.setProperty("browser.edge.path",     tfEdgePath.getText().trim());
        System.setProperty("browser.profile.firefox",  tfFirefoxProf.getText().trim());
        System.setProperty("browser.profile.chromium", tfChromiumProf.getText().trim());
        System.setProperty("browser.profile.edge",     tfEdgeProf.getText().trim());

        // Adapter-Konfiguration aus System Properties aktualisieren
        de.bund.zrb.config.BrowserSystemConfig.initFromSystemProperties();

        JOptionPane.showMessageDialog(this, "Einstellungen übernommen.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}

package de.bund.zrb.ui.debug;

import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.components.AnimatedTimerCircle;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Timer;
import java.util.TimerTask;

public class OtpTestDialog extends JDialog {

    private final JLabel timerLabel;
    private final JLabel otpCodeLabel;
    private final AnimatedTimerCircle timerCircle;
    private final Timer timer = new Timer(true);
    private final String otpSecret;

    public OtpTestDialog(Window parent, UserRegistry.User user) {
        super(parent, "OTP-Test für " + user.getUsername(), ModalityType.APPLICATION_MODAL);
        this.otpSecret = user.getOtpSecret();

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(350, 180);
        setLocationRelativeTo(parent);

        // OTP-Anzeige
        otpCodeLabel = new JLabel();
        otpCodeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        otpCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Timer-Kreis (links)
        timerLabel = new JLabel();
        timerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        timerLabel.setForeground(Color.WHITE);
        timerCircle = new AnimatedTimerCircle(timerLabel);
        timerCircle.setPreferredSize(new Dimension(80, 80));

        // Hauptpanel mit Kreis links und OTP rechts
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(timerCircle, BorderLayout.WEST);
        // OTP-Code + Copy-Button nebeneinander
        JPanel otpCodePanel = new JPanel(new BorderLayout());

        otpCodePanel.add(otpCodeLabel, BorderLayout.CENTER);

        // Copy-to-Clipboard-Button mit Unicode-Symbol
        JButton copyButton = new JButton("📋");
        copyButton.setToolTipText("OTP in Zwischenablage kopieren");
        copyButton.setMargin(new Insets(2, 6, 2, 6));
        copyButton.addActionListener(e -> {
            String otpText = otpCodeLabel.getText();
            if (!otpText.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(otpText), null
                );
                copyButton.setToolTipText("Kopiert ✔");
            }
        });

        otpCodePanel.add(copyButton, BorderLayout.EAST);

        centerPanel.add(otpCodePanel, BorderLayout.CENTER);

        // Schließen-Button
        JButton closeButton = new JButton("Schließen");
        closeButton.addActionListener(e -> {
            timer.cancel();
            dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        updateDisplay();
        startTimer();
    }

    private void startTimer() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateDisplay());
            }
        }, 0, 1000);
    }

    private void updateDisplay() {
        try {
            long now = System.currentTimeMillis();
            int seconds = (int) (now / 1000);
            int secondsRemaining = 30 - (seconds % 30);
            float progress = secondsRemaining / 30.0f;

            int otp = TotpService.getInstance().generateCurrentOtp(otpSecret);
            otpCodeLabel.setText(String.format("%06d", otp));
            timerLabel.setText(secondsRemaining + "s");
            timerCircle.setProgress(progress);
        } catch (Exception ex) {
            otpCodeLabel.setText("Fehler");
            timerLabel.setText("");
        }
    }
}

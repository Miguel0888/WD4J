package de.bund.zrb.ui.commands;

import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Edit regex presets (title/message) stored in regex.json via RegexPatternRegistry.
 * Keep UI simple: two multi-line text areas (one per list), one-per-line entries.
 */
public class RegexPresetsCommand extends ShortcutMenuCommand {

    @Override
    public String getId() {
        return "file.regexPresets";
    }

    @Override
    public String getLabel() {
        return "Regex-Presets…";
    }

    @Override
    public void perform() {
        final RegexPatternRegistry reg = RegexPatternRegistry.getInstance();

        // Build text areas prefilled with presets, one-per-line
        final JTextArea taTitle = new JTextArea(joinLines(reg.getTitlePresets()));
        final JTextArea taMsg   = new JTextArea(joinLines(reg.getMessagePresets()));

        taTitle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        taMsg.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        taTitle.setRows(10);
        taMsg.setRows(12);

        JScrollPane spTitle = new JScrollPane(taTitle);
        JScrollPane spMsg   = new JScrollPane(taMsg);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,8,6,8);
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;

        int row = 0;
        // Title block
        gc.gridx = 0; gc.gridy = row; gc.weighty = 0;
        content.add(new JLabel("Title-Presets (ein Regex pro Zeile):"), gc);
        row++;
        gc.gridx = 0; gc.gridy = row; gc.weighty = 0.5;
        content.add(spTitle, gc);

        // Message block
        row++;
        gc.gridx = 0; gc.gridy = row; gc.weighty = 0;
        content.add(new JLabel("Message-Presets (ein Regex pro Zeile):"), gc);
        row++;
        gc.gridx = 0; gc.gridy = row; gc.weighty = 0.5;
        content.add(spMsg, gc);

        int res = JOptionPane.showConfirmDialog(
                null, content, "Regex-Presets",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return;

        List<String> titleList = splitLines(taTitle.getText());
        List<String> msgList   = splitLines(taMsg.getText());

        reg.setTitlePresets(titleList);
        reg.setMessagePresets(msgList);

        JOptionPane.showMessageDialog(null, "Regex-Presets gespeichert.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- helpers ---

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private static List<String> splitLines(String text) {
        List<String> list = new ArrayList<String>();
        if (text == null) return list;
        String[] arr = text.split("\\r?\\n");
        for (String s : arr) {
            if (s == null) continue;
            String t = s.trim();
            if (t.length() == 0) continue;
            if (!list.contains(t)) list.add(t);
        }
        return list;
    }
}

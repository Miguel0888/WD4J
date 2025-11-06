package de.bund.zrb.ui.util;

import javax.swing.*;
import java.awt.*;

/** Collect user decisions for adding a regex to the registry. */
public final class RegexAddDialog {

    public static class Decision {
        public final boolean confirmed;
        public final RegexRegistryFacade.Target target;

        public Decision(boolean confirmed, RegexRegistryFacade.Target target) {
            this.confirmed = confirmed;
            this.target = target;
        }
    }

    private RegexAddDialog() { }

    /** Ask user for target (if needed) and confirm adding the regex. */
    public static Decision ask(Component parent, String regex, Integer viewColumnOrNull) {
        RegexRegistryFacade.Target target = suggestTarget(viewColumnOrNull);

        // Build a small panel to display regex nicely
        JTextArea ta = new JTextArea(regex);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ta.getFont().getSize()));
        ta.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane sc = new JScrollPane(ta);
        sc.setPreferredSize(new Dimension(560, 140));

        // If target is UNKNOWN (null), ask user explicitly
        if (target == null) {
            Object[] options = new Object[]{"Title-Presets", "Message-Presets", "Abbrechen"};
            int choice = JOptionPane.showOptionDialog(
                    parent,
                    new Object[]{
                            "Bitte Ziel-Registry wählen:",
                            "RegEx wird wie angezeigt übernommen:",
                            sc
                    },
                    "RegEx zur Registry hinzufügen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            if (choice == 0) target = RegexRegistryFacade.Target.TITLE;
            else if (choice == 1) target = RegexRegistryFacade.Target.MESSAGE;
            else return new Decision(false, null);
        } else {
            int res = JOptionPane.showConfirmDialog(
                    parent,
                    new Object[]{
                            "Diesen RegEx wirklich zu den " + (target == RegexRegistryFacade.Target.TITLE ? "Title-Presets" : "Message-Presets") + " hinzufügen?",
                            sc
                    },
                    "RegEx zur Registry hinzufügen",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (res != JOptionPane.OK_OPTION) return new Decision(false, null);
        }

        return new Decision(true, target);
    }

    /** Suggest target based on column index: 2=Title, 3=Message; otherwise null. */
    private static RegexRegistryFacade.Target suggestTarget(Integer viewColumn) {
        if (viewColumn == null) return null;
        if (viewColumn.intValue() == 2) return RegexRegistryFacade.Target.TITLE;
        if (viewColumn.intValue() == 3) return RegexRegistryFacade.Target.MESSAGE;
        return null;
    }
}

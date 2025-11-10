package de.bund.zrb;

import static de.bund.zrb.settings.SettingsBootstrap.ensureUserSettingsPresent;

public class Main {

    public static final String RECORD_FLAG = "-d";
    public static final String ALT_RECORD_FLAG = "--debug";
    public static final String DEBUG_ON_FAIL = "--debugOnFail";
    public static final String DEBUG_PLAYERS = "--debugPlayers";

    public static void main(String[] args) {
        // Ensure default user settings are initialized once if missing
        ensureUserSettingsPresent();

        // Call existing startup logic (unchanged)
        de.bund.zrb.service.SettingsService.getInstance().initAdapter();
        de.bund.zrb.ui.MainFrame.main(args);
    }
}

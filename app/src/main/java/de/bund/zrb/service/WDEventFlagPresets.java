package de.bund.zrb.service;

import de.bund.zrb.websocket.WDEventNames;
import java.util.EnumMap;

public final class WDEventFlagPresets {
    private WDEventFlagPresets() {}

    public static EnumMap<WDEventNames, Boolean> recorderDefaults() {
        EnumMap<WDEventNames, Boolean> m = new EnumMap<>(WDEventNames.class);
        for (WDEventNames e : new WDEventNames[] {
                WDEventNames.CONTEXT_CREATED,
                WDEventNames.CONTEXT_DESTROYED,
                WDEventNames.DOM_CONTENT_LOADED,
                WDEventNames.LOAD,
                WDEventNames.BEFORE_REQUEST_SENT,
                WDEventNames.RESPONSE_STARTED,
                WDEventNames.RESPONSE_COMPLETED,
                WDEventNames.FETCH_ERROR,
                WDEventNames.NAVIGATION_STARTED,
                WDEventNames.FRAGMENT_NAVIGATED,
                WDEventNames.ENTRY_ADDED,
                WDEventNames.USER_PROMPT_OPENED,
                WDEventNames.REALM_CREATED,
                WDEventNames.FILE_DIALOG_OPENED
        }) m.put(e, Boolean.TRUE);
        return m;
    }
}

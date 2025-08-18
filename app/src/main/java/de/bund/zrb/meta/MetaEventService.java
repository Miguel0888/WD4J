package de.bund.zrb.meta;

import de.bund.zrb.websocket.WDEvent;

public interface MetaEventService {

    // --- Bestehende API (behalten für Abwärtskompatibilität) ---
    void addListener(MetaEventListener listener);
    void removeListener(MetaEventListener listener);
    void publish(MetaEvent event);

    // --- Neue, typsichere API für WDEvents ---
    void addListenerForUserContext(WDEventListener listener, String userContextId);
    void addListenerForBrowsingContext(WDEventListener listener, String browsingContextId);
    void removeListener(WDEventListener listener);

    /** Publish typed WDEvent; resolve contexts via registry if needed. */
    void publish(WDEvent<?> event);
}

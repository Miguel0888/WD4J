package de.bund.zrb.meta;

import de.bund.zrb.websocket.WDEvent;

/** Listen for typed WebDriver-BiDi events.*/
public interface WDEventListener {

    void onWDEvent(WDEvent<?> event);
}

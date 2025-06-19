package de.bund.zrb.event;

import com.google.gson.JsonObject;
import de.bund.zrb.api.markerInterfaces.WDModule;
import de.bund.zrb.websocket.WDEvent;
import de.bund.zrb.type.log.WDLogEntry;
import de.bund.zrb.websocket.WDEventNames;

public class WDLogEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends WDEvent<WDLogEntry /*aka. EntryAdded*/> {
        private final String method = WDEventNames.ENTRY_ADDED.getName();

        public EntryAdded(JsonObject json) {
            super(json, WDLogEntry.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}

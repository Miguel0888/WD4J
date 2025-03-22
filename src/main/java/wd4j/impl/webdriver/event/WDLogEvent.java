package wd4j.impl.webdriver.event;

import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.websocket.WDEvent;
import wd4j.impl.webdriver.type.log.WDLogEntry;
import wd4j.impl.webdriver.type.script.WDStackTrace;
import wd4j.impl.webdriver.type.script.WDSource;

public class WDLogEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends WDEvent<WDLogEntry /*aka. EntryAdded*/> {
        private final String method = WDEventMapping.ENTRY_ADDED.getName();

        public EntryAdded(JsonObject json) {
            super(json, WDLogEntry.class);
        }

        @Override
        public String getMethod() {
            return method;
        }
    }
}

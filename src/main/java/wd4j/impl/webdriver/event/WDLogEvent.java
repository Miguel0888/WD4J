package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.websocket.WDEvent;
import wd4j.impl.webdriver.type.log.WDLogEntry;

public class WDLogEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends WDEvent<EntryAdded.EntryAddedParameters> {
        private String method = WDEventMapping.ENTRY_ADDED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class EntryAddedParameters {
            private WDLogEntry logEntry;

            public EntryAddedParameters(WDLogEntry logEntry) {
                this.logEntry = logEntry;
            }

            public WDLogEntry getEntry() {
                return logEntry;
            }

            public void setEntry(WDLogEntry logEntry) {
                this.logEntry = logEntry;
            }

            @Override
            public String toString() {
                return "EntryAddedParameters{" +
                        "entry=" + logEntry +
                        '}';
            }
        }
    }
}

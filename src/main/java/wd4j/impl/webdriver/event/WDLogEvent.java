package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.WDModule;
import wd4j.impl.websocket.WDEvent;
import wd4j.impl.webdriver.type.log.WDLogEntry;

public class WDLogEvent implements WDModule {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends WDEvent<EntryAdded.EntryAddedParameters> {
        private String method = WDMethodEvent.ENTRY_ADDED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class EntryAddedParameters {
            private WDLogEntry WDLogEntry;

            public EntryAddedParameters(WDLogEntry WDLogEntry) {
                this.WDLogEntry = WDLogEntry;
            }

            public WDLogEntry getEntry() {
                return WDLogEntry;
            }

            public void setEntry(WDLogEntry WDLogEntry) {
                this.WDLogEntry = WDLogEntry;
            }

            @Override
            public String toString() {
                return "EntryAddedParameters{" +
                        "entry=" + WDLogEntry +
                        '}';
            }
        }
    }
}

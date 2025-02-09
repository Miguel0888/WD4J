package wd4j.impl.webdriver.event;

import wd4j.impl.markerInterfaces.Module;
import wd4j.impl.websocket.Event;
import wd4j.impl.webdriver.type.log.LogEntry;

public class LogEvent implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends Event<EntryAdded.EntryAddedParameters> {
        private String method = MethodEvent.ENTRY_ADDED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class EntryAddedParameters {
            private LogEntry logEntry;

            public EntryAddedParameters(LogEntry logEntry) {
                this.logEntry = logEntry;
            }

            public LogEntry getEntry() {
                return logEntry;
            }

            public void setEntry(LogEntry logEntry) {
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

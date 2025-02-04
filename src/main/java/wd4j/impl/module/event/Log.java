package wd4j.impl.module.event;

import wd4j.impl.module.generic.Module;
import wd4j.impl.module.websocket.Event;
import wd4j.impl.module.type.LogEntry;

public class Log implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class EntryAdded extends Event<EntryAdded.EntryAddedParameters> {
        private String method = Method.ENTRY_ADDED.getName();

        @Override
        public String getMethod() {
            return method;
        }

        public static class EntryAddedParameters {
            private LogEntry entry;

            public EntryAddedParameters(LogEntry entry) {
                this.entry = entry;
            }

            public LogEntry getEntry() {
                return entry;
            }

            public void setEntry(LogEntry entry) {
                this.entry = entry;
            }

            @Override
            public String toString() {
                return "EntryAddedParameters{" +
                        "entry=" + entry +
                        '}';
            }
        }
    }
}

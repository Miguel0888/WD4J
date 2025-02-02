package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.core.generic.Command;

import java.util.List;

public class Session {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Status extends CommandImpl<Status.ParamsImpl> {

        public Status() {
            super("session.status", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter notwendig für diesen Command
        }
    }

    public static class New extends CommandImpl<New.ParamsImpl> {

        public New(String browserName) {
            super("session.new", new ParamsImpl(browserName));
        }

        // Parameterklasse
        public static class ParamsImpl implements Command.Params {
            private  final Capabilities capabilities;

            public ParamsImpl(String browserName) {
                this.capabilities = new Capabilities(browserName);
            }

            private static class Capabilities {
                private  final String browserName;

                public Capabilities(String browserName) {
                    this.browserName = browserName;
                }
            }
        }
    }

    public static class End extends CommandImpl<End.ParamsImpl> {

        public End() {
            super("session.delete", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class Subscribe extends CommandImpl<Subscribe.ParamsImpl> {

        public Subscribe(List<String> events) {
            super("session.subscribe", new ParamsImpl(events));
        }

        public static class ParamsImpl implements Command.Params {
            private final List<String> events;

            public ParamsImpl(List<String> events) {
                if (events == null || events.isEmpty()) {
                    throw new IllegalArgumentException("Events list must not be null or empty.");
                }
                this.events = events;
            }
        }
    }

    public static class Unsubscribe extends CommandImpl<Unsubscribe.ParamsImpl> {

        public Unsubscribe(List<String> events) {
            super("session.unsubscribe", new ParamsImpl(events));
        }

        public static class ParamsImpl implements Command.Params {
            private final List<String> events;

            public ParamsImpl(List<String> events) {
                if (events == null || events.isEmpty()) {
                    throw new IllegalArgumentException("Events list must not be null or empty.");
                }
                this.events = events;
            }
        }
    }



}
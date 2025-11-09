package de.bund.zrb.command.request.parameters.browser;

import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.browser.WDClientWindow;
import de.bund.zrb.api.WDCommand;

//@JsonAdapter(GenericWrapperAdapter.class) // Not required, since for the GenericWrapper is searched for in the factory
public abstract class SetClientWindowStateParameters implements WDCommand.Params {
    // Feldname angepasst, damit JSON-Schlüssel "clientWindow" statt "WDClientWindow" erzeugt wird
    private final WDClientWindow clientWindow;

    public SetClientWindowStateParameters(WDClientWindow clientWindow) {
        this.clientWindow = clientWindow;
    }

    public WDClientWindow getClientWindow() {
        return clientWindow;
    }

    public static class ClientWindowNamedState extends SetClientWindowStateParameters {
        private final State state;

        public ClientWindowNamedState(WDClientWindow clientWindow, State state) {
            super(clientWindow);
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public enum State implements EnumWrapper {
            FULLSCREEN("fullscreen"),
            MAXIMIZED("maximized"),
            MINIMIZED("minimized");

            private final String value;

            State(String value) {
                this.value = value;
            }

            @Override // confirmed design
            public String value() {
                return value;
            }
        }
    }

    public static class ClientWindowRectState extends SetClientWindowStateParameters {
        private final State state = State.NORMAL;
        private final Integer width; // optional
        private final Integer height; // optional
        private final Integer x; // optional
        private final Integer y; // optional

        public ClientWindowRectState(WDClientWindow clientWindow) {
            this(clientWindow, null, null, null, null);
        }

        public ClientWindowRectState(WDClientWindow clientWindow, Integer width, Integer height, Integer x, Integer y) {
            super(clientWindow);
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        public State getState() {
            return state;
        }

        public Integer getWidth() {
            return width;
        }

        public Integer getHeight() {
            return height;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
            return y;
        }

        public enum State implements EnumWrapper {
            NORMAL("normal");

            private final String value;

            State(String value) {
                this.value = value;
            }

            @Override // confirmed design
            public String value() {
                return value;
            }
        }
    }
}

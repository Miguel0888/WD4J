package wd4j.impl.webdriver.command.request.parameters.browser;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.websocket.Command;

//@JsonAdapter(GenericWrapperAdapter.class) // Not required, since for the GenericWrapper is searched for in the factory
public abstract class SetClientWindowStateParameters implements Command.Params {
    private final ClientWindow clientWindow;

    public SetClientWindowStateParameters(ClientWindow clientWindow) {
        this.clientWindow = clientWindow;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

    public static class ClientWindowNamedState extends SetClientWindowStateParameters {
        private final State state;

        public ClientWindowNamedState(ClientWindow clientWindow, State state) {
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

        public ClientWindowRectState(ClientWindow clientWindow) {
            this(clientWindow, null, null, null, null);
        }

        public ClientWindowRectState(ClientWindow clientWindow, Integer width, Integer height, Integer x, Integer y) {
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

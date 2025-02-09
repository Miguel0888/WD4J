package wd4j.impl.webdriver.command.request.parameters.browser;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.websocket.Command;

public class SetClientWindowStateParameters implements Command.Params {
    private final ClientWindow clientWindow;
    private final ClientWindowState windowState;

    public SetClientWindowStateParameters(ClientWindow clientWindow, ClientWindowState windowState) {
        this.clientWindow = clientWindow;
        this.windowState = windowState;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

    public ClientWindowState getWindowState() {
        return windowState;
    }

    public interface ClientWindowState {
        State getState();

        interface State extends EnumWrapper {}
    }

    public static class ClientWindowNamedState implements ClientWindowState {
        private final State state;

        public ClientWindowNamedState(State state) {
            this.state = state;
        }

        @Override // confirmed design
        public State getState() {
            return state;
        }

        public enum State implements ClientWindowState.State {
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

    public static class ClientWindowRectState implements ClientWindowState {
        private final State state = State.NORMAL;
        private final char width;
        private final char height;
        private final int x;
        private final int y;

        public ClientWindowRectState(char width, char height, int x, int y) {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        @Override // confirmed design
        public State getState() {
            return state;
        }

        public char getWidth() {
            return width;
        }

        public char getHeight() {
            return height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public enum State implements ClientWindowState.State {
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

package wd4j.impl.webdriver.command.request.parameters.browser;

import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.mapping.GenericWrapper;
import wd4j.impl.webdriver.mapping.GenericWrapperAdapter;
import wd4j.impl.webdriver.type.browser.ClientWindow;
import wd4j.impl.websocket.Command;

//@JsonAdapter(GenericWrapperAdapter.class) // Not required, since for the GenericWrapper is searched for in the factory
public class SetClientWindowStateParameters<T extends SetClientWindowStateParameters.ClientWindowState> implements Command.Params, GenericWrapper {
    private final ClientWindow clientWindow;
    private final T value;  // Intern wird das als "value" gespeichert, aber nicht so serialisiert!

    @Override // confirmed design
    public T value() {
        return value;
    }

    public SetClientWindowStateParameters(ClientWindow clientWindow, T value) {
        this.clientWindow = clientWindow;
        this.value = value;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

    public ClientWindowState getValue() {
        return value;
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
        private final Character width; // optional
        private final Character height; // optional
        private final Integer x; // optional
        private final Integer y; // optional

        public ClientWindowRectState() {
            this(null, null, null, null);
        }

        public ClientWindowRectState(Character width, Character height, Integer x, Integer y) {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        @Override // confirmed design
        public State getState() {
            return state;
        }

        public Character getWidth() {
            return width;
        }

        public Character getHeight() {
            return height;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
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

package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.websocket.Command;

public class StorageRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class GetCookies extends CommandImpl<GetCookies.ParamsImpl> implements CommandData {

        public GetCookies(String contextId) {
            super("storage.getCookies", new ParamsImpl(contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;

            public ParamsImpl(String contextId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
            }
        }
    }

    public static class SetCookie extends CommandImpl<SetCookie.ParamsImpl> implements CommandData {

        public SetCookie(String contextId, String name, String value) {
            super("storage.setCookie", new ParamsImpl(contextId, name, value));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String name;
            private final String value;

            public ParamsImpl(String contextId, String name, String value) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Cookie name must not be null or empty.");
                }
                if (value == null || value.isEmpty()) {
                    throw new IllegalArgumentException("Cookie value must not be null or empty.");
                }
                this.context = contextId;
                this.name = name;
                this.value = value;
            }
        }
    }

    public static class DeleteCookies extends CommandImpl<DeleteCookies.ParamsImpl> implements CommandData {

        public DeleteCookies(String contextId, String name) {
            super("storage.deleteCookies", new ParamsImpl(contextId, name));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String name;

            public ParamsImpl(String contextId, String name) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Cookie name must not be null or empty.");
                }
                this.context = contextId;
                this.name = name;
            }
        }
    }
}
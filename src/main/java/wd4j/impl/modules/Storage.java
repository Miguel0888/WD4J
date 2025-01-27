package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Module;

public class Storage implements Module {

    private final WebSocketConnection webSocketConnection;

    public Storage(WebSocketConnection webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves cookies for the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @return A JSON string containing the cookies.
     * @throws RuntimeException if the operation fails.
     */
    public String getCookies(String contextId) {
        try {
            String response = webSocketConnection.send(new GetCookies(contextId));
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonObject("result").toString();
        } catch (RuntimeException e) {
            System.out.println("Error retrieving cookies: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param name      The name of the cookie.
     * @param value     The value of the cookie.
     * @throws RuntimeException if the operation fails.
     */
    public void setCookie(String contextId, String name, String value) {
        try {
            webSocketConnection.send(new SetCookie(contextId, name, value));
            System.out.println("Cookie set: " + name + " = " + value + " in context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error setting cookie: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes a cookie in the specified browsing context.
     *
     * @param contextId The ID of the browsing context.
     * @param name      The name of the cookie to delete.
     * @throws RuntimeException if the operation fails.
     */
    public void deleteCookie(String contextId, String name) {
        try {
            webSocketConnection.send(new DeleteCookies(contextId, name));
            System.out.println("Cookie deleted: " + name + " from context: " + contextId);
        } catch (RuntimeException e) {
            System.out.println("Error deleting cookie: " + e.getMessage());
            throw e;
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class GetCookies extends CommandImpl<GetCookies.ParamsImpl> {

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

    public static class SetCookie extends CommandImpl<SetCookie.ParamsImpl> {

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

    public static class DeleteCookies extends CommandImpl<DeleteCookies.ParamsImpl> {

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
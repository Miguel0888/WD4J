package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.helper.BrowserType;
import wd4j.helper.JsonObjectBuilder;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Event;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Session implements Module {
    private String sessionId; // if available
    private final BrowserType browserType;
    private final WebSocketConnection webSocketConnection;
    private String defaultContextId; // Speichert die Standard-Kontext-ID
    private List<BrowsingContext> browsingContext = new ArrayList<>(); // ToDo: Maybe use a HashMap instead?

    /**
     * Erstellt eine neue Session und gibt diese zurück.
     * Da einige Browser einen Standard-Kontext erstellen, wird mit diesem direkt ein neuer Browsing-Kontext erstellt.
     * Damit das Verhalten konsistent ist, wird ein neuer Kontext erstellt, wenn kein Standard-Kontext gefunden wird.
     *
     * @param browserType Der Browsertyp
     * @return Die erstellte Session
     */
    public Session(BrowserType browserType, WebSocketConnection webSocketConnection) throws ExecutionException, InterruptedException {
        this.browserType = browserType;
        this.webSocketConnection = webSocketConnection;

        // Register for events
        webSocketConnection.addEventListener(event -> onEvent(event));

        // Create a new session
        String sessionResponse = newSession(browserType.name()).get(); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        processInitResponse(sessionResponse);
        if(defaultContextId == null) {
            // Fallback zu browsingContext.getTree, wenn kein Kontext gefunden wurde
            System.out.println("--- Keine default Context-ID gefunden. Führe browsingContext.getTree aus. ---");
            defaultContextId = fetchDefaultContextFromTree();
        }
        System.out.println("Context ID: " + defaultContextId);

        if(defaultContextId != null) {
            // BrowsingContext erstellen und speichern
            this.browsingContext.add(new BrowsingContext(webSocketConnection, defaultContextId));
            System.out.println("BrowsingContext erstellt mit ID: " + defaultContextId);
        }
        else {
            System.out.println("No default context found, creating a new one.");
            this.browsingContext.add(new BrowsingContext(webSocketConnection)); // ToDo: This step is optional!
        }
    }

    /**
     * Verwendet eine vorhandene Session-ID, um eine neue Session zu erstellen. Es wird kein new-Command ausgeführt.
     */
    public Session(BrowserType browserType, WebSocketConnection webSocketConnection, String SessionId) throws ExecutionException, InterruptedException {
        this.browserType = browserType;
        this.webSocketConnection = webSocketConnection;
        this.sessionId = SessionId;

        // ToDo: Get the defaultContextId from the SessionId OR create a new one!
    }

    private void processInitResponse(String sessionResponse) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(sessionResponse, JsonObject.class);
        JsonObject result = jsonResponse.getAsJsonObject("result");

        // Prüfe, ob ein Default Browsing-Kontext in der Antwort enthalten ist
        if (result != null && result.has("contexts")) {
            JsonObject context = result.getAsJsonArray("contexts")
                    .get(0)
                    .getAsJsonObject();
            if (context.has("context")) {
                defaultContextId = context.get("context").getAsString();
                System.out.println("--- Browsing Context-ID gefunden: " + defaultContextId);
            }
        }

        // Prüfe, ob die Antwort eine Session-ID enthält
        if (result != null && result.has("sessionId")) {
            sessionId = result.get("sessionId").getAsString();
            System.out.println("--- Session-ID gefunden: " + sessionId);
        }
    }

    // Fallback-Methode: Kontext über getTree suchen
    private String fetchDefaultContextFromTree() {
        CommandImpl getTreeCommand = new GetTreeCommand();

        try {
            String response = webSocketConnection.send(getTreeCommand);

            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");

            if (result != null && result.has("contexts")) {
                return result.getAsJsonArray("contexts")
                        .get(0)
                        .getAsJsonObject()
                        .get("context")
                        .getAsString();
            }
        } catch (RuntimeException e) {
            System.out.println("Error fetching context tree: " + e.getMessage());
            throw e;
        }

        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void onEvent(Event event) {
        switch (event.getType()) {
            case "session.created":
                System.out.println("Session created: " + event.getData());
                break;
            case "session.deleted":
                System.out.println("Session deleted: " + event.getData());
                break;
            default:
                System.out.println("Unhandled event: " + event.getType());
        }
    }

    public CompletableFuture<String> status() {
        return webSocketConnection.sendAsync(new StatusCommand());
    }

    // new() - Since plain "new" is a reserved word in Java!
    public CompletableFuture<String> newSession(String browserName) {
        return webSocketConnection.sendAsync(new Session.NewSessionCommand(browserName));
    }

    // end() - In corespondance to new!
    public CompletableFuture<String> endSession() {
        // ToDo: Maybe close all BrowsingContexts?
        CommandImpl endSessionCommand = new EndSessionCommand();
    
        return webSocketConnection.sendAsync(endSessionCommand);
    }    

    public void subscribe()
    {}
    public void unsubscribe()
    {}

    public static class CapabilitesRequest implements Type {
        // ToDo
    }
    public static class CapabilityRequest implements Type {
        // ToDo
    }
    public static class ProxyConfiguration implements Type {
        // ToDo
    }
    public static class UserPromptHandler implements Type {
        // ToDo
    }
    public static class UserPromptHandlerType implements Type {
        // ToDo
    }
    public static class Subscription implements Type {
        // ToDo
    }
    public static class SubscriptionRequest implements Type {
        // ToDo
    }
    public static class UnsubscribeByIdRequest implements Type {
        // ToDo
    }
    public static class UnsubscribeByAttributeRequest implements Type {
        // ToDo
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the BrowsingContext at the given index.
     * A BrowsingContext is a tab or window in the browser!
     *
     * @param index The index of the BrowsingContext.
     * @return The BrowsingContext at the given index.
     */
    // ToDo: Not very good practice?!?
    public BrowsingContext getBrowsingContext(int index) {
        return browsingContext.get(index);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class NewSessionCommand extends CommandImpl<NewSessionCommand.ParamsImpl> {

        public NewSessionCommand(String browserName) {
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

    public static class StatusCommand extends CommandImpl<StatusCommand.ParamsImpl> {

        public StatusCommand() {
            super("session.status", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter notwendig für diesen Command
        }
    }

    public static class GetTreeCommand extends CommandImpl<GetTreeCommand.ParamsImpl> {

        public GetTreeCommand() {
            super("browsingContext.getTree", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class EndSessionCommand extends CommandImpl<EndSessionCommand.ParamsImpl> {

        public EndSessionCommand() {
            super("session.delete", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }


}
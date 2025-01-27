package wd4j.impl.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.core.CommandImpl;
import wd4j.helper.BrowserType;
import wd4j.core.WebSocketConnection;
import wd4j.impl.generic.Command;
import wd4j.impl.generic.Event;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // ToDo: Gehört das hierher? Soll das nicht in BrowserContext sein wg. getTree?
    // Fallback-Methode: Kontext über getTree suchen
    private String fetchDefaultContextFromTree() {
        CommandImpl getTreeCommand = new BrowsingContext.GetTreeCommand();

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

    /**
     * Subscribes to specific WebDriver BiDi events.
     *
     * @param events A list of event names to subscribe to.
     * @throws RuntimeException if the subscription fails.
     */
    public void subscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new SubscribeCommand(events));
            System.out.println("Subscribed to events: " + events);
        } catch (RuntimeException e) {
            System.out.println("Error subscribing to events: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Unsubscribes from specific WebDriver BiDi events.
     *
     * @param events A list of event names to unsubscribe from.
     * @throws RuntimeException if the unsubscription fails.
     */
    public void unsubscribe(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list must not be null or empty.");
        }

        try {
            webSocketConnection.send(new UnsubscribeCommand(events));
            System.out.println("Unsubscribed from events: " + events);
        } catch (RuntimeException e) {
            System.out.println("Error unsubscribing from events: " + e.getMessage());
            throw e;
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class SessionCapabilitiesRequest {
        private final Map<String, Object> capabilities;

        public SessionCapabilitiesRequest(Map<String, Object> capabilities) {
            if (capabilities == null || capabilities.isEmpty()) {
                throw new IllegalArgumentException("Capabilities must not be null or empty.");
            }
            this.capabilities = capabilities;
        }

        public Map<String, Object> getCapabilities() {
            return capabilities;
        }
    }

    public class SessionCapabilityRequest {
        private final String name;

        public SessionCapabilityRequest(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name must not be null or empty.");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public class SessionProxyConfiguration {
        private final String proxyType;
        private final String proxy;

        public SessionProxyConfiguration(String proxyType, String proxy) {
            if (proxyType == null || proxyType.isEmpty()) {
                throw new IllegalArgumentException("Proxy type must not be null or empty.");
            }
            this.proxyType = proxyType;
            this.proxy = proxy;
        }

        public String getProxyType() {
            return proxyType;
        }

        public String getProxy() {
            return proxy;
        }
    }

    public class SessionUserPromptHandler {
        private final String contextId;
        private final String handlerType;

        public SessionUserPromptHandler(String contextId, String handlerType) {
            if (contextId == null || contextId.isEmpty()) {
                throw new IllegalArgumentException("Context ID must not be null or empty.");
            }
            if (handlerType == null || handlerType.isEmpty()) {
                throw new IllegalArgumentException("Handler type must not be null or empty.");
            }
            this.contextId = contextId;
            this.handlerType = handlerType;
        }

        public String getContextId() {
            return contextId;
        }

        public String getHandlerType() {
            return handlerType;
        }
    }

    public class SessionUserPromptHandlerType {
        private final String type;

        public SessionUserPromptHandlerType(String type) {
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("Type must not be null or empty.");
            }
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public class SessionSubscription {
        private final String eventName;

        public SessionSubscription(String eventName) {
            if (eventName == null || eventName.isEmpty()) {
                throw new IllegalArgumentException("Event name must not be null or empty.");
            }
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }
    }

    public class SessionSubscriptionRequest {
        private final List<SessionSubscription> subscriptions;

        public SessionSubscriptionRequest(List<SessionSubscription> subscriptions) {
            if (subscriptions == null || subscriptions.isEmpty()) {
                throw new IllegalArgumentException("Subscriptions must not be null or empty.");
            }
            this.subscriptions = subscriptions;
        }

        public List<SessionSubscription> getSubscriptions() {
            return subscriptions;
        }
    }

    public class SessionUnsubscribeByIDRequest {
        private final String subscriptionId;

        public SessionUnsubscribeByIDRequest(String subscriptionId) {
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                throw new IllegalArgumentException("Subscription ID must not be null or empty.");
            }
            this.subscriptionId = subscriptionId;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }
    }

    public class SessionUnsubscribeByAttributesRequest {
        private final String eventName;

        public SessionUnsubscribeByAttributesRequest(String eventName) {
            if (eventName == null || eventName.isEmpty()) {
                throw new IllegalArgumentException("Event name must not be null or empty.");
            }
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }
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

    public static class StatusCommand extends CommandImpl<StatusCommand.ParamsImpl> {

        public StatusCommand() {
            super("session.status", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter notwendig für diesen Command
        }
    }

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

    public static class EndSessionCommand extends CommandImpl<EndSessionCommand.ParamsImpl> {

        public EndSessionCommand() {
            super("session.delete", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class SubscribeCommand extends CommandImpl<SubscribeCommand.ParamsImpl> {

        public SubscribeCommand(List<String> events) {
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


    public static class UnsubscribeCommand extends CommandImpl<UnsubscribeCommand.ParamsImpl> {

        public UnsubscribeCommand(List<String> events) {
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
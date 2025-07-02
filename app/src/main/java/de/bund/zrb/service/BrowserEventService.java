package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Verwaltet das Registrieren und Deregistrieren von Browser-Events (Console, Request etc.).
 */
public class BrowserEventService {

    private static final Logger logger = LoggerFactory.getLogger(BrowserEventService.class);
    private static final BrowserEventService INSTANCE = new BrowserEventService();

    private final Map<String, EventHandler> eventHandlers = new HashMap<>();
    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private final Consumer<ConsoleMessage> consoleMessageHandler = msg -> log("Console: " + msg.text());
    private final Consumer<Response> responseHandler = res -> log("Response: " + res.url());
    private final Consumer<Page> loadHandler = page -> log("Page loaded: " + page.url());

    private BrowserEventService() {
        eventHandlers.put("Console Log", new EventHandler(() -> on().onConsoleMessage(consoleMessageHandler), () -> on().offConsoleMessage(consoleMessageHandler)));
        eventHandlers.put("Network Response", new EventHandler(() -> on().onResponse(responseHandler), () -> on().offResponse(responseHandler)));
        eventHandlers.put("Page Loaded", new EventHandler(() -> on().onLoad(loadHandler), () -> on().offLoad(loadHandler)));
    }

    public static BrowserEventService getInstance() {
        return INSTANCE;
    }

    public void register(String eventName) {
        EventHandler handler = eventHandlers.get(eventName);
        if (handler != null) handler.register();
    }

    public void deregister(String eventName) {
        EventHandler handler = eventHandlers.get(eventName);
        if (handler != null) handler.deregister();
    }

    public Map<String, EventHandler> getAllHandlers() {
        return eventHandlers;
    }

    private PageImpl on() {
        return browserService.getBrowser().getActivePage();
    }

    private void log(String message) {
        logger.info(message);
        // TODO: Optional: Weitergabe an DebugTab
    }

    public static class EventHandler {
        private final Runnable register;
        private final Runnable deregister;

        public EventHandler(Runnable register, Runnable deregister) {
            this.register = register;
            this.deregister = deregister;
        }

        public void register() {
            register.run();
        }

        public void deregister() {
            deregister.run();
        }
    }
}

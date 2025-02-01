//package wd4j.impl.events;
//
//import com.google.gson.JsonObject;
//import wd4j.impl.generic.Event;
//
//import java.util.function.Consumer;
//
//public class BrowsingContextEvents {
//
//    public static class ContextCreatedEvent implements Event {
//        private final String contextId;
//        private final String url;
//
//        public ContextCreatedEvent(JsonObject data) {
//            this.contextId = data.get("context").getAsString();
//            this.url = data.get("url").getAsString();
//        }
//
//        public String getContextId() {
//            return contextId;
//        }
//
//        public String getUrl() {
//            return url;
//        }
//    }
//
//    public static class ContextDestroyedEvent implements Event {
//        private final String contextId;
//
//        public ContextDestroyedEvent(JsonObject data) {
//            this.contextId = data.get("context").getAsString();
//        }
//
//        public String getContextId() {
//            return contextId;
//        }
//    }
//
//    public static class ContextLoadedEvent implements Event {
//        private final String contextId;
//        private final String url;
//
//        public ContextLoadedEvent(JsonObject data) {
//            this.contextId = data.get("context").getAsString();
//            this.url = data.get("url").getAsString();
//        }
//
//        public String getContextId() {
//            return contextId;
//        }
//
//        public String getUrl() {
//            return url;
//        }
//    }
//
//    public static class ContextFragmentNavigatedEvent implements Event {
//        private final String contextId;
//        private final String url;
//
//        public ContextFragmentNavigatedEvent(JsonObject data) {
//            this.contextId = data.get("context").getAsString();
//            this.url = data.get("url").getAsString();
//        }
//
//        public String getContextId() {
//            return contextId;
//        }
//
//        public String getUrl() {
//            return url;
//        }
//    }
//
//    /**
//     * Returns a Consumer<Event> that delegates to the appropriate typed event handler.
//     *
//     * @param onContextCreated  Handler for ContextCreatedEvent.
//     * @param onContextDestroyed Handler for ContextDestroyedEvent.
//     * @param onContextLoaded   Handler for ContextLoadedEvent.
//     * @param onFragmentNavigated Handler for ContextFragmentNavigatedEvent.
//     * @return A Consumer<Event> to handle the events.
//     */
//    public static Consumer<Event> createListener(
//            Consumer<ContextCreatedEvent> onContextCreated,
//            Consumer<ContextDestroyedEvent> onContextDestroyed,
//            Consumer<ContextLoadedEvent> onContextLoaded,
//            Consumer<ContextFragmentNavigatedEvent> onFragmentNavigated) {
//        return event -> {
//            if (event instanceof ContextCreatedEvent) {
//                onContextCreated.accept((ContextCreatedEvent) event);
//            } else if (event instanceof ContextDestroyedEvent) {
//                onContextDestroyed.accept((ContextDestroyedEvent) event);
//            } else if (event instanceof ContextLoadedEvent) {
//                onContextLoaded.accept((ContextLoadedEvent) event);
//            } else if (event instanceof ContextFragmentNavigatedEvent) {
//                onFragmentNavigated.accept((ContextFragmentNavigatedEvent) event);
//            }
//        };
//    }
//}

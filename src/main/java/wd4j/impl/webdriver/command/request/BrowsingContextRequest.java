package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.NavigateParameters;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.websocket.Command;

public class BrowsingContextRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Activate extends CommandImpl<Activate.ParamsImpl> implements CommandData {

        public Activate(String contextId) {
            super("browsingContext.activate", new ParamsImpl(contextId));
        }

        public static class ParamsImpl implements Command.Params{
            private final String context;

            public ParamsImpl(String contextId) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
            }
        }
    }


    public static class CaptureScreenshot extends CommandImpl<CaptureScreenshot.ParamsImpl> implements CommandData {

        public CaptureScreenshot(String contextId) {
            super("browsingContext.captureScreenshot", new ParamsImpl(contextId));
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


    public static class Close extends CommandImpl<Close.ParamsImpl> implements CommandData {

        public Close(String contextId) {
            super("browsingContext.close", new ParamsImpl(contextId));
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


    public static class Create extends CommandImpl<Create.ParamsImpl> implements CommandData {

        public Create(String type) {
            super("browsingContext.create", new ParamsImpl(type));
        }

        public static class ParamsImpl implements Command.Params {
            private final String type;

            public ParamsImpl(String type) {
                if (type == null || type.isEmpty()) {
                    throw new IllegalArgumentException("Type must not be null or empty.");
                }
                this.type = type;
            }
        }
    }

    public static class GetTree extends CommandImpl<GetTree.ParamsImpl> implements CommandData {

        public GetTree() {
            super("browsingContext.getTree", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class HandleUserPrompt extends CommandImpl<HandleUserPrompt.ParamsImpl> implements CommandData {

        public HandleUserPrompt(String contextId, String userText) {
            super("browsingContext.handleUserPrompt", new ParamsImpl(contextId, userText));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String userText;

            public ParamsImpl(String contextId, String userText) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
                this.userText = userText; // `userText` kann null sein, falls der Nutzer keinen Text eingibt.
            }
        }
    }

    public static class LocateNodes extends CommandImpl<LocateNodes.ParamsImpl> implements CommandData {

        public LocateNodes(String contextId, String selector) {
            super("browsingContext.locateNodes", new ParamsImpl(contextId, selector));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final String selector;

            public ParamsImpl(String contextId, String selector) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (selector == null || selector.isEmpty()) {
                    throw new IllegalArgumentException("Selector must not be null or empty.");
                }
                this.context = contextId;
                this.selector = selector;
            }
        }
    }


    public static class Navigate extends CommandImpl<NavigateParameters> implements CommandData {
        // ToDo: What to do with the `ReadinessState` parameter? -> currently set to null
        public Navigate(String url, String contextId) {
            super("browsingContext.navigate", new NavigateParameters(new BrowsingContext(contextId), url, null));
        }
    }

    public static class Print extends CommandImpl<Print.ParamsImpl> implements CommandData {

        public Print(String contextId) {
            super("browsingContext.print", new ParamsImpl(contextId));
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


    public static class Reload extends CommandImpl<Reload.ParamsImpl> implements CommandData {

        public Reload(String contextId) {
            super("browsingContext.reload", new ParamsImpl(contextId));
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


    public static class SetViewport extends CommandImpl<SetViewport.ParamsImpl> implements CommandData {

        public SetViewport(String contextId, int width, int height) {
            super("browsingContext.setViewport", new ParamsImpl(contextId, width, height));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final int width;
            private final int height;

            public ParamsImpl(String contextId, int width, int height) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                if (width <= 0) {
                    throw new IllegalArgumentException("Width must be greater than 0.");
                }
                if (height <= 0) {
                    throw new IllegalArgumentException("Height must be greater than 0.");
                }
                this.context = contextId;
                this.width = width;
                this.height = height;
            }
        }
    }


    public static class TraverseHistory extends CommandImpl<TraverseHistory.ParamsImpl> implements CommandData {

        public TraverseHistory(String contextId, int delta) {
            super("browsingContext.traverseHistory", new ParamsImpl(contextId, delta));
        }

        public static class ParamsImpl implements Command.Params {
            private final String context;
            private final int delta;

            public ParamsImpl(String contextId, int delta) {
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.context = contextId;
                this.delta = delta;
            }
        }
    }

}
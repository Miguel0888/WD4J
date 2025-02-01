package wd4j.impl.module.command;

import wd4j.core.CommandImpl;
import wd4j.impl.generic.Command;

public class BrowsingContext {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Activate extends CommandImpl<Activate.ParamsImpl> {

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


    public static class CaptureScreenshot extends CommandImpl<CaptureScreenshot.ParamsImpl> {

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


    public static class Close extends CommandImpl<Close.ParamsImpl> {

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


    public static class Create extends CommandImpl<Create.ParamsImpl> {

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

    public static class GetTree extends CommandImpl<GetTree.ParamsImpl> {

        public GetTree() {
            super("browsingContext.getTree", new ParamsImpl());
        }

        public static class ParamsImpl implements Command.Params {
            // Keine Parameter erforderlich, daher bleibt die Klasse leer.
        }
    }

    public static class HandleUserPrompt extends CommandImpl<HandleUserPrompt.ParamsImpl> {

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

    public static class LocateNodes extends CommandImpl<LocateNodes.ParamsImpl> {

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


    public static class Navigate extends CommandImpl<Navigate.ParamsImpl> {

        public Navigate(String url, String contextId) {
            super("browsingContext.navigate", new ParamsImpl(url, contextId));
        }

        public static class ParamsImpl implements Command.Params {
            private final String url;
            private final String context;

            public ParamsImpl(String url, String contextId) {
                if (url == null || url.isEmpty()) {
                    throw new IllegalArgumentException("URL must not be null or empty.");
                }
                if (contextId == null || contextId.isEmpty()) {
                    throw new IllegalArgumentException("Context ID must not be null or empty.");
                }
                this.url = url;
                this.context = contextId;
            }
        }
    }

    public static class Print extends CommandImpl<Print.ParamsImpl> {

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


    public static class Reload extends CommandImpl<Reload.ParamsImpl> {

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


    public static class SetViewport extends CommandImpl<SetViewport.ParamsImpl> {

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


    public static class TraverseHistory extends CommandImpl<TraverseHistory.ParamsImpl> {

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
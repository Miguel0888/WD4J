package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class SetViewportParameters implements Command.Params {
    private final BrowsingContext context;
    private final Viewport viewport; // optional
    private final Float devicePixelRatio; // optional

    public SetViewportParameters(BrowsingContext context) {
        this(context, null, null);
    }

    public SetViewportParameters(BrowsingContext context, Viewport viewport, Float devicePixelRatio) {
        this.context = context;
        this.viewport = viewport;
        this.devicePixelRatio = devicePixelRatio;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Float getDevicePixelRatio() {
        return devicePixelRatio;
    }

    public static class Viewport {
        private final char width;
        private final char height;

        public Viewport(char width, char height) {
            this.width = width;
            this.height = height;
        }

        public char getWidth() {
            return width;
        }

        public char getHeight() {
            return height;
        }
    }
}

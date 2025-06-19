package de.bund.zrb.command.request.parameters.browsingContext;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.api.WDCommand;

public class SetViewportParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final Viewport viewport; // optional
    private final Float devicePixelRatio; // optional

    public SetViewportParameters(WDBrowsingContext context) {
        this(context, null, null);
    }

    public SetViewportParameters(WDBrowsingContext context, Viewport viewport, Float devicePixelRatio) {
        this.context = context;
        this.viewport = viewport;
        this.devicePixelRatio = devicePixelRatio;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Float getDevicePixelRatio() {
        return devicePixelRatio;
    }

    public static class Viewport {
        private final int width;
        private final int height;

        public Viewport(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}

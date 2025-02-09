package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class SetViewportParameters implements Command.Params {
    private final BrowsingContext context;
    private final Viewport viewport;
    private final float devicePixelRatio;

    public SetViewportParameters(BrowsingContext context, Viewport viewport, float devicePixelRatio) {
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

    public float getDevicePixelRatio() {
        return devicePixelRatio;
    }

}

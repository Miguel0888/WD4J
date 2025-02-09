package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class CaptureScreenshotParameters implements Command.Params {
    private final BrowsingContext context;
    private final Origin origin;
    private final ImageFormat format;
    private final ClipRectangle clip;

    public CaptureScreenshotParameters(BrowsingContext context, Origin origin, ImageFormat format, ClipRectangle clip) {
        this.context = context;
        this.origin = origin;
        this.format = format;
        this.clip = clip;
    }

    public BrowsingContext getContext() {
        return context;
    }

    public Origin getOrigin() {
        return origin;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public ClipRectangle getClip() {
        return clip;
    }

    public enum Origin implements EnumWrapper {
        VIEWPORT("viewport"),
        DOCUMENT("document");

        private final String value;

        Origin(String value) {
            this.value = value;
        }

        @Override // confirmed
        public String value() {
            return value;
        }
    }
}

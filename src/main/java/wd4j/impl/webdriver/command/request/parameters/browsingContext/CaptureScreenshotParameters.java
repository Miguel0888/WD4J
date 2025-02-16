package wd4j.impl.webdriver.command.request.parameters.browsingContext;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDRemoteReference;
import wd4j.impl.websocket.WDCommand;

public class CaptureScreenshotParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final Origin origin; // optional
    private final ImageFormat format; // optional
    private final ClipRectangle clip; // optional

    public CaptureScreenshotParameters(WDBrowsingContext context) {
        this(context, null, null, null);
    }

    public CaptureScreenshotParameters(WDBrowsingContext context, Origin origin, ImageFormat format, ClipRectangle clip) {
        this.context = context;
        this.origin = origin;
        this.format = format;
        this.clip = clip;
    }

    public WDBrowsingContext getContext() {
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

    public interface ClipRectangle {

        class ElementClipRectangle implements ClipRectangle {
            private final String type = "element";
            private final WDRemoteReference.SharedReferenceWD element;

            public ElementClipRectangle(WDRemoteReference.SharedReferenceWD element) {
                this.element = element;
            }

            public String getType() {
                return type;
            }

            public WDRemoteReference.SharedReferenceWD getElement() {
                return element;
            }
        }

        class BoxClipRectangle implements ClipRectangle {
            private final String type = "box";
            private final int x;
            private final int y;
            private final int width;
            private final int height;

            public BoxClipRectangle(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            public String getType() {
                return type;
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }

            public int getWidth() {
                return width;
            }

            public int getHeight() {
                return height;
            }
        }
    }

    public static class ImageFormat {
        public final String type;
        private final float quality;

        public ImageFormat(String type, float quality) {
            this.type = type;
            this.quality = quality;
        }

        public String getType() {
            return type;
        }

        public float getQuality() {
            return quality;
        }
    }
}

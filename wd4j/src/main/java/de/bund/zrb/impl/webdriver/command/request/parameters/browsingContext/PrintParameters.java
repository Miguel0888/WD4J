package de.bund.zrb.impl.webdriver.command.request.parameters.browsingContext;

import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.websocket.WDCommand;

public class PrintParameters implements WDCommand.Params {
    private final WDBrowsingContext context;
    private final boolean background; // optional, default false
    private final PrintMarginParameters margin; // optional
    private final Orientation orientation; // optional, default PORTRAIT
    private final PrintPageParameters page; // optional
    private final long pageRanges; // optional
    private final float scale; // optional, default 1.0
    private final boolean shrinkToFit; // optional, default true

    public PrintParameters(WDBrowsingContext context) {
        this.context = context;
        this.background = false;
        this.margin = null;
        this.orientation = Orientation.PORTRAIT;
        this.page = null;
        this.pageRanges = 0;
        this.scale = 1.0f;
        this.shrinkToFit = true;
    }

    public PrintParameters(WDBrowsingContext context, boolean background, PrintMarginParameters margin, Orientation orientation, PrintPageParameters page, long pageRanges, float scale, boolean shrinkToFit) {
        this.context = context;
        this.background = background;
        this.margin = margin;
        this.orientation = orientation;
        this.page = page;
        this.pageRanges = pageRanges;
        this.scale = scale;
        this.shrinkToFit = shrinkToFit;
    }

    public WDBrowsingContext getContext() {
        return context;
    }

    public boolean getBackground() {
        return background;
    }

    public PrintMarginParameters getMargin() {
        return margin;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public PrintPageParameters getPage() {
        return page;
    }

    public long getPageRanges() {
        return pageRanges;
    }

    public float getScale() {
        return scale;
    }

    public boolean getShrinkToFit() {
        return shrinkToFit;
    }

    /**
     * Minimum size is 1pt x 1pt. Conversion follows from https://www.w3.org/TR/css3-values/#absolute-lengths
     */
    public static class PrintPageParameters {
        private final float height; // optional, default 27.94
        private final float width; // optional, .default 21.59

        public PrintPageParameters() {
            this.height = 27.94f;
            this.width = 21.59f;
        }

        public PrintPageParameters(float height, float width) {
            this.height = height;
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public float getWidth() {
            return width;
        }
    }

    public static class PrintMarginParameters {
        private final float bottom; // optional, default 0
        private final float left; // optional, default 0
        private final float right; // optional, default 0
        private final float top; // optional, default 0

        public PrintMarginParameters() {
            this.bottom = 0;
            this.left = 0;
            this.right = 0;
            this.top = 0;
        }

        public PrintMarginParameters(float bottom, float left, float right, float top) {
            this.bottom = bottom;
            this.left = left;
            this.right = right;
            this.top = top;
        }

        public float getBottom() {
            return bottom;
        }

        public float getLeft() {
            return left;
        }

        public float getRight() {
            return right;
        }

        public float getTop() {
            return top;
        }
    }
}

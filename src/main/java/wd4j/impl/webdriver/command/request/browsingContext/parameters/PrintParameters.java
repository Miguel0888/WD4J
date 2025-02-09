package wd4j.impl.webdriver.command.request.browsingContext.parameters;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.websocket.Command;

public class PrintParameters implements Command.Params {
    private final BrowsingContext context;
    private final boolean background;
    private final PrintMarginParameters margin;
    private final Orientation orientation;
    private final PrintPageParameters page;
    private final char pageRanges;
    private final float scale;
    private final boolean shrinkToFit;

    public PrintParameters(BrowsingContext context, boolean background, PrintMarginParameters margin, Orientation orientation, PrintPageParameters page, char pageRanges, float scale, boolean shrinkToFit) {
        this.context = context;
        this.background = background;
        this.margin = margin;
        this.orientation = orientation;
        this.page = page;
        this.pageRanges = pageRanges;
        this.scale = scale;
        this.shrinkToFit = shrinkToFit;
    }

    public BrowsingContext getContext() {
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

    public char getPageRanges() {
        return pageRanges;
    }

    public float getScale() {
        return scale;
    }

    public boolean getShrinkToFit() {
        return shrinkToFit;
    }
}

package de.bund.zrb.type.input;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.WDRemoteReference;

/**
 * Represent the input.FileDialogInfo struct.
 */
public class WDFileDialogInfo {
    private WDBrowsingContext context;
    private WDRemoteReference.SharedReference element; // optional
    private boolean multiple;

    public WDFileDialogInfo() {
        // Gson no-args
    }

    public WDFileDialogInfo(WDBrowsingContext context, WDRemoteReference.SharedReference element, boolean multiple) {
        this.context = context;
        this.element = element;
        this.multiple = multiple;
    }

    public WDBrowsingContext getContext() { return context; }
    public WDRemoteReference.SharedReference getElement() { return element; }
    public boolean isMultiple() { return multiple; }
}

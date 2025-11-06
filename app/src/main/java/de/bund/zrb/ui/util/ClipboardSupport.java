package de.bund.zrb.ui.util;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

/** Copy text into system clipboard. */
public final class ClipboardSupport {
    private ClipboardSupport() { }

    /** Put plain text into clipboard. */
    public static void putString(String text) {
        if (text == null) text = "";
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }
}

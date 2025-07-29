package de.bund.zrb.ui.components.log;

import javax.swing.text.html.HTMLEditorKit;

public class HTMLKitWithPublicParser extends HTMLEditorKit {
    public Parser getPublicParser() {
        return super.getParser();
    }
}

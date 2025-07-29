package de.bund.zrb.ui.components.log;

import java.util.Collections;
import java.util.List;

public class StepLog implements LogComponent {

    private final String phase;
    private final String content;

    public StepLog(String phase, String content) {
        this.phase = phase;
        this.content = content;
    }

    @Override
    public String toHtml() {
        return "<p><b>" + phase + ":</b> " + escape(content) + "</p>";
    }

    @Override
    public String getName() {
        return phase;
    }

    @Override
    public List<LogComponent> getChildren() {
        return Collections.emptyList();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public String getPhase() {
        return phase;
    }

    public String getContent() {
        return content;
    }
}

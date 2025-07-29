package de.bund.zrb.ui.components.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepLog implements LogComponent {

    private final String phase;
    private final String content;

    private LogComponent parent;
    private List<LogComponent> children = new ArrayList<>();

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
    public LogComponent getParent() {
        return parent;
    }

    @Override
    public List<LogComponent> getChildren() {
        return children;
    }

    @Override
    public void setParent(LogComponent parent) {
        this.parent = parent;
    }

    @Override
    public void setChildren(List<LogComponent> children) {
        this.children = children;
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

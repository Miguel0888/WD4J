package de.bund.zrb.ui.components.log;

import de.bund.zrb.ui.components.log.LogComponent;

import java.util.ArrayList;
import java.util.List;

public class SuiteLog implements LogComponent {

    private final String name;
    private final List<LogComponent> children = new ArrayList<>();

    public SuiteLog(String name) {
        this.name = name;
    }

    public void addChild(LogComponent component) {
        children.add(component);
    }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder("<h1>" + escape(name) + "</h1>");
        for (LogComponent child : children) {
            sb.append(child.toHtml());
        }
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

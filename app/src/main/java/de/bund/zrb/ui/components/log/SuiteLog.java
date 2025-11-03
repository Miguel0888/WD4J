package de.bund.zrb.ui.components.log;

import java.util.ArrayList;
import java.util.List;

public class SuiteLog implements LogComponent {

    private final String name;
    private LogComponent parent = null;
    private List<LogComponent> children = new ArrayList<LogComponent>();

    // Explicit status for this suite (null = unknown/not set)
    private Boolean success = null;

    // Optional error message for this suite (plain text; will be escaped in HTML)
    private String errorMessage = null;

    public SuiteLog(String name) {
        this.name = name;
    }

    public void addChild(LogComponent component) {
        children.add(component);
    }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder();

        // Build status badge (only if status was explicitly set)
        String badge = "";
        if (success != null) {
            // Comment: Render a green check for success, red cross for failure
            String symbol = success.booleanValue() ? "&#10003;" : "&#10007;";
            String color  = success.booleanValue() ? "#2e7d32" : "#c62828";
            badge = " <span style='font-size:.9rem;color:" + color + ";vertical-align:middle'>" + symbol + "</span>";
        }

        sb.append("<h1>").append(escape(name)).append(badge).append("</h1>");

        // Render error block when present
        if (errorMessage != null && errorMessage.length() > 0) {
            // Comment: Show an inline error panel with escaped message
            sb.append("<div style='border:1px solid #c62828;background:#ffebee;color:#b71c1c;padding:.5rem 0.75rem;margin:.5rem 0;border-radius:6px'>")
              .append("<strong>Fehler:</strong> ")
              .append(escape(errorMessage))
              .append("</div>");
        }

        // Render children
        for (LogComponent child : children) {
            sb.append(child.toHtml());
        }

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public String getName() {
        return name;
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
        this.children = (children != null) ? children : new ArrayList<LogComponent>();
    }

    public void setStatus(boolean success) {
        // Comment: Store explicit suite status for rendering
        this.success = Boolean.valueOf(success);
    }

    public void setError(String message) {
        // Comment: Store a plain-text error; escape on rendering
        this.errorMessage = (message != null) ? message : "";
        // Comment: When an error is set, mark the suite as failed if not already set
        if (this.success == null || this.success.booleanValue()) {
            this.success = Boolean.FALSE;
        }
    }
}

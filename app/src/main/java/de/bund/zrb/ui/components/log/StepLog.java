package de.bund.zrb.ui.components.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepLog implements LogComponent {

    private final String phase;
    private final String content;
    private boolean success = true;
    private String errorMessage;

    private LogComponent parent;
    private List<LogComponent> children = new ArrayList<>();

    private String htmlAppend; // NEU

    public StepLog(String phase, String content) {
        this.phase = phase;
        this.content = content;
    }

    public void setHtmlAppend(String htmlAppend) { this.htmlAppend = htmlAppend; }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        String statusSymbol = success ? "✔" : "❌";
        sb.append("<p><b>").append(statusSymbol).append(" ").append(phase).append(":</b> ")
                .append(escape(content));
        if (!success && errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("<br><i style='color:red'>Fehler: ").append(escape(errorMessage)).append("</i>");
        }
        if (htmlAppend != null && !htmlAppend.isEmpty()) {
            sb.append("<br>").append(htmlAppend); // rohes HTML
        }
        sb.append("</p>");
        return sb.toString();
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

    public String getPhase() {
        return phase;
    }

    public String getContent() {
        return content;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setStatus(boolean success) {
        this.success = success;
    }

    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

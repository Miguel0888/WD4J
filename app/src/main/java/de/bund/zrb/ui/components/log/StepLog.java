package de.bund.zrb.ui.components.log;

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

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

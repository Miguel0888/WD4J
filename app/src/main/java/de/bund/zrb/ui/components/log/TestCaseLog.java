package de.bund.zrb.ui.components.log;

import java.util.ArrayList;
import java.util.List;

public class TestCaseLog implements LogComponent {

    private final String name;
    private final List<LogComponent> steps = new ArrayList<>();

    public TestCaseLog(String name) {
        this.name = name;
    }

    public void addStep(LogComponent step) {
        steps.add(step);
    }

    @Override
    public String toHtml() {
        StringBuilder sb = new StringBuilder("<h2>" + escape(name) + "</h2>");
        for (LogComponent step : steps) {
            sb.append(step.toHtml());
        }
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<LogComponent> getChildren() {
        return steps;
    }
}

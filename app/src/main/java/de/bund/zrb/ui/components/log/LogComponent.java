package de.bund.zrb.ui.components.log;

import java.util.List;

public interface LogComponent {
    String toHtml();

    String getName();

    LogComponent getParent();

    List<LogComponent> getChildren();

    void setParent(LogComponent parent);

    void setChildren(List<LogComponent> children);
}

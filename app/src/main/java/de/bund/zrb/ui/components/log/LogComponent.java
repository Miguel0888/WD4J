package de.bund.zrb.ui.components.log;

import java.util.List;

public interface LogComponent {
    String toHtml();

    String getName();

    List<LogComponent> getChildren();
}

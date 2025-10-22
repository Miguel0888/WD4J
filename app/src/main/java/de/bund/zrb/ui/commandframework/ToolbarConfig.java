package de.bund.zrb.ui.commandframework;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class ToolbarConfig {
    public int buttonSizePx;
    public float fontSizeRatio;
    public List<ToolbarButtonConfig> buttons;
    public LinkedHashSet<String> rightSideIds;
    public LinkedHashMap<String, String> groupColors;
}

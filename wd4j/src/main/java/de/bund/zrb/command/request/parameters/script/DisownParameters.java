package de.bund.zrb.command.request.parameters.script;

import de.bund.zrb.type.script.WDHandle;
import de.bund.zrb.type.script.WDTarget;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class DisownParameters implements WDCommand.Params {
    private final List<WDHandle> WDHandles;
    private final WDTarget WDTarget;

    public DisownParameters(List<WDHandle> WDHandles, WDTarget WDTarget) {
        this.WDHandles = WDHandles;
        this.WDTarget = WDTarget;
    }

    public List<WDHandle> getHandles() {
        return WDHandles;
    }

    public WDTarget getTarget() {
        return WDTarget;
    }
}

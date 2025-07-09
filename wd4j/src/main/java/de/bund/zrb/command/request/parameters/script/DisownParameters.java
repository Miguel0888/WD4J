package de.bund.zrb.command.request.parameters.script;

import de.bund.zrb.type.script.WDHandle;
import de.bund.zrb.type.script.WDTarget;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class DisownParameters implements WDCommand.Params {
    private final List<WDHandle> handles;
    private final WDTarget target;

    public DisownParameters(List<WDHandle> handles, WDTarget target) {
        this.handles = handles;
        this.target = target;
    }

    public List<WDHandle> getHandles() {
        return handles;
    }

    public WDTarget getTarget() {
        return target;
    }
}

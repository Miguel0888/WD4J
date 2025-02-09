package wd4j.impl.webdriver.command.request.script.parameters;

import wd4j.impl.webdriver.type.script.Handle;
import wd4j.impl.webdriver.type.script.Target;
import wd4j.impl.websocket.Command;

import java.util.List;

public class DisownParameters implements Command.Params {
    private final List<Handle> handles;
    private final Target target;

    public DisownParameters(List<Handle> handles, Target target) {
        this.handles = handles;
        this.target = target;
    }

    public List<Handle> getHandles() {
        return handles;
    }

    public Target getTarget() {
        return target;
    }
}

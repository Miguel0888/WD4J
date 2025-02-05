package wd4j.impl.webdriver.type.input.parameters;

import wd4j.impl.webdriver.command.request.BrowsingContext;
import wd4j.impl.webdriver.type.script.remoteReference.SharedReference;
import wd4j.impl.websocket.Command;

import java.util.List;

public class SetFileParameters implements Command.Params {
    public final BrowsingContext browsingContext;
    public final SharedReference sharedReference;
    List<String> files;

    public SetFileParameters(BrowsingContext browsingContext, SharedReference sharedReference, List<String> files) {
        this.browsingContext = browsingContext;
        this.sharedReference = sharedReference;
        this.files = files;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public SharedReference getSharedReference() {
        return sharedReference;
    }

    public List<String> getFiles() {
        return files;
    }
}

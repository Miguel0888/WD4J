package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.script.RemoteReference;
import wd4j.impl.websocket.Command;

import java.util.List;

public class SetFilesParameters implements Command.Params {
    public final BrowsingContext browsingContext;
    public final RemoteReference.SharedReference sharedReference;
    List<String> files;

    public SetFilesParameters(BrowsingContext browsingContext, RemoteReference.SharedReference sharedReference, List<String> files) {
        this.browsingContext = browsingContext;
        this.sharedReference = sharedReference;
        this.files = files;
    }

    public BrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public RemoteReference.SharedReference getSharedReference() {
        return sharedReference;
    }

    public List<String> getFiles() {
        return files;
    }
}

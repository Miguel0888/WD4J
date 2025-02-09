package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.command.request.BrowsingContextRequest;
import wd4j.impl.webdriver.type.script.RemoteReference;
import wd4j.impl.websocket.Command;

import java.util.List;

public class SetFileParameters implements Command.Params {
    public final BrowsingContextRequest browsingContextRequest;
    public final RemoteReference.SharedReference sharedReference;
    List<String> files;

    public SetFileParameters(BrowsingContextRequest browsingContextRequest, RemoteReference.SharedReference sharedReference, List<String> files) {
        this.browsingContextRequest = browsingContextRequest;
        this.sharedReference = sharedReference;
        this.files = files;
    }

    public BrowsingContextRequest getBrowsingContext() {
        return browsingContextRequest;
    }

    public RemoteReference.SharedReference getSharedReference() {
        return sharedReference;
    }

    public List<String> getFiles() {
        return files;
    }
}

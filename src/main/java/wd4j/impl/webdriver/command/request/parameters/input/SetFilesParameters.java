package wd4j.impl.webdriver.command.request.parameters.input;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDRemoteReference;
import wd4j.impl.websocket.WDCommand;

import java.util.List;

public class SetFilesParameters implements WDCommand.Params {
    public final WDBrowsingContext WDBrowsingContext;
    public final WDRemoteReference.SharedReferenceWD sharedReference;
    List<String> files;

    public SetFilesParameters(WDBrowsingContext WDBrowsingContext, WDRemoteReference.SharedReferenceWD sharedReference, List<String> files) {
        this.WDBrowsingContext = WDBrowsingContext;
        this.sharedReference = sharedReference;
        this.files = files;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }

    public WDRemoteReference.SharedReferenceWD getSharedReference() {
        return sharedReference;
    }

    public List<String> getFiles() {
        return files;
    }
}

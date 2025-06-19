package de.bund.zrb.command.request.parameters.input;

import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.WDRemoteReference;
import de.bund.zrb.api.WDCommand;

import java.util.List;

public class SetFilesParameters implements WDCommand.Params {
    public final WDBrowsingContext WDBrowsingContext;
    public final WDRemoteReference.SharedReference sharedReference;
    List<String> files;

    public SetFilesParameters(WDBrowsingContext WDBrowsingContext, WDRemoteReference.SharedReference sharedReference, List<String> files) {
        this.WDBrowsingContext = WDBrowsingContext;
        this.sharedReference = sharedReference;
        this.files = files;
    }

    public WDBrowsingContext getBrowsingContext() {
        return WDBrowsingContext;
    }

    public WDRemoteReference.SharedReference getSharedReference() {
        return sharedReference;
    }

    public List<String> getFiles() {
        return files;
    }
}

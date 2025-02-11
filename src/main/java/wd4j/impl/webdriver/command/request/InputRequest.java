package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;
import wd4j.impl.webdriver.command.request.parameters.input.ReleaseActionsParameters;
import wd4j.impl.webdriver.command.request.parameters.input.SetFilesParameters;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.script.RemoteReference;

import java.util.List;

public class InputRequest {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends CommandImpl<PerformActionsParameters> implements CommandData {
        public PerformActions(String contextId, List<PerformActionsParameters.SourceActions> actions) {
            super("input.performActions", new PerformActionsParameters(new BrowsingContext(contextId), actions));
        }
        public PerformActions(BrowsingContext context, List<PerformActionsParameters.SourceActions> actions) {
            super("input.performActions", new PerformActionsParameters(context, actions));
        }
    }

    public static class ReleaseActions extends CommandImpl<ReleaseActionsParameters> implements CommandData {
        public ReleaseActions(String contextId) {
            super("input.releaseActions", new ReleaseActionsParameters(new BrowsingContext(contextId)));
        }
        public ReleaseActions(BrowsingContext context) {
            super("input.releaseActions", new ReleaseActionsParameters(context));
        }
    }

    public static class SetFiles extends CommandImpl<SetFilesParameters> implements CommandData {
        public SetFiles(String contextId, RemoteReference.SharedReference sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(new BrowsingContext(contextId), sharedReference, files));
        }
        public SetFiles(BrowsingContext context, RemoteReference.SharedReference sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(context, sharedReference, files));
        }
    }

}
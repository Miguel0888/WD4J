package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.webdriver.command.request.helper.WDCommandImpl;
import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;
import wd4j.impl.webdriver.command.request.parameters.input.ReleaseActionsParameters;
import wd4j.impl.webdriver.command.request.parameters.input.SetFilesParameters;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDRemoteReference;

import java.util.List;

public class InputRequest {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends WDCommandImpl<PerformActionsParameters> implements WDCommandData {
        public PerformActions(String contextId, List<PerformActionsParameters.SourceActions> actions) {
            super("input.performActions", new PerformActionsParameters(new WDBrowsingContext(contextId), actions));
        }
        public PerformActions(WDBrowsingContext context, List<PerformActionsParameters.SourceActions> actions) {
            super("input.performActions", new PerformActionsParameters(context, actions));
        }
    }

    public static class ReleaseActions extends WDCommandImpl<ReleaseActionsParameters> implements WDCommandData {
        public ReleaseActions(String contextId) {
            super("input.releaseActions", new ReleaseActionsParameters(new WDBrowsingContext(contextId)));
        }
        public ReleaseActions(WDBrowsingContext context) {
            super("input.releaseActions", new ReleaseActionsParameters(context));
        }
    }

    public static class SetFiles extends WDCommandImpl<SetFilesParameters> implements WDCommandData {
        public SetFiles(String contextId, WDRemoteReference.SharedReferenceWD sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(new WDBrowsingContext(contextId), sharedReference, files));
        }
        public SetFiles(WDBrowsingContext context, WDRemoteReference.SharedReferenceWD sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(context, sharedReference, files));
        }
    }

}
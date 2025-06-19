package de.bund.zrb.impl.webdriver.command.request;

import de.bund.zrb.impl.markerInterfaces.WDCommandData;
import de.bund.zrb.impl.webdriver.command.request.helper.WDCommandImpl;
import de.bund.zrb.impl.webdriver.command.request.parameters.input.PerformActionsParameters;
import de.bund.zrb.impl.webdriver.command.request.parameters.input.ReleaseActionsParameters;
import de.bund.zrb.impl.webdriver.command.request.parameters.input.SetFilesParameters;
import de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions.SourceActions;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.webdriver.type.script.WDRemoteReference;

import java.util.List;

public class WDInputRequest {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class PerformActions extends WDCommandImpl<PerformActionsParameters> implements WDCommandData {
        public PerformActions(String contextId, List<SourceActions> actions) {
            super("input.performActions", new PerformActionsParameters(new WDBrowsingContext(contextId), actions));
        }
        public PerformActions(WDBrowsingContext context, List<SourceActions> actions) {
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
        public SetFiles(String contextId, WDRemoteReference.SharedReference sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(new WDBrowsingContext(contextId), sharedReference, files));
        }
        public SetFiles(WDBrowsingContext context, WDRemoteReference.SharedReference sharedReference, List<String> files) {
            super("input.setFiles", new SetFilesParameters(context, sharedReference, files));
        }
    }

}
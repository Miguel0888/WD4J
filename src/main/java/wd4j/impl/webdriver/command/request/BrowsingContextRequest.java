package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.*;
import wd4j.impl.webdriver.type.browser.UserContext;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.Locator;
import wd4j.impl.webdriver.type.browsingContext.ReadinessState;
import wd4j.impl.webdriver.type.script.RemoteReference;
import wd4j.impl.webdriver.type.script.SerializationOptions;
import wd4j.impl.websocket.Command;

import java.util.List;

public class BrowsingContextRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Activate extends CommandImpl<ActivateParameters> implements CommandData {
        public Activate(String contextId) {
            super("browsingContext.activate", new ActivateParameters(new BrowsingContext(contextId)));
        }
    }


    public static class CaptureScreenshot extends CommandImpl<CaptureScreenshotParameters> implements CommandData {
        public CaptureScreenshot(String contextId) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(new BrowsingContext(contextId)));
        }
        public CaptureScreenshot(String contextId, CaptureScreenshotParameters.Origin origin, CaptureScreenshotParameters.ImageFormat format, CaptureScreenshotParameters.ClipRectangle clip) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(new BrowsingContext(contextId), origin, format, clip));
        }
    }


    public static class Close extends CommandImpl<CloseParameters> implements CommandData {
        public Close(String contextId) {
            super("browsingContext.close", new CloseParameters(new BrowsingContext(contextId)));
        }

        public Close(String contextId, boolean promptUnload) {
            super("browsingContext.close", new CloseParameters(new BrowsingContext(contextId), promptUnload));
        }
    }


    public static class Create extends CommandImpl<CreateParameters> implements CommandData {
        public Create(CreateType type) {
            super("browsingContext.create", new CreateParameters(type));
        }

        public Create(CreateType type, BrowsingContext referenceContext, Boolean background, UserContext userContext) {
            super("browsingContext.create", new CreateParameters(type, referenceContext, background, userContext));
        }
    }

    public static class GetTree extends CommandImpl<GetTreeParameters> implements CommandData {
        public GetTree() {
            super("browsingContext.getTree", new GetTreeParameters());
        }

        public GetTree(Character maxDepth, BrowsingContext root) {
            super("browsingContext.getTree", new GetTreeParameters(maxDepth, root));
        }
    }


    public static class HandleUserPrompt extends CommandImpl<HandleUserPromptParameters> implements CommandData {
        public HandleUserPrompt(String contextId) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new BrowsingContext(contextId)));
        }

        public HandleUserPrompt(String contextId, Boolean accept) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new BrowsingContext(contextId), accept));
        }

        public HandleUserPrompt(String contextId, Boolean accept, String userText) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new BrowsingContext(contextId), accept, userText));
        }
    }

    public static class LocateNodes extends CommandImpl<LocateNodesParameters> implements CommandData {
        public LocateNodes(String contextId, Locator locator) {
            super("browsingContext.locateNodes", new LocateNodesParameters(new BrowsingContext(contextId), locator));
        }

        // BrowsingContext context, Locator locator, Character maxNodeCount, SerializationOptions serializationOptions, List<RemoteReference.SharedReference> startNodes
        public LocateNodes(String contextId, Locator locator, Character maxNodeCount, SerializationOptions serializationOptions, List<RemoteReference.SharedReference> startNodes) {
            super("browsingContext.locateNodes", new LocateNodesParameters(new BrowsingContext(contextId), locator, maxNodeCount, serializationOptions, startNodes));
        }
    }


    public static class Navigate extends CommandImpl<NavigateParameters> implements CommandData {

        public Navigate(String url, String contextId) {
            super("browsingContext.navigate", new NavigateParameters(new BrowsingContext(contextId), url));
        }

        public Navigate(String url, String contextId, ReadinessState readinessState) {
            super("browsingContext.navigate", new NavigateParameters(new BrowsingContext(contextId), url, readinessState));
        }
    }

    public static class Print extends CommandImpl<PrintParameters> implements CommandData {
        public Print(String contextId) {
            super("browsingContext.print", new PrintParameters(new BrowsingContext(contextId)));
        }

        public Print(String contextId, boolean background, PrintParameters.PrintMarginParameters margin, Orientation orientation, PrintParameters.PrintPageParameters page, char pageRanges, float scale, boolean shrinkToFit) {
            super("browsingContext.print", new PrintParameters(new BrowsingContext(contextId), background, margin, orientation, page, pageRanges, scale, shrinkToFit));
        }
    }



    public static class Reload extends CommandImpl<ReloadParameters> implements CommandData {
        public Reload(String contextId) {
            super("browsingContext.reload", new ReloadParameters(new BrowsingContext(contextId)));
        }

        public Reload(String contextId, Boolean ignoreCache, ReadinessState wait) {
            super("browsingContext.reload", new ReloadParameters(new BrowsingContext(contextId), ignoreCache, wait));
        }
    }


    public static class SetViewport extends CommandImpl<SetViewportParameters> implements CommandData {
        public SetViewport(String contextId) {
            super("browsingContext.setViewport", new SetViewportParameters(new BrowsingContext(contextId)));
        }

        public SetViewport(String contextId, SetViewportParameters.Viewport viewport, Float devicePixelRatio) {
            super("browsingContext.setViewport", new SetViewportParameters(new BrowsingContext(contextId), viewport, devicePixelRatio));
        }
    }


    public static class TraverseHistory extends CommandImpl<TraverseHistoryParameters> implements CommandData {
        public TraverseHistory(String contextId, int delta) {
            super("browsingContext.traverseHistory", new TraverseHistoryParameters(new BrowsingContext(contextId), delta));
        }
    }

}
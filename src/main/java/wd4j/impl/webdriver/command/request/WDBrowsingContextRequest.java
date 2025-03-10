package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.webdriver.command.request.helper.WDCommandImpl;
import wd4j.impl.webdriver.command.request.parameters.browsingContext.*;
import wd4j.impl.webdriver.type.browser.WDUserContext;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDLocator;
import wd4j.impl.webdriver.type.browsingContext.WDReadinessState;
import wd4j.impl.webdriver.type.script.WDRemoteReference;
import wd4j.impl.webdriver.type.script.WDSerializationOptions;

import java.util.List;

public class WDBrowsingContextRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Activate extends WDCommandImpl<ActivateParameters> implements WDCommandData {
        public Activate(String contextId) {
            super("browsingContext.activate", new ActivateParameters(new WDBrowsingContext(contextId)));
        }
        public Activate(WDBrowsingContext context) {
            super("browsingContext.activate", new ActivateParameters(context));
        }
    }


    public static class CaptureScreenshot extends WDCommandImpl<CaptureScreenshotParameters> implements WDCommandData {
        public CaptureScreenshot(String contextId) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(new WDBrowsingContext(contextId)));
        }
        public CaptureScreenshot(String contextId, CaptureScreenshotParameters.Origin origin, CaptureScreenshotParameters.ImageFormat format, CaptureScreenshotParameters.ClipRectangle clip) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(new WDBrowsingContext(contextId), origin, format, clip));
        }
        public CaptureScreenshot(WDBrowsingContext context) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(context));
        }
        public CaptureScreenshot(WDBrowsingContext context, CaptureScreenshotParameters.Origin origin, CaptureScreenshotParameters.ImageFormat format, CaptureScreenshotParameters.ClipRectangle clip) {
            super("browsingContext.captureScreenshot", new CaptureScreenshotParameters(context, origin, format, clip));
        }
    }


    public static class Close extends WDCommandImpl<CloseParameters> implements WDCommandData {
        public Close(String contextId) {
            super("browsingContext.close", new CloseParameters(new WDBrowsingContext(contextId)));
        }
        public Close(String contextId, Boolean promptUnload) {
            super("browsingContext.close", new CloseParameters(new WDBrowsingContext(contextId), promptUnload));
        }
        public Close(WDBrowsingContext context) {
            super("browsingContext.close", new CloseParameters(context));
        }
        public Close(WDBrowsingContext context, Boolean promptUnload) {
            super("browsingContext.close", new CloseParameters(context, promptUnload));
        }
    }


    public static class Create extends WDCommandImpl<CreateParameters> implements WDCommandData {
        public Create(CreateType type) {
            super("browsingContext.create", new CreateParameters(type));
        }
        public Create(CreateType type, WDBrowsingContext referenceContext, Boolean background, WDUserContext WDUserContext) {
            super("browsingContext.create", new CreateParameters(type, referenceContext, background, WDUserContext));
        }
    }

    public static class GetTree extends WDCommandImpl<GetTreeParameters> implements WDCommandData {
        public GetTree() {
            super("browsingContext.getTree", new GetTreeParameters());
        }
        public GetTree(Long maxDepth) {
            super("browsingContext.getTree", new GetTreeParameters(maxDepth, null));
        }
        public GetTree(WDBrowsingContext root) {
            super("browsingContext.getTree", new GetTreeParameters(null, root));
        }
        public GetTree(Long maxDepth, WDBrowsingContext root) {
            super("browsingContext.getTree", new GetTreeParameters(maxDepth, root));
        }
    }


    public static class HandleUserPrompt extends WDCommandImpl<HandleUserPromptParameters> implements WDCommandData {
        public HandleUserPrompt(String contextId) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new WDBrowsingContext(contextId)));
        }
        public HandleUserPrompt(String contextId, Boolean accept) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new WDBrowsingContext(contextId), accept));
        }
        public HandleUserPrompt(String contextId, Boolean accept, String userText) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(new WDBrowsingContext(contextId), accept, userText));
        }
        public HandleUserPrompt(WDBrowsingContext context) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(context));
        }
        public HandleUserPrompt(WDBrowsingContext context, Boolean accept) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(context, accept));
        }
        public HandleUserPrompt(WDBrowsingContext context, Boolean accept, String userText) {
            super("browsingContext.handleUserPrompt", new HandleUserPromptParameters(context, accept, userText));
        }
    }

    public static class LocateNodes extends WDCommandImpl<LocateNodesParameters> implements WDCommandData {
        public LocateNodes(String contextId, WDLocator WDLocator) {
            super("browsingContext.locateNodes", new LocateNodesParameters(new WDBrowsingContext(contextId), WDLocator));
        }
        public LocateNodes(String contextId, WDLocator WDLocator, Long maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReference> startNodes) {
            super("browsingContext.locateNodes", new LocateNodesParameters(new WDBrowsingContext(contextId), WDLocator, maxNodeCount, WDSerializationOptions, startNodes));
        }
        public LocateNodes(WDBrowsingContext context, WDLocator WDLocator) {
            super("browsingContext.locateNodes", new LocateNodesParameters(context, WDLocator));
        }
        public LocateNodes(WDBrowsingContext context, WDLocator WDLocator, Long maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReference> startNodes) {
            super("browsingContext.locateNodes", new LocateNodesParameters(context, WDLocator, maxNodeCount, WDSerializationOptions, startNodes));
        }
    }


    public static class Navigate extends WDCommandImpl<NavigateParameters> implements WDCommandData {
        public Navigate(String url, String contextId) {
            super("browsingContext.navigate", new NavigateParameters(new WDBrowsingContext(contextId), url));
        }
        public Navigate(String url, String contextId, WDReadinessState WDReadinessState) {
            super("browsingContext.navigate", new NavigateParameters(new WDBrowsingContext(contextId), url, WDReadinessState));
        }
        public Navigate(String url, WDBrowsingContext context) {
            super("browsingContext.navigate", new NavigateParameters(context, url));
        }
        public Navigate(String url, WDBrowsingContext context, WDReadinessState WDReadinessState) {
            super("browsingContext.navigate", new NavigateParameters(context, url, WDReadinessState));
        }
    }

    public static class Print extends WDCommandImpl<PrintParameters> implements WDCommandData {
        public Print(String contextId) {
            super("browsingContext.print", new PrintParameters(new WDBrowsingContext(contextId)));
        }
        public Print(String contextId, boolean background, PrintParameters.PrintMarginParameters margin, Orientation WDOrientation, PrintParameters.PrintPageParameters page, long pageRanges, float scale, boolean shrinkToFit) {
            super("browsingContext.print", new PrintParameters(new WDBrowsingContext(contextId), background, margin, WDOrientation, page, pageRanges, scale, shrinkToFit));
        }
        public Print(WDBrowsingContext context) {
            super("browsingContext.print", new PrintParameters(context));
        }
        public Print(WDBrowsingContext context, boolean background, PrintParameters.PrintMarginParameters margin, Orientation WDOrientation, PrintParameters.PrintPageParameters page, long pageRanges, float scale, boolean shrinkToFit) {
            super("browsingContext.print", new PrintParameters(context, background, margin, WDOrientation, page, pageRanges, scale, shrinkToFit));
        }
    }



    public static class Reload extends WDCommandImpl<ReloadParameters> implements WDCommandData {
        public Reload(String contextId) {
            super("browsingContext.reload", new ReloadParameters(new WDBrowsingContext(contextId)));
        }
        public Reload(String contextId, Boolean ignoreCache, WDReadinessState wait) {
            super("browsingContext.reload", new ReloadParameters(new WDBrowsingContext(contextId), ignoreCache, wait));
        }
        public Reload(WDBrowsingContext context) {
            super("browsingContext.reload", new ReloadParameters(context));
        }
        public Reload(WDBrowsingContext context, Boolean ignoreCache, WDReadinessState wait) {
            super("browsingContext.reload", new ReloadParameters(context, ignoreCache, wait));
        }
    }


    public static class SetViewport extends WDCommandImpl<SetViewportParameters> implements WDCommandData {
        public SetViewport(String contextId) {
            super("browsingContext.setViewport", new SetViewportParameters(new WDBrowsingContext(contextId)));
        }
        public SetViewport(String contextId, SetViewportParameters.Viewport viewport, Float devicePixelRatio) {
            super("browsingContext.setViewport", new SetViewportParameters(new WDBrowsingContext(contextId), viewport, devicePixelRatio));
        }
        public SetViewport(WDBrowsingContext context) {
            super("browsingContext.setViewport", new SetViewportParameters(context));
        }
        public SetViewport(WDBrowsingContext context, SetViewportParameters.Viewport viewport, Float devicePixelRatio) {
            super("browsingContext.setViewport", new SetViewportParameters(context, viewport, devicePixelRatio));
        }
    }


    public static class TraverseHistory extends WDCommandImpl<TraverseHistoryParameters> implements WDCommandData {
        public TraverseHistory(String contextId, int delta) {
            super("browsingContext.traverseHistory", new TraverseHistoryParameters(new WDBrowsingContext(contextId), delta));
        }
        public TraverseHistory(WDBrowsingContext context, int delta) {
            super("browsingContext.traverseHistory", new TraverseHistoryParameters(context, delta));
        }
    }

}
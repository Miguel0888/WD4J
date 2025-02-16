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

    public static class Activate extends WDCommandImpl<WDActivateParameters> implements WDCommandData {
        public Activate(String contextId) {
            super("browsingContext.activate", new WDActivateParameters(new WDBrowsingContext(contextId)));
        }
        public Activate(WDBrowsingContext context) {
            super("browsingContext.activate", new WDActivateParameters(context));
        }
    }


    public static class CaptureScreenshot extends WDCommandImpl<WDCaptureScreenshotParameters> implements WDCommandData {
        public CaptureScreenshot(String contextId) {
            super("browsingContext.captureScreenshot", new WDCaptureScreenshotParameters(new WDBrowsingContext(contextId)));
        }
        public CaptureScreenshot(String contextId, WDCaptureScreenshotParameters.Origin origin, WDCaptureScreenshotParameters.ImageFormat format, WDCaptureScreenshotParameters.ClipRectangle clip) {
            super("browsingContext.captureScreenshot", new WDCaptureScreenshotParameters(new WDBrowsingContext(contextId), origin, format, clip));
        }
        public CaptureScreenshot(WDBrowsingContext context) {
            super("browsingContext.captureScreenshot", new WDCaptureScreenshotParameters(context));
        }
        public CaptureScreenshot(WDBrowsingContext context, WDCaptureScreenshotParameters.Origin origin, WDCaptureScreenshotParameters.ImageFormat format, WDCaptureScreenshotParameters.ClipRectangle clip) {
            super("browsingContext.captureScreenshot", new WDCaptureScreenshotParameters(context, origin, format, clip));
        }
    }


    public static class Close extends WDCommandImpl<WDCloseParameters> implements WDCommandData {
        public Close(String contextId) {
            super("browsingContext.close", new WDCloseParameters(new WDBrowsingContext(contextId)));
        }
        public Close(String contextId, boolean promptUnload) {
            super("browsingContext.close", new WDCloseParameters(new WDBrowsingContext(contextId), promptUnload));
        }
        public Close(WDBrowsingContext context) {
            super("browsingContext.close", new WDCloseParameters(context));
        }
        public Close(WDBrowsingContext context, boolean promptUnload) {
            super("browsingContext.close", new WDCloseParameters(context, promptUnload));
        }
    }


    public static class Create extends WDCommandImpl<WDCreateParameters> implements WDCommandData {
        public Create(WDCreateType type) {
            super("browsingContext.create", new WDCreateParameters(type));
        }
        public Create(WDCreateType type, WDBrowsingContext referenceContext, Boolean background, WDUserContext WDUserContext) {
            super("browsingContext.create", new WDCreateParameters(type, referenceContext, background, WDUserContext));
        }
    }

    public static class GetTree extends WDCommandImpl<WDGetTreeParameters> implements WDCommandData {
        public GetTree() {
            super("browsingContext.getTree", new WDGetTreeParameters());
        }
        public GetTree(Character maxDepth, WDBrowsingContext root) {
            super("browsingContext.getTree", new WDGetTreeParameters(maxDepth, root));
        }
    }


    public static class HandleUserPrompt extends WDCommandImpl<WDHandleUserPromptParameters> implements WDCommandData {
        public HandleUserPrompt(String contextId) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(new WDBrowsingContext(contextId)));
        }
        public HandleUserPrompt(String contextId, Boolean accept) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(new WDBrowsingContext(contextId), accept));
        }
        public HandleUserPrompt(String contextId, Boolean accept, String userText) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(new WDBrowsingContext(contextId), accept, userText));
        }
        public HandleUserPrompt(WDBrowsingContext context) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(context));
        }
        public HandleUserPrompt(WDBrowsingContext context, Boolean accept) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(context, accept));
        }
        public HandleUserPrompt(WDBrowsingContext context, Boolean accept, String userText) {
            super("browsingContext.handleUserPrompt", new WDHandleUserPromptParameters(context, accept, userText));
        }
    }

    public static class LocateNodes extends WDCommandImpl<WDLocateNodesParameters> implements WDCommandData {
        public LocateNodes(String contextId, WDLocator WDLocator) {
            super("browsingContext.locateNodes", new WDLocateNodesParameters(new WDBrowsingContext(contextId), WDLocator));
        }
        public LocateNodes(String contextId, WDLocator WDLocator, Character maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReferenceWD> startNodes) {
            super("browsingContext.locateNodes", new WDLocateNodesParameters(new WDBrowsingContext(contextId), WDLocator, maxNodeCount, WDSerializationOptions, startNodes));
        }
        public LocateNodes(WDBrowsingContext context, WDLocator WDLocator) {
            super("browsingContext.locateNodes", new WDLocateNodesParameters(context, WDLocator));
        }
        public LocateNodes(WDBrowsingContext context, WDLocator WDLocator, Character maxNodeCount, WDSerializationOptions WDSerializationOptions, List<WDRemoteReference.SharedReferenceWD> startNodes) {
            super("browsingContext.locateNodes", new WDLocateNodesParameters(context, WDLocator, maxNodeCount, WDSerializationOptions, startNodes));
        }
    }


    public static class Navigate extends WDCommandImpl<WDNavigateParameters> implements WDCommandData {
        public Navigate(String url, String contextId) {
            super("browsingContext.navigate", new WDNavigateParameters(new WDBrowsingContext(contextId), url));
        }
        public Navigate(String url, String contextId, WDReadinessState WDReadinessState) {
            super("browsingContext.navigate", new WDNavigateParameters(new WDBrowsingContext(contextId), url, WDReadinessState));
        }
        public Navigate(String url, WDBrowsingContext context) {
            super("browsingContext.navigate", new WDNavigateParameters(context, url));
        }
        public Navigate(String url, WDBrowsingContext context, WDReadinessState WDReadinessState) {
            super("browsingContext.navigate", new WDNavigateParameters(context, url, WDReadinessState));
        }
    }

    public static class Print extends WDCommandImpl<WDPrintParameters> implements WDCommandData {
        public Print(String contextId) {
            super("browsingContext.print", new WDPrintParameters(new WDBrowsingContext(contextId)));
        }
        public Print(String contextId, boolean background, WDPrintParameters.PrintMarginParameters margin, WDOrientation WDOrientation, WDPrintParameters.PrintPageParameters page, char pageRanges, float scale, boolean shrinkToFit) {
            super("browsingContext.print", new WDPrintParameters(new WDBrowsingContext(contextId), background, margin, WDOrientation, page, pageRanges, scale, shrinkToFit));
        }
        public Print(WDBrowsingContext context) {
            super("browsingContext.print", new WDPrintParameters(context));
        }
        public Print(WDBrowsingContext context, boolean background, WDPrintParameters.PrintMarginParameters margin, WDOrientation WDOrientation, WDPrintParameters.PrintPageParameters page, char pageRanges, float scale, boolean shrinkToFit) {
            super("browsingContext.print", new WDPrintParameters(context, background, margin, WDOrientation, page, pageRanges, scale, shrinkToFit));
        }
    }



    public static class Reload extends WDCommandImpl<WDReloadParameters> implements WDCommandData {
        public Reload(String contextId) {
            super("browsingContext.reload", new WDReloadParameters(new WDBrowsingContext(contextId)));
        }
        public Reload(String contextId, Boolean ignoreCache, WDReadinessState wait) {
            super("browsingContext.reload", new WDReloadParameters(new WDBrowsingContext(contextId), ignoreCache, wait));
        }
        public Reload(WDBrowsingContext context) {
            super("browsingContext.reload", new WDReloadParameters(context));
        }
        public Reload(WDBrowsingContext context, Boolean ignoreCache, WDReadinessState wait) {
            super("browsingContext.reload", new WDReloadParameters(context, ignoreCache, wait));
        }
    }


    public static class SetViewport extends WDCommandImpl<WDSetViewportParameters> implements WDCommandData {
        public SetViewport(String contextId) {
            super("browsingContext.setViewport", new WDSetViewportParameters(new WDBrowsingContext(contextId)));
        }
        public SetViewport(String contextId, WDSetViewportParameters.Viewport viewport, Float devicePixelRatio) {
            super("browsingContext.setViewport", new WDSetViewportParameters(new WDBrowsingContext(contextId), viewport, devicePixelRatio));
        }
        public SetViewport(WDBrowsingContext context) {
            super("browsingContext.setViewport", new WDSetViewportParameters(context));
        }
        public SetViewport(WDBrowsingContext context, WDSetViewportParameters.Viewport viewport, Float devicePixelRatio) {
            super("browsingContext.setViewport", new WDSetViewportParameters(context, viewport, devicePixelRatio));
        }
    }


    public static class TraverseHistory extends WDCommandImpl<WDTraverseHistoryParameters> implements WDCommandData {
        public TraverseHistory(String contextId, int delta) {
            super("browsingContext.traverseHistory", new WDTraverseHistoryParameters(new WDBrowsingContext(contextId), delta));
        }
        public TraverseHistory(WDBrowsingContext context, int delta) {
            super("browsingContext.traverseHistory", new WDTraverseHistoryParameters(context, delta));
        }
    }

}
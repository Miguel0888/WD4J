package de.bund.zrb.websocket;

import de.bund.zrb.webdriver.command.request.*;
import de.bund.zrb.webdriver.command.response.*;
import de.bund.zrb.webdriver.type.browser.WDClientWindowInfo;
import de.bund.zrb.webdriver.type.browser.WDUserContextInfo;
import de.bund.zrb.webdriver.type.script.WDEvaluateResult;

public interface WebSocketManager {

    WDScriptResult.AddPreloadScriptResult sendAndWaitForResponse(WDScriptRequest.AddPreloadScript addPreloadScript, Class<WDScriptResult.AddPreloadScriptResult> addPreloadScriptResultClass);

    void sendAndWaitForResponse(WDScriptRequest.Disown disown, Class<WDEmptyResult> wdEmptyResultClass);

    WDEvaluateResult sendAndWaitForResponse(WDScriptRequest.CallFunction callFunction, Class<WDEvaluateResult> wdEvaluateResultClass);

    WDEvaluateResult sendAndWaitForResponse(WDScriptRequest.Evaluate evaluate, Class<WDEvaluateResult> wdEvaluateResultClass);

    WDScriptResult.GetRealmsResult sendAndWaitForResponse(WDScriptRequest.GetRealms getRealms, Class<WDScriptResult.GetRealmsResult> getRealmsResultClass);

    void sendAndWaitForResponse(WDScriptRequest.RemovePreloadScript removePreloadScript, Class<WDEmptyResult> wdEmptyResultClass);

    boolean isConnected();

    void sendAndWaitForResponse(WDBrowserRequest.Close close, Class<WDEmptyResult> wdEmptyResultClass);

    WDBrowserResult.CreateUserContextResult sendAndWaitForResponse(WDBrowserRequest.CreateUserContext createUserContext, Class<WDUserContextInfo> wdUserContextInfoClass);

    WDBrowserResult.GetClientWindowsResult sendAndWaitForResponse(WDBrowserRequest.GetClientWindows getClientWindows, Class<WDBrowserResult.GetClientWindowsResult> getClientWindowsResultClass);

    WDBrowserResult.GetUserContextsResult sendAndWaitForResponse(WDBrowserRequest.GetUserContexts getUserContexts, Class<WDBrowserResult.GetUserContextsResult> getUserContextsResultClass);

    void sendAndWaitForResponse(WDBrowserRequest.RemoveUserContext removeUserContext, Class<WDEmptyResult> wdEmptyResultClass);

    WDClientWindowInfo sendAndWaitForResponse(WDBrowserRequest.SetClientWindowState setClientWindowState, Class<WDClientWindowInfo> wdClientWindowInfoClass);

    WDBrowsingContextResult.CreateResult sendAndWaitForResponse(WDBrowsingContextRequest.Create create, Class<WDBrowsingContextResult.CreateResult> createResultClass);

    WDBrowsingContextResult.NavigateResult sendAndWaitForResponse(WDBrowsingContextRequest.Navigate navigate, Class<WDBrowsingContextResult.NavigateResult> navigateResultClass);

    WDBrowsingContextResult.GetTreeResult sendAndWaitForResponse(WDBrowsingContextRequest.GetTree getTree, Class<WDBrowsingContextResult.GetTreeResult> getTreeResultClass);

    void sendAndWaitForResponse(WDBrowsingContextRequest.Activate activate, Class<WDEmptyResult> wdEmptyResultClass);

    WDBrowsingContextResult.CaptureScreenshotResult sendAndWaitForResponse(WDBrowsingContextRequest.CaptureScreenshot captureScreenshot, Class<WDBrowsingContextResult.CaptureScreenshotResult> captureScreenshotResultClass);

    void sendAndWaitForResponse(WDBrowsingContextRequest.Close close, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDBrowsingContextRequest.HandleUserPrompt handleUserPrompt, Class<WDEmptyResult> wdEmptyResultClass);

    WDBrowsingContextResult.LocateNodesResult sendAndWaitForResponse(WDBrowsingContextRequest.LocateNodes locateNodes, Class<WDBrowsingContextResult.LocateNodesResult> locateNodesResultClass);

    WDBrowsingContextResult.PrintResult sendAndWaitForResponse(WDBrowsingContextRequest.Print print, Class<WDBrowsingContextResult.PrintResult> printResultClass);

    void sendAndWaitForResponse(WDBrowsingContextRequest.Reload reload, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDBrowsingContextRequest.SetViewport setViewport, Class<WDEmptyResult> wdEmptyResultClass);

    WDBrowsingContextResult.TraverseHistoryResult sendAndWaitForResponse(WDBrowsingContextRequest.TraverseHistory traverseHistory, Class<WDBrowsingContextResult.TraverseHistoryResult> traverseHistoryResultClass);

    void sendAndWaitForResponse(WDInputRequest.PerformActions performActions, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDInputRequest.ReleaseActions releaseActions, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDInputRequest.SetFiles setFiles, Class<WDEmptyResult> wdEmptyResultClass);

    WDNetworkResult.AddInterceptResult sendAndWaitForResponse(WDNetworkRequest.AddIntercept addIntercept, Class<WDNetworkResult.AddInterceptResult> addInterceptResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.ContinueRequest continueRequest, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.ContinueResponse continueResponse, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.ContinueWithAuth continueWithAuth, Class<WDEmptyResult> wdEmptyResultClass);


    void sendAndWaitForResponse(WDNetworkRequest.FailRequest failRequest, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.ProvideResponse provideResponse, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.RemoveIntercept removeIntercept, Class<WDEmptyResult> wdEmptyResultClass);

    void sendAndWaitForResponse(WDNetworkRequest.SetCacheBehavior setCacheBehavior, Class<WDEmptyResult> wdEmptyResultClass);

    WDSessionResult.StatusResult sendAndWaitForResponse(WDSessionRequest.Status status, Class<WDSessionResult.StatusResult> statusResultClass);

    WDSessionResult.NewResult sendAndWaitForResponse(WDSessionRequest.New aNew, Class<WDSessionResult.NewResult> newResultClass);

    void sendAndWaitForResponse(WDSessionRequest.End end, Class<WDEmptyResult> wdEmptyResultClass);

    WDSessionResult.SubscribeResult sendAndWaitForResponse(WDSessionRequest.Subscribe subscribeCommand, Class<WDSessionResult.SubscribeResult> subscribeResultClass);

    void sendAndWaitForResponse(WDSessionRequest.Unsubscribe unsubscribeRequest, Class<WDEmptyResult> wdEmptyResultClass);

    WDStorageResult.GetCookieResult sendAndWaitForResponse(WDStorageRequest.GetCookies getCookies, Class<WDStorageResult.GetCookieResult> getCookieResultClass);

    WDStorageResult.SetCookieResult sendAndWaitForResponse(WDStorageRequest.SetCookie setCookie, Class<WDStorageResult.SetCookieResult> setCookieResultClass);

    WDStorageResult.DeleteCookiesResult sendAndWaitForResponse(WDStorageRequest.DeleteCookies deleteCookies, Class<WDStorageResult.DeleteCookiesResult> deleteCookiesResultClass);

    WDWebExtensionResult.InstallResult sendAndWaitForResponse(WDWebExtensionRequest.Install install, Class<WDWebExtensionResult.InstallResult> installResultClass);

    void sendAndWaitForResponse(WDWebExtensionRequest.Uninstall uninstall, Class<WDEmptyResult> wdEmptyResultClass);

}

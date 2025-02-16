package wd4j.impl.playwright;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wd4j.api.*;
import wd4j.api.options.AriaRole;
import wd4j.api.options.BoundingBox;
import wd4j.api.options.FilePayload;
import wd4j.api.options.SelectOption;
import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;
import wd4j.impl.websocket.WebSocketManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class LocatorImpl implements Locator {
    private final String selector;
    private final String contextId;
    private final WebSocketManager webSocket;

//    public LocatorImpl(String selector) {
//        // TODO: Implement
//        this.selector = selector;
//        this.webSocket = null; // ToDo
//    }
//
//    public LocatorImpl(String selector, WebSocketImpl webSocket) {
//        this.selector = selector;
//        this.webSocket = webSocket;
//    }

    public LocatorImpl(String selector, String contextId, WebSocketManager webSocketManager) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        this.selector = selector;
        this.contextId = contextId;
        this.webSocket = webSocketManager;
    }

    @Override
    public List<Locator> all() {
        try {
            // 1. Hole den DOM-Baum für den Kontext
//            CompletableFuture<WebSocketFrame> futureResponse = webSocket.send(new BrowsingContextRequest.GetTree());
//            String jsonResponse = futureResponse.get(5, TimeUnit.SECONDS).text();
//            System.out.println("===>" + jsonResponse);

            String jsonResponse  = webSocket.sendAndWaitForResponse(new WDBrowsingContextRequest.GetTree(), String.class);

            JsonObject json = new Gson().fromJson(jsonResponse, JsonObject.class);
            JsonArray nodes = json.getAsJsonArray("nodes");

            // 2. Finde Elemente mit `script.evaluate`Result Type
            List<Locator> locators = new ArrayList<>();
            for (JsonElement node : nodes) {
                // ToDo: Fix Evalutate Call

//                CompletableFuture<WebSocketFrame> evalResponse = webSocket.send(new ScriptRequest.Evaluate(contextId, selector));
//                String evalJsonResponse = evalResponse.get(5, TimeUnit.SECONDS).text();
//                JsonObject evalJson = new Gson().fromJson(evalJsonResponse, JsonObject.class);

//                if (evalJson.has("result") && !evalJson.get("result").isJsonNull()) {
//                    locators.add(new LocatorImpl(selector, contextId, webSocket));
//                }

                System.out.println(" - " + node.toString());
            }
            return locators;

        } catch (Exception e) {
            throw new RuntimeException("Failed to locate elements: " + e.getMessage(), e);
        }
    }



    @Override
    public List<String> allInnerTexts() {
        return Collections.emptyList();
    }

    @Override
    public List<String> allTextContents() {
        return Collections.emptyList();
    }

    @Override
    public Locator and(Locator locator) {
        return null;
    }

    @Override
    public String ariaSnapshot(AriaSnapshotOptions options) {
        return "";
    }

    @Override
    public void blur(BlurOptions options) {

    }

    @Override
    public BoundingBox boundingBox(BoundingBoxOptions options) {
        return null;
    }

    @Override
    public void check(CheckOptions options) {

    }

    @Override
    public void clear(ClearOptions options) {

    }

    @Override
    public void click(ClickOptions options) {
        String contextId = "default"; // Sollte aus Page kommen
//        webSocket.send(new BrowsingContext.DispatchEvent(contextId, selector, "click"));
    }


    @Override
    public int count() {
        return 0;
    }

    @Override
    public void dblclick(DblclickOptions options) {

    }

    @Override
    public void dispatchEvent(String type, Object eventInit, DispatchEventOptions options) {

    }

    @Override
    public void dragTo(Locator target, DragToOptions options) {

    }

    @Override
    public ElementHandle elementHandle(ElementHandleOptions options) {
        return null;
    }

    @Override
    public List<ElementHandle> elementHandles() {
        return Collections.emptyList();
    }

    @Override
    public FrameLocator contentFrame() {
        return null;
    }

    @Override
    public Object evaluate(String expression, Object arg, EvaluateOptions options) {
        return null;
    }

    @Override
    public Object evaluateAll(String expression, Object arg) {
        return null;
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg, EvaluateHandleOptions options) {
        return null;
    }

    @Override
    public void fill(String value, FillOptions options) {

    }

    @Override
    public Locator filter(FilterOptions options) {
        return null;
    }

    @Override
    public Locator first() {
        return null;
    }

    @Override
    public void focus(FocusOptions options) {

    }

    @Override
    public FrameLocator frameLocator(String selector) {
        return null;
    }

    @Override
    public String getAttribute(String name, GetAttributeOptions options) {
        return "";
    }

    @Override
    public Locator getByAltText(String text, GetByAltTextOptions options) {
        return null;
    }

    @Override
    public Locator getByAltText(Pattern text, GetByAltTextOptions options) {
        return null;
    }

    @Override
    public Locator getByLabel(String text, GetByLabelOptions options) {
        return null;
    }

    @Override
    public Locator getByLabel(Pattern text, GetByLabelOptions options) {
        return null;
    }

    @Override
    public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) {
        return null;
    }

    @Override
    public Locator getByPlaceholder(Pattern text, GetByPlaceholderOptions options) {
        return null;
    }

    @Override
    public Locator getByRole(AriaRole role, GetByRoleOptions options) {
        return null;
    }

    @Override
    public Locator getByTestId(String testId) {
        return null;
    }

    @Override
    public Locator getByTestId(Pattern testId) {
        return null;
    }

    @Override
    public Locator getByText(String text, GetByTextOptions options) {
        return null;
    }

    @Override
    public Locator getByText(Pattern text, GetByTextOptions options) {
        return null;
    }

    @Override
    public Locator getByTitle(String text, GetByTitleOptions options) {
        return null;
    }

    @Override
    public Locator getByTitle(Pattern text, GetByTitleOptions options) {
        return null;
    }

    @Override
    public void highlight() {

    }

    @Override
    public void hover(HoverOptions options) {

    }

    @Override
    public String innerHTML(InnerHTMLOptions options) {
        return "";
    }

    @Override
    public String innerText(InnerTextOptions options) {
        return "";
    }

    @Override
    public String inputValue(InputValueOptions options) {
        return "";
    }

    @Override
    public boolean isChecked(IsCheckedOptions options) {
        return false;
    }

    @Override
    public boolean isDisabled(IsDisabledOptions options) {
        return false;
    }

    @Override
    public boolean isEditable(IsEditableOptions options) {
        return false;
    }

    @Override
    public boolean isEnabled(IsEnabledOptions options) {
        return false;
    }

    @Override
    public boolean isHidden(IsHiddenOptions options) {
        return false;
    }

    @Override
    public boolean isVisible(IsVisibleOptions options) {
        return false;
    }

    @Override
    public Locator last() {
        return null;
    }

    @Override
    public Locator locator(String selectorOrLocator, LocatorOptions options) {
        return null;
    }

    @Override
    public Locator locator(Locator selectorOrLocator, LocatorOptions options) {
        return null;
    }

    @Override
    public Locator nth(int index) {
        return null;
    }

    @Override
    public Locator or(Locator locator) {
        return null;
    }

    @Override
    public Page page() {
        return null;
    }

    @Override
    public void press(String key, PressOptions options) {

    }

    @Override
    public void pressSequentially(String text, PressSequentiallyOptions options) {

    }

    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        return new byte[0];
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {

    }

    @Override
    public List<String> selectOption(String values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(ElementHandle values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(SelectOption values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(ElementHandle[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(SelectOption[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public void selectText(SelectTextOptions options) {

    }

    @Override
    public void setChecked(boolean checked, SetCheckedOptions options) {

    }

    @Override
    public void setInputFiles(Path files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(Path[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(FilePayload files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(FilePayload[] files, SetInputFilesOptions options) {

    }

    @Override
    public void tap(TapOptions options) {

    }

    @Override
    public String textContent(TextContentOptions options) {
        return "";
    }

    @Override
    public void type(String text, TypeOptions options) {

    }

    @Override
    public void uncheck(UncheckOptions options) {

    }

    @Override
    public void waitFor(WaitForOptions options) {

    }
}

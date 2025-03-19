package wd4j.impl.playwright;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wd4j.api.ElementHandle;
import wd4j.api.FrameLocator;
import wd4j.api.JSHandle;
import wd4j.api.Locator;
import wd4j.api.Page;
import wd4j.api.options.AriaRole;
import wd4j.api.options.BoundingBox;
import wd4j.api.options.FilePayload;
import wd4j.api.options.SelectOption;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;
import wd4j.impl.webdriver.command.response.WDBrowsingContextResult;
import wd4j.impl.webdriver.type.browsingContext.WDInfo;
import wd4j.impl.webdriver.type.browsingContext.WDLocator;
import wd4j.impl.webdriver.type.script.WDEvaluateResult;
import wd4j.impl.webdriver.type.script.WDLocalValue;
import wd4j.impl.webdriver.type.script.WDPrimitiveProtocolValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static wd4j.impl.support.WDRemoteValueUtil.getBoundingBoxFromEvaluateResult;
import static wd4j.impl.support.WDRemoteValueUtil.getStringFromEvaluateResult;

public class LocatorImpl implements Locator {
    private final PageImpl page;
    private final String selector;

    private String sharedId; // Wird erst beim ersten Zugriff gesetzt

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public LocatorImpl(PageImpl page, String selector) {
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null.");
        }
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        this.page = page;
        this.selector = selector;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Webdriver Interaction
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveSharedId() {
        if (sharedId == null) {
            WDLocator<?> locator = createWDLocator(selector);
            WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getBrowsingContextManager().locateNodes(
                    page.getBrowsingContextId(),
                    locator
            );
            if (nodes.getNodes().isEmpty()) {
                throw new RuntimeException("No nodes found for selector: " + selector);
            }
            sharedId = nodes.getNodes().get(0).getSharedId().value();
        }
    }

    public static WDLocator<?> createWDLocator(String selector) {
        if (selector.startsWith("/") || selector.startsWith("(")) {
            return new WDLocator.XPathLocator(selector);
        } else if (selector.startsWith("text=")) {
            return new WDLocator.InnerTextLocator(selector.substring(5));
        } else if (selector.startsWith("aria=")) {
            String name = selector.substring(5);
            String role = null; // ToDo: Implement
            WDLocator.AccessibilityLocator.Value value = new WDLocator.AccessibilityLocator.Value(name, role);
            return new WDLocator.AccessibilityLocator(value);
        } else {
            return new WDLocator.CssLocator(selector);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Overridden Methods / Implementation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<Locator> all() {
        try {
            // ToDo: Das lässt sich vielleicht mit LocateNodes und / oder CallFunction viel einfacher lösen ??

            WDBrowsingContextResult.GetTreeResult response = page.getBrowser().getBrowsingContextManager().getTree(page.getBrowsingContextId());

            if(response == null) {
                throw new RuntimeException("Failed to locate elements: Response is null");
            }
            Collection<WDInfo> contexts = response.getContexts();
            if(contexts == null) {
                throw new RuntimeException("Failed to locate elements: Contexts are null");
            }

            List<Locator> locators = new ArrayList<>();
            for (WDInfo context : contexts) {
                System.out.println("Context: " + context);
                // ToDo: Implement!
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
        // ToDo: Use Options
        resolveSharedId();
        page.getBrowser().getScriptManager().executeDomAction(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomAction.CLICK
        );
    }

    @Override
    public int count() {
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getBrowsingContextManager().locateNodes(
                page.getBrowsingContextId(),
                locator
        );
        return nodes.getNodes().size();
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
        // ToDo: Use Options
        resolveSharedId();
        List<WDLocalValue> args = new ArrayList<>();
        args.add(new WDPrimitiveProtocolValue.StringValue(value));
        page.getBrowser().getScriptManager().executeDomAction(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomAction.INPUT,
                args
        );
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
        // ToDo: Use Options
        resolveSharedId();
        page.getBrowser().getScriptManager().executeDomAction(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomAction.FOCUS
        );
    }

    @Override
    public FrameLocator frameLocator(String selector) {
        return null;
    }

    @Override
    public String getAttribute(String name, GetAttributeOptions options) {
        // ToDo: Use Options
        resolveSharedId();
        List<WDLocalValue> args = new ArrayList<>();
        args.add(new WDPrimitiveProtocolValue.StringValue(name));
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomQuery.GET_ATTRIBUTES,
                args
        );
        return getStringFromEvaluateResult(result);
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
        // ToDo: Use Options
        resolveSharedId();
        page.getBrowser().getScriptManager().executeDomAction(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomAction.HOVER
        );
    }

    @Override
    public String innerHTML(InnerHTMLOptions options) {
        return "";
    }

    @Override
    public String innerText(InnerTextOptions options) {
        resolveSharedId();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomQuery.GET_INNER_TEXT
        );
        return getStringFromEvaluateResult(result);
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
    public boolean isVisible(IsVisibleOptions options) { // ToDo: Is implementation correct?
        // ToDo: Use Options
        resolveSharedId();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomQuery.GET_BOUNDING_BOX
        );
        BoundingBox box = getBoundingBoxFromEvaluateResult(result);
        return box != null && box.width > 0 && box.height > 0;
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

    /**
     * @deprecated In most cases, you should use {@link Locator#fill Locator.fill()} instead. You only need to
     * press keys one by one if there is special keyboard handling on the page - in this case use {@link
     * Locator#pressSequentially Locator.pressSequentially()}.
     *
     * @param text A text to type into a focused element.
     * @since v1.14
     */
    @Override
    public void type(String text, TypeOptions options) {
        // ToDo: Use Options
        resolveSharedId();
        List<WDLocalValue> args = Collections.singletonList(new WDPrimitiveProtocolValue.StringValue(text));
        page.getBrowser().getScriptManager().executeDomAction(
                page.getBrowsingContextId(),
                sharedId,
                WDScriptManager.DomAction.INPUT,
                args
        );
    }

    @Override
    public void uncheck(UncheckOptions options) {

    }

    @Override
    public void waitFor(WaitForOptions options) {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



}

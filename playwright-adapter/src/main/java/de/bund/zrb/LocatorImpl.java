package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.SelectOption;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.command.request.parameters.browsingContext.CaptureScreenshotParameters;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDInfo;
import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.type.script.*;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static de.bund.zrb.support.WDRemoteValueUtil.*;

public class LocatorImpl implements Locator {
    private final WebDriver webDriver;

    private final PageImpl page;
    private final String selector;

    private ElementHandleImpl elementHandle; // Wird erst beim ersten Zugriff gesetzt

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public LocatorImpl(WebDriver webDriver, PageImpl page, String selector) {
        if (webDriver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null.");
        }
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        this.webDriver = webDriver;
        this.page = page;
        this.selector = selector;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WebDriver getWebDriver() {
        return webDriver;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Webdriver Interaction
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveElementHandle() {
        if (elementHandle == null) {
            WDLocator<?> locator = createWDLocator(selector);
            WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver().browsingContext().locateNodes(
                    page.getBrowsingContextId(),
                    locator
            );
            if (nodes.getNodes().isEmpty()) {
                throw new RuntimeException("No nodes found for selector: " + selector);
            }
            WDHandle handle = new WDHandle(nodes.getNodes().get(0).getHandle().value());
            WDSharedId sharedId = nodes.getNodes().get(0).getSharedId();
            if(sharedId != null) {
                WDRemoteReference.SharedReference reference = new WDRemoteReference.SharedReference(sharedId, handle);
                elementHandle = new ElementHandleImpl(page.getWebDriver(), reference, new WDTarget.ContextTarget(page.getBrowsingContext()));
            }
            else if(handle != null) {
                WDRemoteReference.RemoteObjectReference reference = new WDRemoteReference.RemoteObjectReference(handle, sharedId);
                new JSHandleImpl(page.getWebDriver(), reference, new WDTarget.ContextTarget(page.getBrowsingContext()));
                throw new RuntimeException("No sharedId found for selector: " + selector + " handle: " + handle);
            }
            else {
                throw new RuntimeException("No handle found for selector: " + selector);
            }



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

            WDBrowsingContextResult.GetTreeResult response = page.getBrowser().getWebDriver().browsingContext().getTree(page.getBrowsingContextId());

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
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver().browsingContext().locateNodes(
                page.getBrowsingContextId(),
                locator,
                Long.MAX_VALUE
        );

        List<String> innerTexts = new ArrayList<>();
        for (WDRemoteValue.NodeRemoteValue node : nodes.getNodes()) {
            WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                    page.getBrowsingContextId(),
                    node.getSharedId().value(),
                    WDScriptManager.DomQuery.GET_INNER_TEXT
            );
            innerTexts.add(getStringFromEvaluateResult(result));
        }
        return innerTexts;
    }

    @Override
    public List<String> allTextContents() {
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver().browsingContext().locateNodes(
                page.getBrowsingContextId(),
                locator,
                Long.MAX_VALUE
        );

        List<String> textContents = new ArrayList<>();
        for (WDRemoteValue.NodeRemoteValue node : nodes.getNodes()) {
            WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                    page.getBrowsingContextId(),
                    node.getSharedId().value(),
                    WDScriptManager.DomQuery.GET_TEXT_CONTENT
            );
            textContents.add(getStringFromEvaluateResult(result));
        }
        return textContents;
    }

    @Override
    public Locator and(Locator locator) {
        if (!(locator instanceof LocatorImpl)) {
            throw new IllegalArgumentException("Locator must be of type LocatorImpl.");
        }
        return new LocatorImpl(webDriver, page, this.selector + " and " + ((LocatorImpl) locator).selector); // ToDo: Check this!
    }

    @Override
    public String ariaSnapshot(AriaSnapshotOptions options) {
        return ""; // ToDo: Implement
    }

    @Override
    public void blur(BlurOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.BLUR
        );
    }

    @Override
    public BoundingBox boundingBox(BoundingBoxOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.GET_BOUNDING_BOX
        );
        return getBoundingBoxFromEvaluateResult(result);
    }

    @Override
    public void check(CheckOptions options) {
        // ToDo: Use Options
        SetCheckedOptions setCheckedOptions = new SetCheckedOptions(); // ToDo: Is this correct?
        setChecked(true, setCheckedOptions);
    }

    @Override
    public void clear(ClearOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.CLEAR_INPUT
        );
    }

    @Override
    public void click(ClickOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.CLICK
        );
    }

    @Override
    public int count() {
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver().browsingContext().locateNodes(
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
        resolveElementHandle();
        if(!(target instanceof LocatorImpl)) {
            throw new IllegalArgumentException("Target must be a LocatorImpl.");
        }
        ((LocatorImpl) target).resolveElementHandle();

        List<WDLocalValue> args = new ArrayList<>();
        // ToDo. Fix Target
//        args.add(new WDPrimitiveProtocolValue.StringValue(elementHandle));
//        args.add(new WDPrimitiveProtocolValue.StringValue(((LocatorImpl) target).elementHandle));

        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.DRAG_AND_DROP,
                args
        );
    }

    @Override
    public ElementHandle elementHandle(ElementHandleOptions options) {
        // ToDo: Use Options
        resolveElementHandle();

        return new ElementHandleImpl(page.getWebDriver(),
                ((WDRemoteReference.SharedReference) elementHandle.getRemoteReference()),
                new WDTarget.ContextTarget(page.getBrowsingContext()));
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
        resolveElementHandle();
        List<WDLocalValue> args = new ArrayList<>();
        args.add(new WDPrimitiveProtocolValue.StringValue(value));
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
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
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
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
        resolveElementHandle();
        List<WDLocalValue> args = new ArrayList<>();
        args.add(new WDPrimitiveProtocolValue.StringValue(name));
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
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
        // ToDo: Use Options
        return new LocatorImpl(webDriver, page, "aria=" + text);
    }

    @Override
    public Locator getByLabel(Pattern text, GetByLabelOptions options) {
        return null;
    }

    @Override
    public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) {
        // ToDo: Use Options
        return new LocatorImpl(webDriver, page, "aria=" + text);
    }

    @Override
    public Locator getByPlaceholder(Pattern text, GetByPlaceholderOptions options) {
        return new LocatorImpl(webDriver, page, "aria=" + text);
    }

    @Override
    public Locator getByRole(AriaRole role, GetByRoleOptions options) {
        // ToDo: Use Options
        String roleString = role.name().toLowerCase();
        // ToDo: Fix this!
        WDLocator.AccessibilityLocator.Value value = new WDLocator.AccessibilityLocator.Value(null, roleString);
        return new LocatorImpl(webDriver, page, "aria=" + roleString);
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
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.HIGHLIGHT
        );
    }

    @Override
    public void hover(HoverOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.HOVER
        );
    }

    @Override
    public String innerHTML(InnerHTMLOptions options) {
        return "";
    }

    @Override
    public String innerText(InnerTextOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
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
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.IS_CHECKED
        );
        return Boolean.TRUE.equals(getBooleanFromEvaluateResult(result)); // ToDo: Check this!
    }

    @Override
    public boolean isDisabled(IsDisabledOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.GET_ATTRIBUTES,
                Collections.singletonList(new WDPrimitiveProtocolValue.StringValue("disabled"))
        );
        Boolean value = getBooleanFromEvaluateResult(result);
        return value != null && value;  // ✅ Falls `null`, wird `false` zurückgegeben
    }

    @Override
    public boolean isEditable(IsEditableOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.IS_EDITABLE
        );
        return Boolean.TRUE.equals(getBooleanFromEvaluateResult(result)); // ToDo: Check this!
    }

    @Override
    public boolean isEnabled(IsEnabledOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.IS_ENABLED
        );
        return Boolean.TRUE.equals(getBooleanFromEvaluateResult(result)); // ToDo: Check this!
    }

    @Override
    public boolean isHidden(IsHiddenOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.IS_HIDDEN
        );
        return Boolean.TRUE.equals(getBooleanFromEvaluateResult(result)); // ToDo: Check this!
    }

    @Override
    public boolean isVisible(IsVisibleOptions options) { // ToDo: Is implementation correct?
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
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
        return new LocatorImpl(webDriver, page, this.selector + " " + selectorOrLocator); // ToDo: Check this!
    }

    @Override
    public Locator locator(Locator selectorOrLocator, LocatorOptions options) {
        return null; // ToDo: Implement
    }

    @Override
    public Locator nth(int index) {
        return new LocatorImpl(webDriver, page, this.selector + ":nth-of-type(" + index + ")"); // ToDo: Check this!
    }

    @Override
    public Locator or(Locator locator) {
        if (!(locator instanceof LocatorImpl)) {
            throw new IllegalArgumentException("Locator must be of type LocatorImpl.");
        }
        return new LocatorImpl(webDriver, page, this.selector + ", " + ((LocatorImpl) locator).selector); // ToDo: Check this!
    }

    @Override
    public Page page() {
        return page;
    }

    @Override
    public void press(String key, PressOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        List<WDLocalValue> args = Collections.singletonList(new WDPrimitiveProtocolValue.StringValue(key));
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.PRESS_KEY,
                args
        );
    }

    @Override
    public void pressSequentially(String text, PressSequentiallyOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        for (char c : text.toCharArray()) {
            List<WDLocalValue> args = Collections.singletonList(new WDPrimitiveProtocolValue.StringValue(String.valueOf(c)));
            page.getBrowser().getScriptManager().executeDomAction(
                    new WDTarget.ContextTarget(page.getBrowsingContext()),
                    elementHandle.getRemoteReference(),
                    WDScriptManager.DomAction.PRESS_KEY,
                    args
            );
        }
    }

    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        // ToDo: Use options
        // Set the options
        WDBrowsingContext context = new WDBrowsingContext(page.getBrowsingContextId());
        CaptureScreenshotParameters.ImageFormat format = new CaptureScreenshotParameters.ImageFormat( "png");

        // Also capture if the element is not is the viewport ? // ToDo: Alternatively scroll into view
        CaptureScreenshotParameters.Origin origin = CaptureScreenshotParameters.Origin.DOCUMENT;

        // Falls das Element außerhalb des Viewports liegt, erst scrollen:
        scrollIntoViewIfNeeded(null);

        // Only capture the element:
        WDRemoteReference.SharedReference sharedReference = (WDRemoteReference.SharedReference) this.elementHandle.getRemoteReference();
        CaptureScreenshotParameters.ClipRectangle clip = new CaptureScreenshotParameters.ClipRectangle.ElementClipRectangle(sharedReference);

        WDBrowsingContextResult.CaptureScreenshotResult captureScreenshotResult =
                page.getBrowser().getWebDriver().browsingContext().captureScreenshot(page.getBrowsingContext(), origin, format, clip);
        String base64Image = captureScreenshotResult.getData();
        return Base64.getDecoder().decode(base64Image);
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.SCROLL_INTO_VIEW
        );
    }

    /**
     * Selects option or options in {@code <select>}.
     *
     * <p> <strong>Details</strong>
     *
     * <p> This method waits for <a href="https://playwright.dev/java/docs/actionability">actionability</a> checks, waits until all
     * specified options are present in the {@code <select>} element and selects these options.
     *
     * <p> If the target element is not a {@code <select>} element, this method throws an error. However, if the element is inside
     * the {@code <label>} element that has an associated <a
     * href="https://developer.mozilla.org/en-US/docs/Web/API/HTMLLabelElement/control">control</a>, the control will be used
     * instead.
     *
     * <p> Returns the array of option values that have been successfully selected.
     *
     * <p> Triggers a {@code change} and {@code input} event once all the provided options have been selected.
     *
     * <p> <strong>Usage</strong>
     * <pre>{@code
     * // single selection matching the value or label
     * element.selectOption("blue");
     * // single selection matching the label
     * element.selectOption(new SelectOption().setLabel("Blue"));
     * // multiple selection for blue, red and second option
     * element.selectOption(new String[] {"red", "green", "blue"});
     * }</pre>
     *
     * @param values Options to select. If the {@code <select>} has the {@code multiple} attribute, all matching options are selected,
     * otherwise only the first option matching one of the passed options is selected. String values are matching both values
     * and labels. Option is considered matching if all specified properties match.
     * @since v1.14
     */
    @Override
    public List<String> selectOption(String values, SelectOptionOptions options) {
        // ToDo: Use Options
        // ToDo: How values can be an array and a string at the same time?
        // ToDo: Make sure only successful values are returned
        // ToDo: Make sure an error is thrown if the element is not a select element
        resolveElementHandle();
        List<WDLocalValue> args = Collections.singletonList(new WDPrimitiveProtocolValue.StringValue(values));
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.SELECT,
                args
        );
        return Collections.singletonList(values); // Proposed an error is thrown otherwise by webdriver
    }

    @Override
    public List<String> selectOption(ElementHandle values, SelectOptionOptions options) {
        return Collections.emptyList(); // ToDo: Implement
    }

    @Override
    public List<String> selectOption(String[] values, SelectOptionOptions options) {
        resolveElementHandle();

        List<WDLocalValue> args = new ArrayList<>();
        for (String value : values) {
            args.add(new WDPrimitiveProtocolValue.StringValue(value));
        }

        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.SELECT, // ToDo: Will it work with multiple values?
                args
        );

        return Arrays.asList(values);
    }

    @Override
    public List<String> selectOption(SelectOption values, SelectOptionOptions options) {
        return Collections.emptyList(); // ToDo: Implement
    }

    @Override
    public List<String> selectOption(ElementHandle[] values, SelectOptionOptions options) {
        return Collections.emptyList(); // ToDo: Implement
    }

    @Override
    public List<String> selectOption(SelectOption[] values, SelectOptionOptions options) {
        return Collections.emptyList(); // ToDo: Implement
    }

    @Override
    public void selectText(SelectTextOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.SELECT_TEXT
        );
    }

    @Override
    public void setChecked(boolean checked, SetCheckedOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDScriptManager.DomAction action = checked ? WDScriptManager.DomAction.CHECK : WDScriptManager.DomAction.UNCHECK;
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                action
        );
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
        // ToDo: Use Options
        resolveElementHandle();
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.TAP
        );
    }

    @Override
    public String textContent(TextContentOptions options) {
        // ToDo: Use Options
        resolveElementHandle();
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomQuery.GET_TEXT_CONTENT
        );
        return getStringFromEvaluateResult(result);
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
        resolveElementHandle();
        List<WDLocalValue> args = Collections.singletonList(new WDPrimitiveProtocolValue.StringValue(text));
        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.INPUT,
                args
        );
    }

    @Override
    public void uncheck(UncheckOptions options) {
        setChecked(false, null);
    }

    @Override
    public void waitFor(WaitForOptions options) {
        // ToDo: Use Options

        // ToDo: Implement... use the State & Timeout from the options
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Deprecated // since WebDriver directly accepts the BrowsingContext and looks up the Window Realm on its own
    public WDTarget.RealmTarget getRealmTarget(WDRealmType realmType) {
        WDScriptResult.GetRealmsResult realmsResult = webDriver.script().getRealms(); // Hol alle existierenden Realms

        // Filter for RealmType.WINDOW
        WDRealm realm = realmsResult.getRealms().stream()
                .filter(r -> r.getType() == WDRealmType.WINDOW)
                .findFirst()
                .map(WDRealmInfo::getRealm)
                .orElse(null);

        if(realm == null) {
            throw new RuntimeException("No realm found for type: " + realmType);
        }
        return new WDTarget.RealmTarget(realm);
    }

}

package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.command.request.parameters.browsingContext.CaptureScreenshotParameters;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.support.ActionabilityCheck;
import de.bund.zrb.support.ActionabilityRequirement;
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

    private FilterOptions filterOptions; // optional

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
        if (elementHandle != null) {
            return;
        }

        WDLocator<?> locator = createWDLocator(selector);

        // Schritt 1: Hole alle passenden Nodes
        WDBrowsingContextResult.LocateNodesResult locateNodesResult =
                page.getBrowser().getWebDriver().browsingContext().locateNodes(
                        page.getBrowsingContextId(),
                        locator,
                        Integer.MAX_VALUE
                );

        List<WDRemoteValue.NodeRemoteValue> nodes = locateNodesResult.getNodes();

        // Schritt 2: Falls Filter gesetzt, filtere Nodes
        if (filterOptions != null) {
            nodes = applyFilter(nodes);
        }

        if (nodes.isEmpty()) {
            throw new RuntimeException("No nodes found for selector: " + selector);
        }

        WDRemoteValue.NodeRemoteValue node = nodes.get(0);
        WDHandle handle = null;
        if (node.getHandle() != null) {
            handle = new WDHandle(node.getHandle().value());
        }
        WDSharedId sharedId = node.getSharedId();

        if (sharedId != null) {
            WDRemoteReference.SharedReference reference = new WDRemoteReference.SharedReference(sharedId, handle);
            elementHandle = new ElementHandleImpl(page.getWebDriver(), reference, new WDTarget.ContextTarget(page.getBrowsingContext()));
        } else if (handle != null) {
            WDRemoteReference.RemoteObjectReference reference = new WDRemoteReference.RemoteObjectReference(handle, sharedId);
            new JSHandleImpl(page.getWebDriver(), reference, new WDTarget.ContextTarget(page.getBrowsingContext())); // ToDo
            throw new RuntimeException("No sharedId found for selector: " + selector + " handle: " + handle);
        } else {
            throw new RuntimeException("No handle found for selector: " + selector);
        }
    }

    public static WDLocator<?> createWDLocator(String selector) {
        if (selector.startsWith("/") || selector.startsWith("(")) {
            return new WDLocator.XPathLocator(selector);
        } else if (selector.startsWith("text=")) {
            return new WDLocator.InnerTextLocator(selector.substring(5));
        } else if (selector.startsWith("aria=")) {
            String rest = selector.substring(5).trim();
            String role = null;
            String name = null;

            // Beispiel: aria=button[name="Senden"]
            int nameStart = rest.indexOf("[name=");
            if (nameStart >= 0) {
                role = rest.substring(0, nameStart).trim();
                String namePart = rest.substring(nameStart);
                name = parseNameFromBrackets(namePart);
            } else if (rest.startsWith("[")) {
                // Beispiel: aria=[name="Senden"]
                name = parseNameFromBrackets(rest);
            } else {
                // Nur Rolle
                role = rest.isEmpty() ? null : rest;
            }

            WDLocator.AccessibilityLocator.Value value = new WDLocator.AccessibilityLocator.Value(name, role);
            return new WDLocator.AccessibilityLocator(value);
        } else {
            return new WDLocator.CssLocator(selector);
        }
    }

    private static String parseNameFromBrackets(String bracketPart) {
        // Erwartet: [name="Senden"]
        int eq = bracketPart.indexOf('=');
        int quote1 = bracketPart.indexOf('"', eq);
        int quote2 = bracketPart.indexOf('"', quote1 + 1);
        if (eq >= 0 && quote1 >= 0 && quote2 >= 0) {
            return bracketPart.substring(quote1 + 1, quote2);
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Overridden Methods / Implementation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<Locator> all() {
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), locator, Integer.MAX_VALUE);

        List<Locator> locators = new ArrayList<>();
        int index = 1;
        for (WDRemoteValue.NodeRemoteValue ignored : nodes.getNodes()) {
            String nthSelector = String.format("%s >> nth=%d", selector, index - 1);
            locators.add(new LocatorImpl(webDriver, page, nthSelector));
            index++;
        }
        return locators;
    }

    @Override
    public List<String> allInnerTexts() {
        WDLocator<?> locator = createWDLocator(selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver().browsingContext().locateNodes(
                page.getBrowsingContextId(),
                locator,
                Integer.MAX_VALUE
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
                Integer.MAX_VALUE
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
            throw new IllegalArgumentException("Locator must be LocatorImpl");
        }
        LocatorImpl other = (LocatorImpl) locator;

        // 1. Hole Nodes für beide Selektoren
        WDBrowsingContextResult.LocateNodesResult nodesThis = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), createWDLocator(this.selector));
        WDBrowsingContextResult.LocateNodesResult nodesOther = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), createWDLocator(other.selector));

        // 2. Schnittmenge bilden (z.B. via Handle-Id)
        List<WDRemoteValue.NodeRemoteValue> both = new ArrayList<>();
        for (WDRemoteValue.NodeRemoteValue n1 : nodesThis.getNodes()) {
            for (WDRemoteValue.NodeRemoteValue n2 : nodesOther.getNodes()) {
                if (n1.getHandle().equals(n2.getHandle())) {
                    both.add(n1);
                }
            }
        }

        if (both.isEmpty()) {
            throw new RuntimeException("No nodes matched both conditions.");
        }

        // 3. Gib neuen Locator zurück (für Einfachheit: ersten Node)
        WDHandle h = new WDHandle(both.get(0).getHandle().value());
        WDSharedId sid = both.get(0).getSharedId();
        WDRemoteReference.SharedReference ref = new WDRemoteReference.SharedReference(sid, h);
        ElementHandleImpl newHandle = new ElementHandleImpl(webDriver, ref, new WDTarget.ContextTarget(page.getBrowsingContext()));

        LocatorImpl result = new LocatorImpl(webDriver, page, this.selector + " + " + other.selector);
        result.elementHandle = newHandle;
        return result;
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
                page.getBrowsingContextId(), locator);
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
        resolveElementHandle();

        // 1️⃣ Timeout:
        double timeout = ((UserContextImpl) page.context()).getDefaultTimeout();
        if (options != null && options.timeout != null) {
            timeout = options.timeout;
        }

        // 2️⃣ Force:
        boolean force = options != null && Boolean.TRUE.equals(options.force);

        // 3️⃣ Actionability Check, nur wenn !force
        if (!force) {
            waitForActionability(ActionabilityCheck.FILL, timeout);
        }

        // 4️⃣ Jetzt wirklich ausführen:
        List<WDLocalValue> args = new ArrayList<>();
        args.add(new WDPrimitiveProtocolValue.StringValue(value));

        page.getBrowser().getScriptManager().executeDomAction(
                new WDTarget.ContextTarget(page.getBrowsingContext()),
                elementHandle.getRemoteReference(),
                WDScriptManager.DomAction.INPUT,
                args
        );

        // 5️⃣ NoWaitAfter – könnte hier steuern, ob du `waitForNavigation` o.ä. weglässt.
        if (options != null && Boolean.TRUE.equals(options.noWaitAfter)) {
            // Zum Beispiel keine Navigation/Netzwerk-Warte auslösen.
        }
    }

    @Override
    public Locator filter(FilterOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("FilterOptions must not be null");
        }

        LocatorImpl filtered = new LocatorImpl(webDriver, page, this.selector);

        // FilterInfo speichert die Bedingungen
        filtered.filterOptions = options;

        return filtered;
    }


    @Override
    public Locator first() {
        return this.nth(0);
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
        // Du kannst auch mit locateNodes() ermitteln:
        int count = this.count();
        return this.nth(count - 1);
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
            throw new IllegalArgumentException("Locator must be LocatorImpl.");
        }
        LocatorImpl other = (LocatorImpl) locator;

        // 1️⃣ Hole beide Node-Mengen
        WDBrowsingContextResult.LocateNodesResult nodesThis = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), createWDLocator(this.selector));
        WDBrowsingContextResult.LocateNodesResult nodesOther = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), createWDLocator(other.selector));

        // 2️⃣ Kombiniere ohne Duplikate
        List<WDRemoteValue.NodeRemoteValue> union = new ArrayList<>(nodesThis.getNodes());
        for (WDRemoteValue.NodeRemoteValue n : nodesOther.getNodes()) {
            boolean already = false;
            for (WDRemoteValue.NodeRemoteValue u : union) {
                if (n.getHandle().equals(u.getHandle())) {
                    already = true;
                    break;
                }
            }
            if (!already) {
                union.add(n);
            }
        }

        if (union.isEmpty()) {
            throw new RuntimeException("No nodes matched either condition.");
        }

        // 3️⃣ Liefere Union als eigenen Locator
        // Falls du mehrere brauchst: gib Liste oder neuen Multi-Locator
        // Für einfaches Beispiel: nur erster Node
        WDHandle handle = new WDHandle(union.get(0).getHandle().value());
        WDSharedId sharedId = union.get(0).getSharedId();
        WDRemoteReference.SharedReference reference = new WDRemoteReference.SharedReference(sharedId, handle);
        ElementHandleImpl newHandle = new ElementHandleImpl(webDriver, reference, new WDTarget.ContextTarget(page.getBrowsingContext()));

        LocatorImpl result = new LocatorImpl(webDriver, page, this.selector + " | " + other.selector);
        result.elementHandle = newHandle;
        return result;
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
        resolveElementHandle();

        WaitForSelectorState state = options != null && options.state != null
                ? options.state
                : WaitForSelectorState.VISIBLE;
        double timeout = options != null && options.timeout != null
                ? options.timeout
                : 30000;

        long start = System.currentTimeMillis();

        while (true) {
            boolean success = false;

            switch (state) {
                case ATTACHED:
                    success = elementHandle != null;
                    break;

                case DETACHED:
                    success = !(Boolean) elementHandle.evaluate(
                            "(element) => element.isConnected"
                    );
                    break;

                case VISIBLE:
                    Object result = elementHandle.evaluate("function() { const rect = this.getBoundingClientRect(); " +
                            "return !!(rect.width && rect.height) && window.getComputedStyle(this).visibility !== 'hidden'; }");

                    if (result instanceof WDPrimitiveProtocolValue.BooleanValue) {
                        success = ((WDPrimitiveProtocolValue.BooleanValue) result).getValue(); // oder .value, je nach Feldnamen
                    } else {
                        throw new IllegalStateException("Expected boolean result but got: " + result);
                    }
                    break;

                case HIDDEN:
                    Object hiddenResult = elementHandle.evaluate(
                            "function() { " +
                                    "const rect = this.getBoundingClientRect(); " +
                                    "return rect.width === 0 || rect.height === 0 || window.getComputedStyle(this).visibility === 'hidden'; " +
                                    "}"
                    );

                    if (hiddenResult instanceof WDPrimitiveProtocolValue.BooleanValue) {
                        success = ((WDPrimitiveProtocolValue.BooleanValue) hiddenResult).getValue();
                    } else {
                        throw new IllegalStateException("Expected boolean result but got: " + hiddenResult);
                    }
                    break;
            }

            if (success) {
                return;
            }

            if ((System.currentTimeMillis() - start) > timeout) {
                throw new RuntimeException("Timeout waiting for state: " + state);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void waitForActionability(ActionabilityCheck check, double timeout) {
        resolveElementHandle();

        EnumSet<ActionabilityRequirement> requirements = check.getRequirements();

        // 1️⃣ Sichtbarkeit prüfen, falls gefordert
        if (requirements.contains(ActionabilityRequirement.VISIBLE)) {
            waitFor(new WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeout));
        }

        // 2️⃣ Stabilität prüfen, falls gefordert
        if (requirements.contains(ActionabilityRequirement.STABLE)) {
            waitForStable(timeout);
        }

        // 3️⃣ Events erreichbar? enabled? editable? → per JS prüfen
        if (requirements.contains(ActionabilityRequirement.ENABLED)) {
            boolean enabled = (Boolean) page.evaluate("el => !el.disabled", elementHandle);
            if (!enabled) throw new RuntimeException("Element is disabled!");
        }

        if (requirements.contains(ActionabilityRequirement.EDITABLE)) {
            boolean editable = (Boolean) page.evaluate(
                    "el => el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || el.isContentEditable",
                    elementHandle);
            if (!editable) throw new RuntimeException("Element is not editable!");
        }

        if (requirements.contains(ActionabilityRequirement.RECEIVES_EVENTS)) {
            boolean receivesEvents = (Boolean) page.evaluate(
                    "el => { " +
                            "const rect = el.getBoundingClientRect();" +
                            "return rect.width > 0 && rect.height > 0;" +
                            "}",
                    elementHandle);
            if (!receivesEvents) throw new RuntimeException("Element does not receive events!");
        }
    }

    private void waitForStable(double timeout) {
        long start = System.currentTimeMillis();
        double[] lastRect = getBoundingBox();

        while (true) {
            double[] rect = getBoundingBox();
            if (Arrays.equals(rect, lastRect)) {
                return;
            }
            lastRect = rect;

            if ((System.currentTimeMillis() - start) > timeout) {
                throw new RuntimeException("Timeout waiting for stability");
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private double[] getBoundingBox() {
        Map<String, Object> box = (Map<String, Object>) page.evaluate(
                "el => { const r = el.getBoundingClientRect(); return {x: r.x, y: r.y, w: r.width, h: r.height}; }",
                elementHandle);
        return new double[] {
                (double) box.get("x"),
                (double) box.get("y"),
                (double) box.get("w"),
                (double) box.get("h")
        };
    }

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

    private List<WDRemoteValue.NodeRemoteValue> applyFilter(List<WDRemoteValue.NodeRemoteValue> nodes) {
        List<WDRemoteValue.NodeRemoteValue> result = new ArrayList<>();

        for (WDRemoteValue.NodeRemoteValue node : nodes) {
            boolean include = true;

            // Filter: hasText
            if (filterOptions.hasText != null) {
                String innerText = evaluateInnerText(node);
                if (filterOptions.hasText instanceof String) {
                    include = innerText.toLowerCase().contains(((String) filterOptions.hasText).toLowerCase());
                } else if (filterOptions.hasText instanceof Pattern) {
                    include = ((Pattern) filterOptions.hasText).matcher(innerText).find();
                }
            }

            // Filter: hasNotText
            if (filterOptions.hasNotText != null) {
                String innerText = evaluateInnerText(node);
                boolean hasNot = false;
                if (filterOptions.hasNotText instanceof String) {
                    hasNot = innerText.toLowerCase().contains(((String) filterOptions.hasNotText).toLowerCase());
                } else if (filterOptions.hasNotText instanceof Pattern) {
                    hasNot = ((Pattern) filterOptions.hasNotText).matcher(innerText).find();
                }
                if (hasNot) {
                    include = false;
                }
            }

            // Filter: has
            if (filterOptions.has != null && filterOptions.has instanceof LocatorImpl) {
                LocatorImpl inner = (LocatorImpl) filterOptions.has;
                List<WDRemoteValue.NodeRemoteValue> innerNodes = inner.locateNodesInSubtree(node);
                if (innerNodes.isEmpty()) {
                    include = false;
                }
            }

            // Filter: hasNot
            if (filterOptions.hasNot != null && filterOptions.hasNot instanceof LocatorImpl) {
                LocatorImpl inner = (LocatorImpl) filterOptions.hasNot;
                List<WDRemoteValue.NodeRemoteValue> innerNodes = inner.locateNodesInSubtree(node);
                if (!innerNodes.isEmpty()) {
                    include = false;
                }
            }

            if (include) {
                result.add(node);
            }
        }

        return result;
    }

    private String evaluateInnerText(WDRemoteValue.NodeRemoteValue node) {
        WDEvaluateResult result = page.getBrowser().getScriptManager().queryDomProperty(
                page.getBrowsingContextId(),
                node.getSharedId().value(),
                WDScriptManager.DomQuery.GET_INNER_TEXT
        );
        return getStringFromEvaluateResult(result);
    }

    private List<WDRemoteValue.NodeRemoteValue> locateNodesInSubtree(WDRemoteValue.NodeRemoteValue parent) {
        WDLocator<?> locator = createWDLocator(this.selector);

        List<WDRemoteReference.SharedReference> startNodes = Collections.singletonList(parent.getSharedIdReference());

        WDBrowsingContextResult.LocateNodesResult result = page.getBrowser().getWebDriver()
                .browsingContext()
                .locateNodes(
                        page.getBrowsingContext(),
                        locator,
                        null, // maxNodeCount
                        null, // WDSerializationOptions
                        startNodes // <-- Subtree-Start!
                );

        return result.getNodes();
    }







}

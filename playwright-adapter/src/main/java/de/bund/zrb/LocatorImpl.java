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
import de.bund.zrb.util.AdapterLocatorFactory;
import de.bund.zrb.util.LocatorType;
import de.bund.zrb.util.WebDriverUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static de.bund.zrb.support.WDRemoteValueUtil.*;

public class LocatorImpl implements Locator {
    private final WebDriver webDriver;

    private final PageImpl page;
    private final String selector;
    private final LocatorType explicitType;

    private ElementHandleImpl elementHandle; // Wird erst beim ersten Zugriff gesetzt

    private FilterOptions filterOptions; // optional

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public LocatorImpl(WebDriver webDriver, PageImpl page, LocatorType type, String selector) {
        if (webDriver == null) throw new IllegalArgumentException("WebDriver must not be null.");
        if (page == null) throw new IllegalArgumentException("Page must not be null.");
        if (selector == null || selector.isEmpty()) throw new IllegalArgumentException("Selector must not be null or empty.");
        this.webDriver = webDriver;
        this.page = page;
        this.selector = selector;
        this.explicitType = type;
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

        // Build WDLocator using explicit type if present (fallback to heuristic otherwise)
        WDLocator<?> locator = AdapterLocatorFactory.create(explicitType, selector);

        WDBrowsingContextResult.LocateNodesResult locateNodesResult =
                page.getBrowser().getWebDriver().browsingContext().locateNodes(
                        page.getBrowsingContextId(),
                        locator,
                        Integer.MAX_VALUE
                );

        java.util.List<WDRemoteValue.NodeRemoteValue> nodes = locateNodesResult.getNodes();

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Overridden Methods / Implementation
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public java.util.List<Locator> all() {
        WDLocator<?> locator = AdapterLocatorFactory.create(explicitType, selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), locator, Integer.MAX_VALUE);

        java.util.List<Locator> locators = new java.util.ArrayList<Locator>();
        for (WDRemoteValue.NodeRemoteValue n : nodes.getNodes()) {
            WDRemoteReference.SharedReference ref = new WDRemoteReference.SharedReference(n.getSharedId(), new WDHandle(n.getHandle().value()));
            ElementHandleImpl eh = new ElementHandleImpl(webDriver, ref, new WDTarget.ContextTarget(page.getBrowsingContext()));

            // Build a child Locator that already points to this handle
            LocatorImpl li = new LocatorImpl(webDriver, page, explicitType, selector);
            li.elementHandle = eh;
            locators.add(li);
        }
        return locators;
    }

    @Override
    public java.util.List<String> allInnerTexts() {
        WDLocator<?> locator = AdapterLocatorFactory.create(explicitType, selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), locator, Integer.MAX_VALUE);

        java.util.List<String> innerTexts = new java.util.ArrayList<String>();
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
    public java.util.List<String> allTextContents() {
        WDLocator<?> locator = AdapterLocatorFactory.create(explicitType, selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), locator, Integer.MAX_VALUE);

        java.util.List<String> textContents = new java.util.ArrayList<String>();
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

        WDBrowsingContextResult.LocateNodesResult nodesThis = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), AdapterLocatorFactory.create(this.explicitType, this.selector));
        WDBrowsingContextResult.LocateNodesResult nodesOther = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), AdapterLocatorFactory.create(other.explicitType, other.selector));

        java.util.List<WDRemoteValue.NodeRemoteValue> both = new java.util.ArrayList<WDRemoteValue.NodeRemoteValue>();
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

        WDHandle h = new WDHandle(both.get(0).getHandle().value());
        WDSharedId sid = both.get(0).getSharedId();
        WDRemoteReference.SharedReference ref = new WDRemoteReference.SharedReference(sid, h);
        ElementHandleImpl newHandle = new ElementHandleImpl(webDriver, ref, new WDTarget.ContextTarget(page.getBrowsingContext()));

        LocatorImpl result = new LocatorImpl(webDriver, page, this.explicitType, this.selector + " + " + ((LocatorImpl) locator).selector);
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
        resolveElementHandle();

        double timeout =
                (options != null && options.timeout != null)
                        ? options.timeout
                        : ((UserContextImpl) page.context()).getDefaultTimeout();

        long start = System.currentTimeMillis();

        while (true) {
            // Versuch 1: schnelle Rückgabe, wenn sichtbar
            com.microsoft.playwright.options.BoundingBox bb = elementHandle.boundingBox();
            if (bb != null && bb.width > 0 && bb.height > 0) return bb;

            // optional: kurze Sichtbarkeits-/Stabilitäts-Warte
            try {
                elementHandle.scrollIntoViewIfNeeded(new ElementHandle.ScrollIntoViewIfNeededOptions().setTimeout(250));
            } catch (RuntimeException ignore) {
                // falls detached/unsichtbar, einfach weiterpolling
            }

            // Retry / Timeout
            if ((System.currentTimeMillis() - start) > timeout) {
                // Playwright-ähnliches Verhalten: null zurückgeben, wenn nicht sichtbar/kein BBox
                return elementHandle.boundingBox();
            }

            try { Thread.sleep(100); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
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
        resolveElementHandle();
        elementHandle.click(toHandleClickOptions(options));
    }

    private static ElementHandle.ClickOptions toHandleClickOptions(Locator.ClickOptions src) {
        ElementHandle.ClickOptions dst = new ElementHandle.ClickOptions();
        if (src == null) return dst;
        if (src.force != null)      dst.setForce(src.force);
        if (src.noWaitAfter != null) dst.setNoWaitAfter(src.noWaitAfter); // no-op ok
        if (src.timeout != null)    dst.setTimeout(src.timeout);
        return dst;
    }

    @Override
    public int count() {
        WDLocator<?> locator = AdapterLocatorFactory.create(explicitType, selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), locator);
        return nodes.getNodes().size();
    }

    @Override
    public void dblclick(DblclickOptions options) {
        resolveElementHandle();
        elementHandle.dblclick(toHandleDblclickOptions(options));
    }

    private static ElementHandle.DblclickOptions toHandleDblclickOptions(Locator.DblclickOptions src) {
        ElementHandle.DblclickOptions dst = new ElementHandle.DblclickOptions();
        if (src == null) return dst;
        if (src.force != null)      dst.setForce(src.force);
        if (src.noWaitAfter != null) dst.setNoWaitAfter(src.noWaitAfter);
        if (src.timeout != null)    dst.setTimeout(src.timeout);
        return dst;
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
    public void fill(String value, Locator.FillOptions options) {
        resolveElementHandle();
        elementHandle.fill(value, toHandleFillOptions(options));
    }

    @Override
    public Locator filter(FilterOptions options) {
        if (options == null) throw new IllegalArgumentException("FilterOptions must not be null");
        LocatorImpl filtered = new LocatorImpl(webDriver, page, this.explicitType, this.selector);
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
        return new LocatorImpl(webDriver, page, LocatorType.CSS, "[alt='" + cssEsc(text) + "']");
    }

    @Override
    public Locator getByAltText(Pattern text, GetByAltTextOptions options) {
        String xp = "//*[@alt and contains(normalize-space(@alt)," + xpLit(text.pattern()) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByLabel(String text, GetByLabelOptions options) {
        String lit = xpLit(text);
        String xp = "//*[@id=//label[contains(normalize-space(string(.))," + lit + ")]/@for]"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//input"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//textarea"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//select";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByLabel(Pattern text, GetByLabelOptions options) {
        String lit = xpLit(text.pattern());
        String xp = "//*[@id=//label[contains(normalize-space(string(.))," + lit + ")]/@for]"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//input"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//textarea"
                + " | //label[contains(normalize-space(string(.))," + lit + ")]//select";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) {
        String xp = "//*[@placeholder and contains(normalize-space(@placeholder)," + xpLit(text) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByPlaceholder(Pattern text, GetByPlaceholderOptions options) {
        String xp = "//*[@placeholder and contains(normalize-space(@placeholder)," + xpLit(text.pattern()) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByRole(AriaRole role, GetByRoleOptions options) {
        String roleString = role.name().toLowerCase();
        String xp = "//*[@role='" + roleString + "']";
        if (options != null && options.name != null) {
            xp += "[contains(normalize-space(string(.))," + xpLit(options.name.toString()) + ")]";
        }
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByTestId(String testId) {
        // gängig: data-testid
        return new LocatorImpl(webDriver, page, LocatorType.CSS, "[data-testid='" + cssEsc(testId) + "']");
    }

    @Override
    public Locator getByTestId(Pattern testId) {
        // Fallback: contains()
        String xp = "//*[@data-testid and contains(@data-testid," + xpLit(testId.pattern()) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByText(String text, GetByTextOptions options) {
        String xp = "//*[contains(normalize-space(string(.))," + xpLit(text) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByText(Pattern text, GetByTextOptions options) {
        String xp = "//*[contains(normalize-space(string(.))," + xpLit(text.pattern()) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByTitle(String text, GetByTitleOptions options) {
        String xp = "//*[@title and contains(normalize-space(@title)," + xpLit(text) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByTitle(Pattern text, GetByTitleOptions options) {
        String xp = "//*[@title and contains(normalize-space(@title)," + xpLit(text.pattern()) + ")]";
        return new LocatorImpl(webDriver, page, LocatorType.XPATH, xp);
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
        resolveElementHandle();
        elementHandle.hover(toHandleHoverOptions(options));
    }

    private static ElementHandle.HoverOptions toHandleHoverOptions(Locator.HoverOptions src) {
        ElementHandle.HoverOptions dst = new ElementHandle.HoverOptions();
        if (src == null) return dst;
        if (src.force != null)      dst.setForce(src.force);
        if (src.timeout != null)    dst.setTimeout(src.timeout);
        return dst;
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
        if (explicitType != null && explicitType != LocatorType.CSS) {
            throw new UnsupportedOperationException("Chained locator currently only supported for CSS base selectors.");
        }
        return new LocatorImpl(webDriver, page, LocatorType.CSS, this.selector + " " + selectorOrLocator);
    }

    @Override
    public Locator locator(Locator selectorOrLocator, LocatorOptions options) {
        return null; // ToDo: Implement
    }

    @Override
    public Locator nth(int index) {
        WDLocator<?> loc = AdapterLocatorFactory.create(explicitType, selector);
        WDBrowsingContextResult.LocateNodesResult nodes = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), loc, Integer.MAX_VALUE);

        List<WDRemoteValue.NodeRemoteValue> list = nodes.getNodes();
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("nth index " + index + " out of " + list.size());
        }
        WDRemoteValue.NodeRemoteValue n = list.get(index);
        WDRemoteReference.SharedReference ref =
                new WDRemoteReference.SharedReference(n.getSharedId(), new WDHandle(n.getHandle().value()));
        LocatorImpl li = new LocatorImpl(webDriver, page, explicitType, selector);
        li.elementHandle = new ElementHandleImpl(webDriver, ref, new WDTarget.ContextTarget(page.getBrowsingContext()));
        return li;
    }

    @Override
    public Locator or(Locator locator) {
        if (!(locator instanceof LocatorImpl)) {
            throw new IllegalArgumentException("Locator must be LocatorImpl.");
        }
        LocatorImpl other = (LocatorImpl) locator;

        WDBrowsingContextResult.LocateNodesResult nodesThis = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), AdapterLocatorFactory.create(this.explicitType, this.selector));
        WDBrowsingContextResult.LocateNodesResult nodesOther = page.getBrowser().getWebDriver()
                .browsingContext().locateNodes(page.getBrowsingContextId(), AdapterLocatorFactory.create(other.explicitType, other.selector));

        java.util.List<WDRemoteValue.NodeRemoteValue> union = new java.util.ArrayList<WDRemoteValue.NodeRemoteValue>(nodesThis.getNodes());
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

        WDHandle handle = new WDHandle(union.get(0).getHandle().value());
        WDSharedId sharedId = union.get(0).getSharedId();
        WDRemoteReference.SharedReference reference = new WDRemoteReference.SharedReference(sharedId, handle);
        ElementHandleImpl newHandle = new ElementHandleImpl(webDriver, reference, new WDTarget.ContextTarget(page.getBrowsingContext()));

        LocatorImpl result = new LocatorImpl(webDriver, page, this.explicitType, this.selector + " | " + other.selector);
        result.elementHandle = newHandle;
        return result;
    }

    @Override
    public Page page() {
        return page;
    }

    @Override
    public void press(String key, PressOptions options) {
        resolveElementHandle();
        elementHandle.press(key, toHandlePressOptions(options));
    }

    private static ElementHandle.PressOptions toHandlePressOptions(Locator.PressOptions src) {
        ElementHandle.PressOptions dst = new ElementHandle.PressOptions();
        if (src == null) return dst;
        if (src.delay != null)      dst.setDelay(src.delay);
        if (src.timeout != null)    dst.setTimeout(src.timeout);
        return dst;
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
        resolveElementHandle();
        elementHandle.scrollIntoViewIfNeeded(toHandleScrollOptions(options));
    }

    private static ElementHandle.ScrollIntoViewIfNeededOptions toHandleScrollOptions(
            Locator.ScrollIntoViewIfNeededOptions src) {
        ElementHandle.ScrollIntoViewIfNeededOptions dst = new ElementHandle.ScrollIntoViewIfNeededOptions();
        if (src != null && src.timeout != null) dst.setTimeout(src.timeout);
        return dst;
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
        resolveElementHandle();
        elementHandle.type(text, toHandleTypeOptions(options));
    }

    private static ElementHandle.TypeOptions toHandleTypeOptions(Locator.TypeOptions src) {
        ElementHandle.TypeOptions dst = new ElementHandle.TypeOptions();
        if (src == null) return dst;
        if (src.delay != null)      dst.setDelay(src.delay);
        return dst;
    }

    @Override
    public void uncheck(UncheckOptions options) {
        setChecked(false, null);
    }

    @Override
    public void waitFor(WaitForOptions options) {
        // Hole den Zielzustand und das Timeout
        WaitForSelectorState state = options != null && options.state != null
                ? options.state
                : WaitForSelectorState.VISIBLE;
        double timeout = options != null && options.timeout != null
                ? options.timeout
                : 30_000;

        long start = System.currentTimeMillis();

        // Retry-Loop mit 100ms-Polling
        while (true) {
            try {
                resolveElementHandle();
            } catch (RuntimeException e) {
                if (state == WaitForSelectorState.ATTACHED) {
                    // bei ATTACHED: Element ist einfach noch nicht da → nicht schlimm
                } else {
                    // bei anderen States: wenn resolve scheitert, kann kein Handle evaluiert werden
                    if ((System.currentTimeMillis() - start) > timeout) {
                        throw new RuntimeException("Timeout waiting for state: " + state, e);
                    }
                    sleepQuietly(100);
                    continue;
                }
            }

            boolean success = false;

            switch (state) {
                case ATTACHED:
                    success = elementHandle != null;
                    break;

                case VISIBLE:
                    success = WebDriverUtil.asBoolean(elementHandle.evaluate(
                            "function() { " +
                                    "  const rect = this.getBoundingClientRect(); " +
                                    "  return rect.width > 0 && rect.height > 0 && window.getComputedStyle(this).visibility !== 'hidden'; " +
                                    "}"
                    ));
                    break;

                case HIDDEN:
                    success = WebDriverUtil.asBoolean(elementHandle.evaluate(
                            "function() { " +
                                    "  const rect = this.getBoundingClientRect(); " +
                                    "  return rect.width === 0 || rect.height === 0 || window.getComputedStyle(this).visibility === 'hidden'; " +
                                    "}"
                    ));
                    break;

                case DETACHED:
                    success = WebDriverUtil.asBoolean(elementHandle.evaluate(
                            "function() { return !this.isConnected; }"
                    ));
                    break;
            }

            if (success) {
                // 🆕 Stability-Wait (nur bei VISIBLE sinnvoll)
                if (state == WaitForSelectorState.VISIBLE) {
                    elementHandle.evaluate(
                            "() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))"
                    );
                }
                return;
            }

            if ((System.currentTimeMillis() - start) > timeout) {
                throw new RuntimeException("Timeout waiting for state: " + state);
            }

            sleepQuietly(100);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void waitForActionability(ActionabilityCheck check, double timeout) {
        resolveElementHandle();

        EnumSet<ActionabilityRequirement> requirements = check.getRequirements();

        if (requirements.contains(ActionabilityRequirement.VISIBLE)) {
            waitFor(new WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeout));
        }

        if (requirements.contains(ActionabilityRequirement.STABLE)) {
            waitForStable(timeout);
        }

        if (requirements.contains(ActionabilityRequirement.ENABLED)) {
            boolean enabled = WebDriverUtil.asBoolean(
                    elementHandle.evaluate("function() { return !this.disabled; }")
            );
            if (!enabled) throw new RuntimeException("Element is disabled!");
        }

        if (requirements.contains(ActionabilityRequirement.EDITABLE)) {
            boolean editable = WebDriverUtil.asBoolean(
                    elementHandle.evaluate("function() { " +
                            "return this instanceof HTMLInputElement || " +
                            "this instanceof HTMLTextAreaElement || " +
                            "this.isContentEditable; }")
            );
            if (!editable) throw new RuntimeException("Element is not editable!");
        }

        if (requirements.contains(ActionabilityRequirement.RECEIVES_EVENTS)) {
            boolean receives = WebDriverUtil.asBoolean(
                    elementHandle.evaluate("function() { " +
                            "const r=this.getBoundingClientRect();" +
                            "return r.width>0 && r.height>0; }")
            );
            if (!receives) throw new RuntimeException("Element does not receive events!");
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
        Object result = elementHandle.evaluate(
                "function() { const r = this.getBoundingClientRect(); return { x: r.x, y: r.y, w: r.width, h: r.height }; }"
        );

        if (!(result instanceof WDRemoteValue.ObjectRemoteValue)) {
            throw new IllegalStateException("Expected ObjectRemoteValue, but got: " + result.getClass().getName());
        }

        Map<WDRemoteValue, WDRemoteValue> objectMap = ((WDRemoteValue.ObjectRemoteValue) result).getValue();

        return new double[] {
                extractNumber(objectMap, "x"),
                extractNumber(objectMap, "y"),
                extractNumber(objectMap, "w"),
                extractNumber(objectMap, "h")
        };
    }

    private double extractNumber(Map<WDRemoteValue, WDRemoteValue> map, String key) {
        for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : map.entrySet()) {
            WDRemoteValue k = entry.getKey();
            WDRemoteValue v = entry.getValue();

            if (k instanceof WDPrimitiveProtocolValue.StringValue) {
                String kStr = ((WDPrimitiveProtocolValue.StringValue) k).getValue();
                if (key.equals(kStr)) {
                    if (v instanceof WDPrimitiveProtocolValue.NumberValue) {
                        String raw = ((WDPrimitiveProtocolValue.NumberValue) v).getValue();
                        try {
                            return Double.parseDouble(raw);
                        } catch (NumberFormatException e) {
                            throw new IllegalStateException("Invalid number format for key '" + key + "': " + raw);
                        }
                    } else {
                        throw new IllegalStateException("Expected NumberValue for key '" + key + "', but got: " + v.getClass().getSimpleName());
                    }
                }
            }
        }

        throw new IllegalStateException("Key '" + key + "' not found in object map");
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
        WDLocator<?> locator = AdapterLocatorFactory.create(this.explicitType, this.selector);
        List<WDRemoteReference.SharedReference> startNodes = Collections.singletonList(parent.getSharedIdReference());
        WDBrowsingContextResult.LocateNodesResult result = page.getBrowser().getWebDriver()
                .browsingContext()
                .locateNodes(page.getBrowsingContext(), locator, null, null, startNodes);
        return result.getNodes();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static String xpLit(String s) {
        if (s.indexOf('\'') == -1) return "'" + s + "'";
        if (s.indexOf('"') == -1) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) sb.append(",");
            char c = s.charAt(i);
            if (c == '\'') sb.append("\"'\"");
            else sb.append("'").append(c).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
    private static String cssEsc(String s) { return s.replace("'", "\\'"); }

    // Kleiner Adapter: Locator.FillOptions -> ElementHandle.FillOptions
    private static ElementHandle.FillOptions toHandleFillOptions(Locator.FillOptions src) {
        ElementHandle.FillOptions dst = new ElementHandle.FillOptions();
        if (src == null) return dst;
        if (src.force != null)      dst.setForce(src.force);
        if (src.noWaitAfter != null) dst.setNoWaitAfter(src.noWaitAfter); // deprecated, no-op ok
        if (src.timeout != null)    dst.setTimeout(src.timeout);
        return dst;
    }

}

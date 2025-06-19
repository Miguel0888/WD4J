package de.bund.zrb.event;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.type.browser.WDClientWindow;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browsingContext.WDInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FrameImpl implements Frame {

    private String frameId;
    private PageImpl page;
    private String url;
    private boolean isDetached;
    private final List<Frame> childFrames;

    // 🔹 Konstruktor für Fragment-Navigation
    public FrameImpl(WDBrowsingContextEvent.FragmentNavigated fragmentNavigated) {
        this.frameId = fragmentNavigated.getParams().getContext().value();
        this.page = BrowserImpl.getPage(fragmentNavigated.getParams().getContext());
        this.url = fragmentNavigated.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // 🔹 Konstruktor für Abbruch einer Navigation
    public FrameImpl(WDBrowsingContextEvent.NavigationAborted navigationAborted) {
        this.frameId = navigationAborted.getParams().getContext().value();
        this.page = BrowserImpl.getPage(navigationAborted.getParams().getContext());
        this.url = navigationAborted.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // 🔹 Konstruktor für eine fehlgeschlagene Navigation
    public FrameImpl(WDBrowsingContextEvent.NavigationFailed navigationFailed) {
        this.frameId = navigationFailed.getParams().getContext().value();
        this.page = BrowserImpl.getPage(navigationFailed.getParams().getContext());
        this.url = navigationFailed.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // 🔹 Konstruktor für eine gestartete Navigation
    public FrameImpl(WDBrowsingContextEvent.NavigationStarted navigationStarted) {
        this.frameId = navigationStarted.getParams().getContext().value();
        this.page = BrowserImpl.getPage(navigationStarted.getParams().getContext());
        this.url = navigationStarted.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // 🔹 Konstruktor für eine aktualisierte History
    public FrameImpl(WDBrowsingContextEvent.HistoryUpdated historyUpdated) {
        this.frameId = historyUpdated.getParams().getContext().value();
        this.page = BrowserImpl.getPage(historyUpdated.getParams().getContext());
        this.url = historyUpdated.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // 🔹 Konstruktor für eine festgeschriebene Navigation
    public FrameImpl(WDBrowsingContextEvent.NavigationCommitted navigationCommitted) {
        this.frameId = navigationCommitted.getParams().getContext().value();
        this.page = BrowserImpl.getPage(navigationCommitted.getParams().getContext());
        this.url = navigationCommitted.getParams().getUrl();
        this.isDetached = false;
        this.childFrames = new ArrayList<>();
    }

    // ToDo: Can user context from sub frames be different from the parent frame / page?
    public FrameImpl(PageImpl page, WDUserContext userContext, WDClientWindow clientWindow, String url, Collection<WDInfo> children) {
        this.page = page;
        this.url = url;
        this.childFrames = new ArrayList<>(); // ToDo: Implementierung erforderlich
        this.isDetached = false;
    }

    @Override
    public ElementHandle addScriptTag(AddScriptTagOptions options) {
        throw new UnsupportedOperationException("addScriptTag not implemented yet.");
    }

    @Override
    public ElementHandle addStyleTag(AddStyleTagOptions options) {
        throw new UnsupportedOperationException("addStyleTag not implemented yet.");
    }

    @Override
    public void check(String selector, CheckOptions options) {

    }

    @Override
    public List<Frame> childFrames() {
        return Collections.unmodifiableList(childFrames);
    }

    @Override
    public void click(String selector, ClickOptions options) {

    }

    @Override
    public String content() {
        return "";
    }

    @Override
    public void dblclick(String selector, DblclickOptions options) {

    }

    @Override
    public void dispatchEvent(String selector, String type, Object eventInit, DispatchEventOptions options) {

    }

    @Override
    public void dragAndDrop(String source, String target, DragAndDropOptions options) {

    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg, EvalOnSelectorOptions options) {
        return null;
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        return null;
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        return null;
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        return null;
    }

    @Override
    public void fill(String selector, String value, FillOptions options) {

    }

    @Override
    public void focus(String selector, FocusOptions options) {

    }

    @Override
    public ElementHandle frameElement() {
        return null;
    }

    @Override
    public FrameLocator frameLocator(String selector) {
        return null;
    }

    @Override
    public String getAttribute(String selector, String name, GetAttributeOptions options) {
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
    public Response navigate(String url, NavigateOptions options) {
        return null;
    }

    @Override
    public void hover(String selector, HoverOptions options) {

    }

    @Override
    public String innerHTML(String selector, InnerHTMLOptions options) {
        return "";
    }

    @Override
    public String innerText(String selector, InnerTextOptions options) {
        return "";
    }

    @Override
    public String inputValue(String selector, InputValueOptions options) {
        return "";
    }

    @Override
    public boolean isChecked(String selector, IsCheckedOptions options) {
        return false;
    }

    @Override
    public boolean isDetached() {
        return isDetached;
    }

    @Override
    public boolean isDisabled(String selector, IsDisabledOptions options) {
        return false;
    }

    @Override
    public boolean isEditable(String selector, IsEditableOptions options) {
        return false;
    }

    @Override
    public boolean isEnabled(String selector, IsEnabledOptions options) {
        return false;
    }

    @Override
    public boolean isHidden(String selector, IsHiddenOptions options) {
        return false;
    }

    @Override
    public boolean isVisible(String selector, IsVisibleOptions options) {
        return false;
    }

    @Override
    public Locator locator(String selector, LocatorOptions options) {
        return null;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public Page page() {
        return this.page;
    }

    @Override
    public Frame parentFrame() {
        return null; // Implementierung erforderlich, um das übergeordnete Frame zu ermitteln.
    }

    @Override
    public void press(String selector, String key, PressOptions options) {

    }

    @Override
    public ElementHandle querySelector(String selector, QuerySelectorOptions options) {
        return null;
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, String values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, String[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public void setChecked(String selector, boolean checked, SetCheckedOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void setContent(String html, SetContentOptions options) {
        evaluate("document.documentElement.innerHTML = arguments[0];", html);
    }

    @Override
    public void setInputFiles(String selector, Path files, SetInputFilesOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void setInputFiles(String selector, Path[] files, SetInputFilesOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void setInputFiles(String selector, FilePayload files, SetInputFilesOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void setInputFiles(String selector, FilePayload[] files, SetInputFilesOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void tap(String selector, TapOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public String textContent(String selector, TextContentOptions options) {
        // ToDo: Implementierung erforderlich
        return "";
    }

    @Override
    public String title() {
        return evaluate("document.title").toString();
    }

    @Override
    public void type(String selector, String text, TypeOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public void uncheck(String selector, UncheckOptions options) {
        // ToDo: Implementierung erforderlich
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public JSHandle waitForFunction(String expression, Object arg, WaitForFunctionOptions options) {
        return evaluateHandle(expression, arg);
    }

    @Override
    public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {
        while (!evaluate("document.readyState").equals(state.name().toLowerCase())) {
            try {
                Thread.sleep(50); // Polling alle 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public Response waitForNavigation(WaitForNavigationOptions options, Runnable callback) {
        callback.run(); // ToDo: Implementierung erforderlich
        return null; // Muss später mit echter Navigationslogik ergänzt werden
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        return null; // ToDo: Implementierung erforderlich
    }

    @Override
    public void waitForTimeout(double timeout) {
        try {
            Thread.sleep((long) timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void waitForURL(String url, WaitForURLOptions options) {
        while (!this.url.equals(url)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void waitForURL(Pattern url, WaitForURLOptions options) {
        while (!url.matcher(this.url).matches()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void waitForURL(Predicate<String> url, WaitForURLOptions options) {
        while (!url.test(this.url)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    // 🔹 Zusätzliche Methoden zur Frame-Verwaltung
    public String getFrameId() {
        return frameId;
    }

    public void detach() {
        this.isDetached = true;
    }
}

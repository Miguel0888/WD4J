package wd4j.impl.playwright.event;

import wd4j.api.*;
import wd4j.api.options.AriaRole;
import wd4j.api.options.FilePayload;
import wd4j.api.options.LoadState;
import wd4j.api.options.SelectOption;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class FrameImpl implements Frame {

    public FrameImpl(WDBrowsingContextEvent wdBrowsingContextEvent) {
        // ToDo: Implement this constructor
    }

    public FrameImpl(WDBrowsingContextEvent.FragmentNavigated fragmentNavigated) {
        // ToDo: Implement this constructor
    }

    public FrameImpl(WDBrowsingContextEvent.NavigationAborted navigationAborted) {
        // ToDo: Implement this constructor
    }

    public FrameImpl(WDBrowsingContextEvent.NavigationFailed navigationFailed) {
        // ToDo: Implement this constructor
    }

    public FrameImpl(WDBrowsingContextEvent.NavigationStarted navigationStarted) {
        // ToDo: Implement this constructor
    }

    public FrameImpl(WDBrowsingContextEvent.HistoryUodated historyUodated) {
        // ToDo: Implement this constructor
    }

    @Override
    public ElementHandle addScriptTag(AddScriptTagOptions options) {
        return null;
    }

    @Override
    public ElementHandle addStyleTag(AddStyleTagOptions options) {
        return null;
    }

    @Override
    public void check(String selector, CheckOptions options) {

    }

    @Override
    public List<Frame> childFrames() {
        return Collections.emptyList();
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
        return false;
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
        return null;
    }

    @Override
    public Frame parentFrame() {
        return null;
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

    }

    @Override
    public void setContent(String html, SetContentOptions options) {

    }

    @Override
    public void setInputFiles(String selector, Path files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, Path[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload[] files, SetInputFilesOptions options) {

    }

    @Override
    public void tap(String selector, TapOptions options) {

    }

    @Override
    public String textContent(String selector, TextContentOptions options) {
        return "";
    }

    @Override
    public String title() {
        return "";
    }

    @Override
    public void type(String selector, String text, TypeOptions options) {

    }

    @Override
    public void uncheck(String selector, UncheckOptions options) {

    }

    @Override
    public String url() {
        return "";
    }

    @Override
    public JSHandle waitForFunction(String expression, Object arg, WaitForFunctionOptions options) {
        return null;
    }

    @Override
    public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {

    }

    @Override
    public Response waitForNavigation(WaitForNavigationOptions options, Runnable callback) {
        return null;
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        return null;
    }

    @Override
    public void waitForTimeout(double timeout) {

    }

    @Override
    public void waitForURL(String url, WaitForURLOptions options) {

    }

    @Override
    public void waitForURL(Pattern url, WaitForURLOptions options) {

    }

    @Override
    public void waitForURL(Predicate<String> url, WaitForURLOptions options) {

    }
}

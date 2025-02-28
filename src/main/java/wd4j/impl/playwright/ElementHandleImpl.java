package wd4j.impl.playwright;

import wd4j.api.ElementHandle;
import wd4j.api.Frame;
import wd4j.api.JSHandle;
import wd4j.api.options.*;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.webdriver.type.script.WDHandle;
import wd4j.impl.webdriver.type.script.WDRealm;
import wd4j.impl.webdriver.type.script.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ElementHandleImpl extends JSHandleImpl implements ElementHandle {
    private final WDScriptManager scriptManager;
    private final WDHandle handle;
    private final WDRealm realm;

    public ElementHandleImpl(WDHandle handle, WDRealm realm) {
        super(handle, realm);
        this.scriptManager = WDScriptManager.getInstance();
        this.handle = handle;
        this.realm = realm;
    }

    @Override
    public BoundingBox boundingBox() {
        return (BoundingBox) evaluate("el => el.getBoundingClientRect()");
    }

    @Override
    public void check(CheckOptions options) {
        evaluate("el => el.checked = true");
    }

    @Override
    public void click(ClickOptions options) {
        evaluate("el => el.click()");
    }

    @Override
    public Frame contentFrame() {
        return null; // TODO: Implement frame retrieval if needed
    }

    @Override
    public void dblclick(DblclickOptions options) {
        evaluate("el => { el.dispatchEvent(new MouseEvent('dblclick', { bubbles: true })); };");
    }

    @Override
    public void dispatchEvent(String type, Object eventInit) {
//        evaluate("(el, eventType, eventInit) => el.dispatchEvent(new Event(eventType, eventInit))", type, eventInit);
        // ToDo: Implement dispatchEvent method
    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg) {
        return evaluate("(el, selector, expr) => el.querySelector(selector).evaluate(expr)", selector, expression);
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        return evaluate("(el, selector, expr) => [...el.querySelectorAll(selector)].map(e => e.evaluate(expr))", selector, expression);
    }

    private Object evaluate(String s, String selector, String expression) {
//        return scriptManager.evaluate();
        return null; // TODO: Implement evaluate method
    }

    @Override
    public void fill(String value, FillOptions options) {
        evaluate("(el, value) => { el.value = value; el.dispatchEvent(new Event('input', { bubbles: true })); }", value);
    }

    @Override
    public void focus() {
        evaluate("el => el.focus()");
    }

    @Override
    public String getAttribute(String name) {
        return (String) evaluate("(el, name) => el.getAttribute(name)", name);
    }

    @Override
    public void hover(HoverOptions options) {
        evaluate("el => el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));");
    }

    @Override
    public String innerHTML() {
        return (String) evaluate("el => el.innerHTML");
    }

    @Override
    public String innerText() {
        return (String) evaluate("el => el.innerText");
    }

    @Override
    public String inputValue(InputValueOptions options) {
        return "";
    }

    @Override
    public boolean isChecked() {
        return (Boolean) evaluate("el => el.checked");
    }

    @Override
    public boolean isDisabled() {
        return (Boolean) evaluate("el => el.disabled");
    }

    @Override
    public boolean isEditable() {
        return (Boolean) evaluate("el => el.isContentEditable");
    }

    @Override
    public boolean isEnabled() {
        return !(Boolean) evaluate("el => el.disabled");
    }

    @Override
    public boolean isHidden() {
        return !(Boolean) evaluate("el => el.offsetWidth > 0 && el.offsetHeight > 0");
    }

    @Override
    public boolean isVisible() {
        return (Boolean) evaluate("el => el.offsetWidth > 0 && el.offsetHeight > 0");
    }

    @Override
    public Frame ownerFrame() {
        return null;
    }

    @Override
    public void press(String key, PressOptions options) {

    }

    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        // TODO: Implement screenshot functionality
        return new byte[0];
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {
        evaluate("el => el.scrollIntoView()");
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
    public ElementHandle querySelector(String selector) {
        WDEvaluateResult result = scriptManager.evaluate("el => el.querySelector(arguments[0])", new WDTarget.RealmTarget(realm), true);
        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            return new ElementHandleImpl(new WDHandle(((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult().toString()), realm);
        }
        return null;
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        WDEvaluateResult result = scriptManager.evaluate("el => Array.from(el.querySelectorAll(arguments[0]))", new WDTarget.RealmTarget(realm), true);
        if(result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            return ((List<?>) ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult()).stream()
                    .map(obj -> new ElementHandleImpl(new WDHandle(obj.toString()), realm))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String textContent() {
        return (String) evaluate("el => el.textContent");
    }

    @Override
    public void type(String text, TypeOptions options) {
        evaluate("(el, text) => { el.value = text; el.dispatchEvent(new Event('input', { bubbles: true })); }", text);
    }

    @Override
    public void uncheck(UncheckOptions options) {
        evaluate("el => el.checked = false");
    }

    @Override
    public void waitForElementState(ElementState state, WaitForElementStateOptions options) {
        // TODO: Implement waiting mechanism
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        return querySelector(selector);
    }
}

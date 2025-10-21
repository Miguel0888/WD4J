package de.bund.zrb;

import com.google.gson.JsonObject;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.options.*;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.*;
import de.bund.zrb.util.ScriptUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * ElementHandleImpl represents a DOM element handle based on a WDRemoteValue with type "node".
 */
public class ElementHandleImpl extends JSHandleImpl implements ElementHandle {

    public ElementHandleImpl(WebDriver webDriver, WDRemoteValue remoteValue, WDTarget target) {
        super(webDriver, remoteValue, target);
        if (!"node".equals(remoteValue.getType())) {
            throw new IllegalArgumentException("Expected a remote value of type 'node', but got: " + remoteValue.getType());
        }
    }

    @Override
    public ElementHandle asElement() {
        return this;
    }

    @Override
    public void click() {
        evaluate("element.click()", null);
    }

    @Override
    public void click(ClickOptions options) {
        click();
    }

    @Override
    public void hover() {
        evaluate("element.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }))", null);
    }

    @Override
    public void hover(HoverOptions options) {
        hover();
    }

    @Override
    public void dblclick() {
        evaluate("element.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }))", null);
    }

    @Override
    public void dblclick(DblclickOptions options) {
        dblclick();
    }

    @Override
    public String innerHTML() {
        return (String) evaluate("element.innerHTML", null);
    }

    @Override
    public String innerText() {
        return (String) evaluate("element.innerText", null);
    }

    @Override
    public String inputValue(InputValueOptions options) {
        return (String) evaluate("element.value", null);
    }

    @Override
    public boolean isChecked() {
        return (Boolean) evaluate("element.checked", null);
    }

    @Override
    public boolean isDisabled() {
        return (Boolean) evaluate("element.disabled", null);
    }

    @Override
    public boolean isEditable() {
        return !(Boolean) evaluate("element.readOnly || element.disabled", null);
    }

    @Override
    public boolean isEnabled() {
        return !(Boolean) evaluate("element.disabled", null);
    }

    @Override
    public boolean isHidden() {
        return !(Boolean) evaluate("!!( element.offsetWidth || element.offsetHeight || element.getClientRects().length )", null);
    }

    @Override
    public boolean isVisible() {
        return (Boolean) evaluate("!!( element.offsetWidth || element.offsetHeight || element.getClientRects().length )", null);
    }

    @Override
    public BoundingBox boundingBox() {
        JsonObject box = (JsonObject) evaluate("(() => { const rect = element.getBoundingClientRect(); return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }; })()", null);
        return new BoundingBox(box.get("x").getAsDouble(), box.get("y").getAsDouble(), box.get("width").getAsDouble(), box.get("height").getAsDouble());
    }

    @Override
    public void focus() {
        evaluate("element.focus()", null);
    }

    @Override
    public void type(String text, TypeOptions options) {
        evaluate("element.value = arguments[0]", text);
    }

    @Override
    public void press(String key, PressOptions options) {
        evaluate("element.dispatchEvent(new KeyboardEvent('keydown', { key: arguments[0] }))", key);
    }

    @Override
    public void scrollIntoViewIfNeeded() {
        evaluate("element.scrollIntoView({ block: 'center', inline: 'center', behavior: 'instant' })", null);
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {
        scrollIntoViewIfNeeded();
    }

    @Override
    public String getAttribute(String name) {
        return (String) evaluate("element.getAttribute(arguments[0])", name);
    }

    @Override
    public String textContent() {
        return (String) evaluate("element.textContent", null);
    }

    @Override
    public void dispatchEvent(String type, Object eventInit) {
        evaluate("element.dispatchEvent(new Event(arguments[0], arguments[1]))", new Object[]{type, eventInit});
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        return super.evaluateHandle(expression, arg);
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        List<WDLocalValue> args = arg != null
                ? Collections.singletonList(WDLocalValue.fromObject(arg))
                : Collections.emptyList();

        WDEvaluateResult result = ScriptUtils.evaluateDomFunction(
                webDriver.script(),
                ScriptUtils.wrapExpressionAsFunction(expression),
                target,
                ScriptUtils.sharedRef(remoteValue),
                args
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue returnValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            return super.convertRemoteValue(returnValue);
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            String message = ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails().getText();
            throw new RuntimeException("Evaluation failed: " + message);
        }

        return null;
    }


    // ---- Nicht verwendete Features ----
    @Override public void check(CheckOptions options) {}
    @Override public void uncheck(UncheckOptions options) {}
    @Override public void waitForElementState(ElementState state, WaitForElementStateOptions options) {}
    @Override public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) { return null; }
    @Override public ElementHandle querySelector(String selector) { return null; }
    @Override public List<ElementHandle> querySelectorAll(String selector) { return Collections.emptyList(); }
    @Override public byte[] screenshot(ScreenshotOptions options) { return new byte[0]; }
    @Override public void fill(String value, FillOptions options) {}
    @Override public List<String> selectOption(String values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public List<String> selectOption(ElementHandle values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public List<String> selectOption(String[] values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public List<String> selectOption(SelectOption values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public List<String> selectOption(ElementHandle[] values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public List<String> selectOption(SelectOption[] values, SelectOptionOptions options) { return Collections.emptyList(); }
    @Override public void selectText(SelectTextOptions options) {}
    @Override public void setChecked(boolean checked, SetCheckedOptions options) {}
    @Override public void setInputFiles(Path files, SetInputFilesOptions options) {}
    @Override public void setInputFiles(Path[] files, SetInputFilesOptions options) {}
    @Override public void setInputFiles(FilePayload files, SetInputFilesOptions options) {}
    @Override public void setInputFiles(FilePayload[] files, SetInputFilesOptions options) {}
    @Override public void tap(TapOptions options) {}
    @Override public Frame ownerFrame() { return null; }
    @Override public Frame contentFrame() { return null; }
    @Override public Object evalOnSelector(String selector, String expression, Object arg) { return null; }
    @Override public Object evalOnSelectorAll(String selector, String expression, Object arg) { return null; }
}

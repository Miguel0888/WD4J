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

    private final WDRemoteValue remoteValue;

    public ElementHandleImpl(WebDriver webDriver, WDRemoteValue remoteValue, WDTarget target) {
        super(webDriver, remoteValue, target);
        if (!"node".equals(remoteValue.getType())) {
            throw new IllegalArgumentException("Expected a remote value of type 'node', but got: " + remoteValue.getType());
        }
        this.remoteValue = remoteValue;
    }

    @Override
    public ElementHandle asElement() {
        return this;
    }

    @Override
    public void click() {
        evaluate("this.click()", null);
    }

    @Override
    public void click(ClickOptions options) {
        click();
    }

    @Override
    public void hover() {
        evaluate("this.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }))", null);
    }

    @Override
    public void hover(HoverOptions options) {
        hover();
    }

    @Override
    public void dblclick() {
        evaluate("this.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }))", null);
    }

    @Override
    public void dblclick(DblclickOptions options) {
        dblclick();
    }

    @Override
    public String innerHTML() {
        return (String) evaluate("this.innerHTML", null);
    }

    @Override
    public String innerText() {
        return (String) evaluate("this.innerText", null);
    }

    @Override
    public String inputValue(InputValueOptions options) {
        return (String) evaluate("this.value", null);
    }

    @Override
    public boolean isChecked() {
        return (Boolean) evaluate("this.checked", null);
    }

    @Override
    public boolean isDisabled() {
        return (Boolean) evaluate("this.disabled", null);
    }

    @Override
    public boolean isEditable() {
        return !(Boolean) evaluate("this.readOnly || this.disabled", null);
    }

    @Override
    public boolean isEnabled() {
        return !(Boolean) evaluate("this.disabled", null);
    }

    @Override
    public boolean isHidden() {
        return !(Boolean) evaluate("!!( this.offsetWidth || this.offsetHeight || this.getClientRects().length )", null);
    }

    @Override
    public boolean isVisible() {
        return (Boolean) evaluate("!!( this.offsetWidth || this.offsetHeight || this.getClientRects().length )", null);
    }

    @Override
    public BoundingBox boundingBox() {
        JsonObject box = (JsonObject) evaluate("(() => { const rect = this.getBoundingClientRect(); return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }; })()", null);
        return new BoundingBox(box.get("x").getAsDouble(), box.get("y").getAsDouble(), box.get("width").getAsDouble(), box.get("height").getAsDouble());
    }

    @Override
    public void focus() {
        evaluate("this.focus()", null);
    }

    @Override
    public void type(String text, TypeOptions options) {
        evaluate("this.value = arguments[0]", text);
    }

    @Override
    public void press(String key, PressOptions options) {
        evaluate("this.dispatchEvent(new KeyboardEvent('keydown', { key: arguments[0] }))", key);
    }

    @Override
    public void scrollIntoViewIfNeeded() {
        evaluate("this.scrollIntoView({ block: 'center', inline: 'center', behavior: 'instant' })", null);
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {
        scrollIntoViewIfNeeded();
    }

    @Override
    public String getAttribute(String name) {
        return (String) evaluate("this.getAttribute(arguments[0])", name);
    }

    @Override
    public String textContent() {
        return (String) evaluate("this.textContent", null);
    }

    @Override
    public void dispatchEvent(String type, Object eventInit) {
        evaluate("this.dispatchEvent(new Event(arguments[0], arguments[1]))", new Object[]{type, eventInit});
    }

    //ToDo: Da PlayWright hier CallFunction mit der Syntax von Evaluate meint, kommt man hier an einem Parsing des
    // Ausdrucks und einer Fallunterscheidung nicht vorbei. Daher sollte hier zukünftig RegEx verwendet werden!
    @Override
    public Object evaluate(String expression, Object arg) {
        List<WDLocalValue> arguments = arg != null
                ? Collections.singletonList(WDLocalValue.fromObject(arg))
                : Collections.emptyList();

        String trimmed = expression.trim();
        String functionDeclaration;

        if (trimmed.startsWith("function")) {
            functionDeclaration = trimmed;
        } else if (trimmed.startsWith("this.") || trimmed.contains(";") || trimmed.contains("()")) {
            // Bereits vollständiger Ausdruck, oder Mehrfachausdruck
            functionDeclaration = "function() { return " + trimmed + "; }";
        } else {
            // Einfache Property wie "value" → wird zu "this.value"
            functionDeclaration = "function() { return this." + trimmed + "; }";
        }

        WDEvaluateResult result = webDriver.script().callFunction(
                functionDeclaration,
                true,
                target,
                arguments,
                ScriptUtils.sharedRef(remoteValue),
                WDResultOwnership.NONE,
                null
        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            return ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Script evaluation failed: " + ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails());
        }

        throw new IllegalStateException("Unknown result type from script evaluation");
    }




    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        Object result = evaluate(expression, arg);
        return new JSHandleImpl(webDriver, (WDRemoteValue) result, target);
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

package wd4j.impl.playwright;

import wd4j.api.ElementHandle;
import wd4j.api.Frame;
import wd4j.api.JSHandle;
import wd4j.api.options.BoundingBox;
import wd4j.api.options.ElementState;
import wd4j.api.options.FilePayload;
import wd4j.api.options.SelectOption;
import wd4j.impl.manager.WDScriptManager;
import wd4j.impl.webdriver.type.script.WDHandle;
import wd4j.impl.webdriver.type.script.WDRealm;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ElementHandleImpl implements ElementHandle {
    public ElementHandleImpl(WDScriptManager scriptManager, WDHandle handle, WDRealm realm) {
        // ToDo: Implement
    }

    @Override
    public BoundingBox boundingBox() {
        return null;
    }

    @Override
    public void check(CheckOptions options) {

    }

    @Override
    public void click(ClickOptions options) {

    }

    @Override
    public Frame contentFrame() {
        return null;
    }

    @Override
    public void dblclick(DblclickOptions options) {

    }

    @Override
    public void dispatchEvent(String type, Object eventInit) {

    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg) {
        return null;
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        return null;
    }

    @Override
    public void fill(String value, FillOptions options) {

    }

    @Override
    public void focus() {

    }

    @Override
    public String getAttribute(String name) {
        return "";
    }

    @Override
    public void hover(HoverOptions options) {

    }

    @Override
    public String innerHTML() {
        return "";
    }

    @Override
    public String innerText() {
        return "";
    }

    @Override
    public String inputValue(InputValueOptions options) {
        return "";
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public Frame ownerFrame() {
        return null;
    }

    @Override
    public void press(String key, PressOptions options) {

    }

    @Override
    public ElementHandle querySelector(String selector) {
        return null;
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        return Collections.emptyList();
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
    public String textContent() {
        return "";
    }

    @Override
    public void type(String text, TypeOptions options) {

    }

    @Override
    public void uncheck(UncheckOptions options) {

    }

    @Override
    public void waitForElementState(ElementState state, WaitForElementStateOptions options) {

    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        return null;
    }

    @Override
    public ElementHandle asElement() {
        return null;
    }

    @Override
    public void dispose() {

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
    public Map<String, JSHandle> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public JSHandle getProperty(String propertyName) {
        return null;
    }

    @Override
    public Object jsonValue() {
        return null;
    }
}

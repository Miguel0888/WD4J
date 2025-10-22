package de.bund.zrb;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.options.*;
import de.bund.zrb.command.request.parameters.input.sourceActions.KeySourceAction;
import de.bund.zrb.command.request.parameters.input.sourceActions.PointerSourceAction;
import de.bund.zrb.command.request.parameters.input.sourceActions.SourceActions;
import de.bund.zrb.command.request.parameters.input.sourceActions.PauseAction;
import de.bund.zrb.config.InputDelaysConfig;
import de.bund.zrb.manager.WDInputManager;
import de.bund.zrb.support.ActionabilityCheck;
import de.bund.zrb.support.ActionabilityRequirement;
import de.bund.zrb.support.WDKeys;
import de.bund.zrb.type.input.WDElementOrigin;
import de.bund.zrb.type.script.*;
import de.bund.zrb.util.BrowsingContextResolver;
import de.bund.zrb.util.ScriptUtils;
import de.bund.zrb.util.WebDriverUtil;

import java.nio.file.Path;
import java.util.*;

import static de.bund.zrb.support.WDRemoteValueUtil.getBoundingBoxFromEvaluateResult;

/**
 * ElementHandleImpl implements ElementHandle on top of WebDriver BiDi.
 * Keep existing input-based interactions. Store WDRemoteValue (supertype) and bind 'this' for scripts via callFunction.
 */
public class ElementHandleImpl extends JSHandleImpl implements ElementHandle {

    private final PageImpl page; // optional
    private final WDRemoteValue remoteValue;   // supertype (must be a 'node' at runtime)
    private volatile String cachedContextId;   // lazy cache for performActions

    public ElementHandleImpl(WebDriver webDriver,
                             WDRemoteValue remoteValue,
                             WDTarget target,
                             PageImpl page) {
        // Ensure JSHandleImpl has a ctor that accepts WDRemoteValue
        super(webDriver, remoteValue, target);
        // Validate it is a node, because we need SharedId/Handle for element-origin etc.
        if (remoteValue == null || !"node".equals(remoteValue.getType())) {
            throw new IllegalArgumentException("ElementHandleImpl requires a WDRemoteValue of type 'node'. Got: " +
                    (remoteValue == null ? "null" : remoteValue.getType()));
        }
        this.page = page;
        this.remoteValue = remoteValue;
    }

    public ElementHandleImpl(WebDriver webDriver,
                             WDRemoteValue remoteValue,
                             WDTarget target) {
        this(webDriver, remoteValue, target, null);
    }

    /** Access NodeRemoteValue after validating type once. */
    private WDRemoteValue.NodeRemoteValue node() {
        return (WDRemoteValue.NodeRemoteValue) remoteValue;
    }

    /** Expose shared id for existing call sites. */
    public WDSharedId getSharedId() {
        return node().getSharedId();
    }

    /** Convert node remote value to a SharedReference for legacy APIs expecting a reference. */
    private WDRemoteReference.SharedReference asSharedRef() {
        return new WDRemoteReference.SharedReference(node().getSharedId(), node().getHandle());
    }

    /** Keep legacy getRemoteReference() but back it by the WDRemoteValue. */
    private WDRemoteReference.SharedReference getRemoteReference() {
        return asSharedRef();
    }

    // ------------------------------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------------------------------

    @Override
    public BoundingBox boundingBox() {
        WDEvaluateResult r = callExprOnThis(
                "el => { const rect = el.getBoundingClientRect(); " +
                        "return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }; }");
        return getBoundingBoxFromEvaluateResult(r);
    }

    @Override
    public void check(CheckOptions options) {
        if (!isCheckboxOrRadio()) {
            throw new IllegalStateException("Element is not a checkbox or radio input.");
        }
        if (isChecked()) {
            return;
        }
        waitForActionability(options); // correct overload for CheckOptions
        scrollIntoViewIfNeeded(null);
        click(new ClickOptions());
        if (!isChecked()) {
            throw new IllegalStateException("Element did not become checked.");
        }
    }

    @Override
    public void click(ClickOptions options) {
        // Keep input-based click flow intact.
        waitForActionability(options);
        scrollIntoViewIfNeeded(null);
        waitTwoAnimationFrames();

        double dx = 0.0, dy = 0.0;
        if (options != null && options.position != null) {
            dx = options.position.x;
            dy = options.position.y;
        }
        int button = mapMouseButton(options != null ? options.button : null);
        int clicks = (options != null && options.clickCount != null) ? options.clickCount : 1;

        WDElementOrigin origin = elementOrigin();

        List<PointerSourceAction> pointer = new ArrayList<PointerSourceAction>();
        pointer.add(new PointerSourceAction.PointerMoveAction(dx, dy, origin));
        for (int i = 0; i < clicks; i++) {
            pointer.add(new PointerSourceAction.PointerDownAction(button));
            pointer.add(new PointerSourceAction.PointerUpAction(button));
        }

        SourceActions.PointerSourceActions pointerSeq =
                new SourceActions.PointerSourceActions(
                        "mouse-pointer",
                        new SourceActions.PointerSourceActions.PointerParameters(),
                        pointer
                );

        List<SourceActions> actions = new ArrayList<SourceActions>();
        if (options != null && options.modifiers != null && !options.modifiers.isEmpty()) {
            actions.addAll(buildModifierActions(options.modifiers, true));  // keyDown
        }
        actions.add(pointerSeq);
        if (options != null && options.modifiers != null && !options.modifiers.isEmpty()) {
            actions.addAll(buildModifierActions(options.modifiers, false)); // keyUp
        }

        String contextId = requireContextId();
        input().performActions(contextId, actions);
    }

    @Override
    public Frame contentFrame() { return null; }

    @Override
    public void dblclick(DblclickOptions options) {
        ClickOptions c = new ClickOptions();
        if (options != null) {
            c.button    = options.button;
            c.position  = options.position;
            c.timeout   = options.timeout;
            c.force     = options.force;
            c.modifiers = options.modifiers;
        }
        c.clickCount = 2;
        click(c);
    }

    @Override
    public void dispatchEvent(String type, Object eventInit) {
        String fn = "function(t, init){ " +
                "  if (init && typeof init === 'object') { this.dispatchEvent(new Event(t, init)); } " +
                "  else { this.dispatchEvent(new Event(t)); } " +
                "}";
        List<WDLocalValue> args = new ArrayList<WDLocalValue>();
        args.add(new WDPrimitiveProtocolValue.StringValue(type));
        if (eventInit != null) {
            if (eventInit instanceof Map) {
                args.add(new WDLocalValue.ObjectLocalValue((Map<?, ?>) eventInit));
            } else {
                throw new IllegalArgumentException("eventInit must be a Map.");
            }
        }
        webDriver.script().callFunction(
                fn,
                /* await */ false,
                target,
                args,
                getRemoteReference(),
                WDResultOwnership.NONE,
                null
        );
    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg) {
        List<WDLocalValue> args = Arrays.asList(
                WDLocalValue.fromObject(selector),
                WDLocalValue.fromObject(expression),
                WDLocalValue.fromObject(arg)
        );
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(sel, exprSrc, a){ " +
                        "  const el = this.querySelector(sel); " +
                        "  if(!el) throw new Error('evalOnSelector: no element for selector: '+sel); " +
                        "  const fn = new Function('node','arg', 'return ('+exprSrc+')(node,arg);'); " +
                        "  return fn(el, a); " +
                        "}",
                /* await */ true,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        return WebDriverUtil.unwrap(r);
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        List<WDLocalValue> args = Arrays.asList(
                WDLocalValue.fromObject(selector),
                WDLocalValue.fromObject(expression),
                WDLocalValue.fromObject(arg)
        );
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(sel, exprSrc, a){ " +
                        "  const list = Array.from(this.querySelectorAll(sel)); " +
                        "  const fn = new Function('nodes','arg', 'return ('+exprSrc+')(nodes,arg);'); " +
                        "  return fn(list, a); " +
                        "}",
                /* await */ true,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        return WebDriverUtil.unwrap(r);
    }

    @Override
    public void fill(String value, FillOptions options) {
        waitForActionability(options);
        scrollIntoViewIfNeeded(null);
        focus();

        WDEvaluateResult macRes = webDriver.script().callFunction(
                "function(){ try { return /Mac|iPhone|iPad|iPod/.test(navigator.platform); } catch(e){ return false; } }",
                true, target, null, getRemoteReference(), WDResultOwnership.ROOT, null
        );
        boolean isMac = WebDriverUtil.asBoolean(macRes);
        String modKey = isMac ? WDKeys.META : WDKeys.CONTROL;

        List<KeySourceAction> seq = new ArrayList<>();

        // Select all
        seq.add(new KeySourceAction.KeyDownAction(modKey));
        seq.add(new KeySourceAction.KeyDownAction("a"));
        seq.add(new PauseAction(InputDelaysConfig.getKeyDownDelayMs()));
        seq.add(new KeySourceAction.KeyUpAction("a"));
        seq.add(new PauseAction(InputDelaysConfig.getKeyUpDelayMs()));
        seq.add(new KeySourceAction.KeyUpAction(modKey));
        seq.add(new PauseAction(InputDelaysConfig.getKeyUpDelayMs()));

        // Clear or type
        if (value == null || value.isEmpty()) {
            seq.add(new KeySourceAction.KeyDownAction(WDKeys.DELETE));
            seq.add(new PauseAction(InputDelaysConfig.getKeyDownDelayMs()));
            seq.add(new KeySourceAction.KeyUpAction(WDKeys.DELETE));
            seq.add(new PauseAction(InputDelaysConfig.getKeyUpDelayMs()));
        } else {
            // erst leeren
            seq.add(new KeySourceAction.KeyDownAction(WDKeys.DELETE));
            seq.add(new PauseAction(InputDelaysConfig.getKeyDownDelayMs()));
            seq.add(new KeySourceAction.KeyUpAction(WDKeys.DELETE));
            seq.add(new PauseAction(InputDelaysConfig.getKeyUpDelayMs()));

            // dann Zeichen mit Delay senden
            for (int i = 0; i < value.length(); i++) {
                String ch = String.valueOf(value.charAt(i));
                seq.add(new KeySourceAction.KeyDownAction(ch));
                seq.add(new PauseAction(InputDelaysConfig.getKeyDownDelayMs()));
                seq.add(new KeySourceAction.KeyUpAction(ch));
                seq.add(new PauseAction(InputDelaysConfig.getKeyUpDelayMs()));
            }
        }

        SourceActions.KeySourceActions keyboard =
                new SourceActions.KeySourceActions("keyboard", seq);

        input().performActions(requireContextId(), Collections.singletonList(keyboard));
    }

    @Override
    public void focus() {
        webDriver.script().callFunction(
                "function(){ this.focus(); }",
                /* await */ false,
                target, null, getRemoteReference(),
                WDResultOwnership.NONE, null
        );
        waitTwoAnimationFrames();
    }

    @Override
    public String getAttribute(String name) {
        List<WDLocalValue> args = Collections.<WDLocalValue>singletonList(new WDPrimitiveProtocolValue.StringValue(name));
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(n){ return this.getAttribute(n); }",
                /* await */ true,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if (v instanceof WDPrimitiveProtocolValue.StringValue) {
                return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
            }
        }
        return null;
    }

    @Override
    public void hover(HoverOptions options) {
        waitForActionability(ActionabilityCheck.HOVER, 30_000);
        scrollIntoViewIfNeeded(null);
        waitTwoAnimationFrames();

        double dx = 0.0, dy = 0.0;
        if (options != null && options.position != null) {
            dx = options.position.x;
            dy = options.position.y;
        }

        WDElementOrigin origin = elementOrigin();
        List<PointerSourceAction> pointer = new ArrayList<PointerSourceAction>();
        pointer.add(new PointerSourceAction.PointerMoveAction(dx, dy, origin));

        SourceActions.PointerSourceActions pointerSeq =
                new SourceActions.PointerSourceActions(
                        "mouse-pointer",
                        new SourceActions.PointerSourceActions.PointerParameters(),
                        pointer
                );

        List<SourceActions> actions = new ArrayList<SourceActions>();
        actions.add(pointerSeq);

        String contextId = requireContextId();
        input().performActions(contextId, actions);
    }

    @Override public String innerHTML() { return evaluateString("el => el.innerHTML"); }
    @Override public String innerText() { return evaluateString("el => el.innerText"); }

    @Override
    public String inputValue(InputValueOptions options) {
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(){"
                        + "  const el = this;"
                        + "  const t = el.tagName && el.tagName.toUpperCase();"
                        + "  if (t === 'INPUT' || t === 'TEXTAREA' || t === 'SELECT') return el.value;"
                        + "  const lab = el.closest && el.closest('label');"
                        + "  if (lab && lab.control) return lab.control.value;"
                        + "  throw new Error('Element is not an input/textarea/select or label/control');"
                        + "}",
                /* await */ true,
                target, null, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if (v instanceof WDPrimitiveProtocolValue.StringValue) {
                return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
            }
        }
        return null;
    }

    @Override public boolean isChecked()  { return evaluateBoolean("el => !!el.checked"); }
    @Override public boolean isVisible()  { return evaluateBoolean("el => !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length)"); }
    @Override public boolean isDisabled() { return evaluateBoolean("el => !!el.disabled"); }
    @Override public boolean isEditable() { return evaluateBoolean("el => (el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || el.isContentEditable) && !el.readOnly && !el.disabled"); }
    @Override public boolean isEnabled()  { return !isDisabled(); }
    @Override public boolean isHidden()   { return !isVisible(); }

    @Override public Frame ownerFrame() { return null; }

    @Override
    public void press(String key, PressOptions options) {
        scrollIntoViewIfNeeded(null);
        focus();

        long delay = (options != null && options.delay != null) ? options.delay.longValue() : 0L;

        String[] parts = key.split("\\+");
        List<String> tokens = new ArrayList<String>();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p != null && !p.isEmpty()) tokens.add(p);
        }
        if (tokens.isEmpty()) return;

        String main = tokens.get(tokens.size() - 1);
        List<String> mods = tokens.subList(0, tokens.size() - 1);

        List<KeySourceAction> seq = new ArrayList<KeySourceAction>();

        List<String> order = Arrays.asList("Control", "Alt", "Shift", "Meta");
        for (int i = 0; i < order.size(); i++) {
            String o = order.get(i);
            for (int j = 0; j < mods.size(); j++) {
                String m = mods.get(j);
                if (m.equalsIgnoreCase(o)) {
                    seq.add(new KeySourceAction.KeyDownAction(o));
                }
            }
        }

        seq.add(new KeySourceAction.KeyDownAction(main));
        if (delay > 0) seq.add(new PauseAction((int) delay));
        seq.add(new KeySourceAction.KeyUpAction(main));

        ListIterator<String> it = order.listIterator(order.size());
        while (it.hasPrevious()) {
            String o = it.previous();
            for (int j = 0; j < mods.size(); j++) {
                String m = mods.get(j);
                if (m.equalsIgnoreCase(o)) {
                    seq.add(new KeySourceAction.KeyUpAction(o));
                }
            }
        }

        SourceActions.KeySourceActions keyboard = new SourceActions.KeySourceActions("keyboard", seq);
        input().performActions(requireContextId(), Collections.singletonList(keyboard));
    }

    @Override
    public void waitForElementState(ElementState state, WaitForElementStateOptions options) {
        for (int i = 0; i < 10; i++) {
            if (matchesElementState(state)) return;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        long timeout = options != null && options.timeout != null ? options.timeout.longValue() : 30_000L;
        String state = options != null && options.state != null ? options.state.name() : "ATTACHED";

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start <= timeout) {
            ElementHandle eh = querySelector(selector);
            if ("HIDDEN".equalsIgnoreCase(state)) {
                if (eh == null || !eh.isVisible()) return null;
            } else if ("VISIBLE".equalsIgnoreCase(state)) {
                if (eh != null && eh.isVisible()) return eh;
            } else { // ATTACHED
                if (eh != null) return eh;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("waitForSelector timeout: " + selector + " (state=" + state + ")");
    }

    @Override
    public ElementHandle querySelector(String selector) {
        List<WDLocalValue> args = Collections.<WDLocalValue>singletonList(WDLocalValue.fromObject(selector));
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(sel){ return this.querySelector(sel); }",
                /* await */ false,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if ("node".equals(v.getType())) {
                return new ElementHandleImpl(webDriver, v, target);
            }
        }
        return null;
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        List<WDLocalValue> args = Collections.<WDLocalValue>singletonList(WDLocalValue.fromObject(selector));
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(sel){ return Array.from(this.querySelectorAll(sel)); }",
                /* await */ false,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if (v instanceof WDRemoteValue.ArrayRemoteValue) {
                List<ElementHandle> out = new ArrayList<ElementHandle>();
                List<WDRemoteValue> arr = ((WDRemoteValue.ArrayRemoteValue) v).getValue();
                for (int i = 0; i < arr.size(); i++) {
                    WDRemoteValue it = arr.get(i);
                    if ("node".equals(it.getType())) {
                        out.add(new ElementHandleImpl(webDriver, it, target));
                    }
                }
                return out;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        return new byte[0];
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) {
        double timeout = (options != null && options.timeout != null) ? options.timeout : 30_000;
        long start = System.currentTimeMillis();

        while (true) {
            WDEvaluateResult visRes = webDriver.script().callFunction(
                    "function(){"
                            + "  const r=this.getBoundingClientRect();"
                            + "  const vw = (window.innerWidth||document.documentElement.clientWidth);"
                            + "  const vh = (window.innerHeight||document.documentElement.clientHeight);"
                            + "  return r.width>0 && r.height>0 && r.top>=0 && r.left>=0 && r.bottom<=vh && r.right<=vw;"
                            + "}",
                    false,
                    target,
                    null,
                    getRemoteReference(),
                    WDResultOwnership.NONE,
                    null
            );

            boolean inView = WebDriverUtil.asBoolean(visRes);
            if (inView) {
                waitTwoAnimationFrames();
                return;
            }

            webDriver.script().callFunction(
                    "function(){ this.scrollIntoView({block:'center', inline:'nearest'}); }",
                    false,
                    target,
                    null,
                    getRemoteReference(),
                    WDResultOwnership.NONE,
                    null
            );

            waitTwoAnimationFrames();

            if ((System.currentTimeMillis() - start) > timeout) {
                throw new RuntimeException("Timeout in scrollIntoViewIfNeeded()");
            }
        }
    }

    @Override
    public List<String> selectOption(String value, SelectOptionOptions options) {
        if (value == null) return Collections.<String>emptyList();
        return selectOption(new String[]{ value }, options);
    }

    @Override
    public List<String> selectOption(ElementHandle values, SelectOptionOptions options) {
        return Collections.<String>emptyList();
    }

    @Override
    public List<String> selectOption(String[] values, SelectOptionOptions options) {
        List<WDLocalValue> args = Collections.<WDLocalValue>singletonList(WDLocalValue.fromObject(values));
        WDEvaluateResult r = webDriver.script().callFunction(
                "function(vals){ " +
                        "  const resolveControl = (el)=>{ " +
                        "    if (el instanceof HTMLSelectElement) return el; " +
                        "    const lab = el.closest && el.closest('label'); " +
                        "    if (lab && lab.control instanceof HTMLSelectElement) return lab.control; " +
                        "    if (el.tagName && el.tagName.toUpperCase()==='SELECT') return el; " +
                        "    throw new Error('selectOption: not a <select> or associated control'); " +
                        "  }; " +
                        "  const sel = resolveControl(this); " +
                        "  const wanted = Array.isArray(vals) ? vals : [vals]; " +
                        "  const chosen = []; " +
                        "  if (!sel.multiple) { for (var i=0;i<sel.options.length;i++){ sel.options[i].selected = false; } } " +
                        "  function pick(s){ " +
                        "    for (var i=0;i<sel.options.length;i++){ var o = sel.options[i]; " +
                        "      if (o.value === s || o.label === s) { o.selected = true; if (chosen.indexOf(o.value)<0) chosen.push(o.value); return true; } " +
                        "    } return false; " +
                        "  } " +
                        "  for (var j=0;j<wanted.length;j++){ pick(String(wanted[j])); if (!sel.multiple) break; } " +
                        "  sel.dispatchEvent(new Event('input',{bubbles:true})); " +
                        "  sel.dispatchEvent(new Event('change',{bubbles:true})); " +
                        "  return chosen; " +
                        "}",
                /* await */ true,
                target, args, getRemoteReference(),
                WDResultOwnership.ROOT, null
        );
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if (v instanceof WDRemoteValue.ArrayRemoteValue) {
                List<String> out = new ArrayList<String>();
                List<WDRemoteValue> arr = ((WDRemoteValue.ArrayRemoteValue) v).getValue();
                for (int i = 0; i < arr.size(); i++) {
                    WDRemoteValue it = arr.get(i);
                    if (it instanceof WDPrimitiveProtocolValue.StringValue) {
                        out.add(((WDPrimitiveProtocolValue.StringValue) it).getValue());
                    }
                }
                return out;
            }
        }
        return Collections.<String>emptyList();
    }

    @Override public List<String> selectOption(SelectOption values, SelectOptionOptions options) { return Collections.<String>emptyList(); }
    @Override public List<String> selectOption(ElementHandle[] values, SelectOptionOptions options) { return Collections.<String>emptyList(); }
    @Override public List<String> selectOption(SelectOption[] values, SelectOptionOptions options) { return Collections.<String>emptyList(); }

    @Override
    public void selectText(SelectTextOptions options) {
        // Intentionally left blank: keep behavior parity with previous stub.
    }

    @Override
    public void setChecked(boolean checked, SetCheckedOptions options) {
        if (!isCheckboxOrRadio())
            throw new IllegalStateException("setChecked: element is not <input type=checkbox|radio>");

        if (isChecked() == checked) return;

        waitForActionability(ActionabilityCheck.CLICK, options != null && options.timeout != null ? options.timeout : 30_000);
        scrollIntoViewIfNeeded(null);
        waitTwoAnimationFrames();
        click(new ClickOptions());

        if (isChecked() != checked)
            throw new RuntimeException("setChecked failed: state did not change.");
    }

    @Override public void setInputFiles(Path files, SetInputFilesOptions options) { }
    @Override public void setInputFiles(Path[] files, SetInputFilesOptions options) { }
    @Override public void setInputFiles(FilePayload files, SetInputFilesOptions options) { }
    @Override public void setInputFiles(FilePayload[] files, SetInputFilesOptions options) { }

    @Override
    public void tap(TapOptions options) {
        waitForActionability(ActionabilityCheck.TAP, 30_000);
        scrollIntoViewIfNeeded(null);
        waitTwoAnimationFrames();

        double dx = 0, dy = 0;
        if (options != null && options.position != null) { dx = options.position.x; dy = options.position.y; }

        WDElementOrigin origin = elementOrigin();
        List<PointerSourceAction> pointer = new ArrayList<PointerSourceAction>();
        pointer.add(new PointerSourceAction.PointerMoveAction(dx, dy, origin));
        pointer.add(new PointerSourceAction.PointerDownAction(0)); // touch: button=0
        pointer.add(new PointerSourceAction.PointerUpAction(0));

        SourceActions.PointerSourceActions pointerSeq =
                new SourceActions.PointerSourceActions(
                        "touch-pointer",
                        new SourceActions.PointerSourceActions.PointerParameters(
                                SourceActions.PointerSourceActions.PointerParameters.PointerType.TOUCH),
                        pointer
                );

        input().performActions(requireContextId(), Collections.singletonList(pointerSeq));
    }

    @Override
    public String textContent() { return evaluateString("el => el.textContent"); }

    @Override
    public void type(String text, TypeOptions options) {
        if (text == null) return;
        for (int i = 0; i < text.length(); i++) {
            press(String.valueOf(text.charAt(i)), null);
        }
    }

    @Override
    public void uncheck(UncheckOptions options) {
        if (!isCheckboxOrRadio())
            throw new IllegalStateException("uncheck: element is not <input type=checkbox|radio>");

        if (!isChecked()) return;

        waitForActionability(ActionabilityCheck.CLICK, options != null && options.timeout != null ? options.timeout : 30_000);
        scrollIntoViewIfNeeded(null);
        waitTwoAnimationFrames();
        click(new ClickOptions());

        if (isChecked())
            throw new RuntimeException("uncheck failed: still checked.");
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private boolean isCheckboxOrRadio() {
        return evaluateBoolean("el => el.tagName && el.tagName.toLowerCase() === 'input' && (el.type === 'checkbox' || el.type === 'radio')");
    }

    /** Evaluate arrow/function expression against 'this' and return boolean. */
    private boolean evaluateBoolean(String arrowExpr) {
        WDEvaluateResult r = callExprOnThis(arrowExpr);
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            return (v instanceof WDPrimitiveProtocolValue.BooleanValue) &&
                    ((WDPrimitiveProtocolValue.BooleanValue) v).getValue();
        }
        return false;
    }

    /** Evaluate arrow/function expression against 'this' and return string. */
    private String evaluateString(String arrowExpr) {
        WDEvaluateResult r = callExprOnThis(arrowExpr);
        if (r instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue v = ((WDEvaluateResult.WDEvaluateResultSuccess) r).getResult();
            if (v instanceof WDPrimitiveProtocolValue.StringValue) {
                return ((WDPrimitiveProtocolValue.StringValue) v).getValue();
            }
        }
        return "";
    }

    /** Return current bounding box as primitive array. */
    private double[] getBoundingBoxNow() {
        WDEvaluateResult res = webDriver.script().callFunction(
                "function(){ const r=this.getBoundingClientRect(); return [r.x,r.y,r.width,r.height]; }",
                false,
                target,
                null,
                getRemoteReference(),
                WDResultOwnership.NONE,
                null
        );
        return WebDriverUtil.asDoubleArray(res);
    }

    /** Wait two animation frames or time-based fallback when page is not visible. */
    private void waitTwoAnimationFrames() {
        webDriver.script().callFunction(
                "() => new Promise(resolve => {" +
                        "  const vis = document.visibilityState;" +
                        "  const raf = typeof requestAnimationFrame === 'function';" +
                        "  if (vis === 'visible' && raf) {" +
                        "    requestAnimationFrame(() => requestAnimationFrame(resolve));" +
                        "  } else {" +
                        "    setTimeout(resolve, 50);" +
                        "  }" +
                        "})",
                true,
                target,
                null,
                getRemoteReference(),
                WDResultOwnership.NONE,
                null
        );
    }

    /** Derive checks from CheckOptions (skip when force). */
    private void waitForActionability(CheckOptions options) {
        boolean force = options != null && Boolean.TRUE.equals(options.force);
        double timeout = (options != null && options.timeout != null) ? options.timeout : 30_000;
        if (!force) {
            // Checking a checkbox/radio uses the same preconditions as a click
            waitForActionability(ActionabilityCheck.CLICK, timeout);
        }
    }

    /** Centralized actionability wait with configurable check set and timeout. */
    public void waitForActionability(ActionabilityCheck check, double timeout) {
        EnumSet<ActionabilityRequirement> req = check.getRequirements();
        long start = System.currentTimeMillis();

        while (true) {
            boolean ok = true;

            if (req.contains(ActionabilityRequirement.VISIBLE)) {
                boolean visible = WebDriverUtil.asBoolean(webDriver.script().callFunction(
                        "function(){ const r=this.getBoundingClientRect();" +
                                " return r.width>0 && r.height>0 && window.getComputedStyle(this).visibility!=='hidden'; }",
                        true, target, null, getRemoteReference(), WDResultOwnership.NONE, null));
                ok = ok && visible;
            }

            if (ok && req.contains(ActionabilityRequirement.ENABLED)) {
                boolean enabled = WebDriverUtil.asBoolean(webDriver.script().callFunction(
                        "function(){ return !this.disabled; }",
                        true, target, null, getRemoteReference(), WDResultOwnership.NONE, null));
                ok = ok && enabled;
            }

            if (ok && req.contains(ActionabilityRequirement.EDITABLE)) {
                boolean editable = WebDriverUtil.asBoolean(webDriver.script().callFunction(
                        "function(){ return (this instanceof HTMLInputElement || this instanceof HTMLTextAreaElement || this.isContentEditable) && !this.readOnly; }",
                        true, target, null, getRemoteReference(), WDResultOwnership.NONE, null));
                ok = ok && editable;
            }

            if (ok && req.contains(ActionabilityRequirement.RECEIVES_EVENTS)) {
                boolean receives = WebDriverUtil.asBoolean(webDriver.script().callFunction(
                        "function(){ const r=this.getBoundingClientRect(); return r.width>0 && r.height>0; }",
                        true, target, null, getRemoteReference(), WDResultOwnership.NONE, null));
                ok = ok && receives;
            }

            if (ok) {
                if (req.contains(ActionabilityRequirement.STABLE)) {
                    double[] last = getBoundingBoxNow();
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    double[] now = getBoundingBoxNow();
                    if (!java.util.Arrays.equals(last, now)) {
                        ok = false;
                    }
                }
            }

            if (ok) {
                if (req.contains(ActionabilityRequirement.VISIBLE)) {
                    waitTwoAnimationFrames();
                }
                return;
            }

            if ((System.currentTimeMillis() - start) > timeout) {
                throw new RuntimeException("Timeout waiting for actionability: " + check);
            }

            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    /** Derive checks from FillOptions (skip when force). */
    private void waitForActionability(FillOptions options) {
        boolean force = options != null && Boolean.TRUE.equals(options.force);
        double timeout = options != null && options.timeout != null ? options.timeout : 30_000;
        if (!force) {
            waitForActionability(ActionabilityCheck.FILL, timeout);
        }
    }

    /** Derive checks from ClickOptions (skip when force). */
    private void waitForActionability(ClickOptions options) {
        boolean force = options != null && Boolean.TRUE.equals(options.force);
        double timeout = options != null && options.timeout != null ? options.timeout : 30_000;
        if (!force) {
            waitForActionability(ActionabilityCheck.CLICK, timeout);
        }
    }

    private boolean matchesElementState(ElementState state) {
        switch (state) {
            case VISIBLE:  return isVisible();
            case HIDDEN:   return !isVisible();
            case ENABLED:  return isEnabled();
            case DISABLED: return isDisabled();
            case EDITABLE: return isEditable();
            default:       return false;
        }
    }

    private String requireContextId() {
        String cid = cachedContextId;
        if (cid != null) return cid;
        synchronized (this) {
            if (cachedContextId == null) {
                cachedContextId = BrowsingContextResolver.resolveContextId(webDriver, target);
            }
            return cachedContextId;
        }
    }

    private WDInputManager input() {
        return webDriver.input();
    }

    private WDElementOrigin elementOrigin() {
        return new WDElementOrigin(getRemoteReference());
    }

    private static int mapMouseButton(MouseButton b) {
        if (b == null) return 0;
        switch (b) {
            case LEFT:   return 0;
            case MIDDLE: return 1;
            case RIGHT:  return 2;
            default:     return 0;
        }
    }

    private static List<SourceActions> buildModifierActions(List<KeyboardModifier> mods, boolean keyDown) {
        if (mods == null || mods.isEmpty()) return java.util.Collections.<SourceActions>emptyList();

        List<String> order = Arrays.asList("Control", "Alt", "Shift", "Meta");
        if (!keyDown) {
            List<String> rev = new ArrayList<String>(order);
            Collections.reverse(rev);
            order = rev;
        }

        Set<String> want = new HashSet<String>();
        for (int i = 0; i < mods.size(); i++) {
            KeyboardModifier m = mods.get(i);
            switch (m) {
                case CONTROL: want.add("Control"); break;
                case ALT:     want.add("Alt");     break;
                case SHIFT:   want.add("Shift");   break;
                case META:    want.add("Meta");    break;
            }
        }

        List<KeySourceAction> keyActions = new ArrayList<KeySourceAction>();
        for (int i = 0; i < order.size(); i++) {
            String k = order.get(i);
            if (!want.contains(k)) continue;
            if (keyDown) keyActions.add(new KeySourceAction.KeyDownAction(k));
            else         keyActions.add(new KeySourceAction.KeyUpAction(k));
        }
        if (keyActions.isEmpty()) return java.util.Collections.<SourceActions>emptyList();

        SourceActions.KeySourceActions seq = new SourceActions.KeySourceActions("keyboard", keyActions);
        return java.util.Collections.<SourceActions>singletonList(seq);
    }

    // ------------------------------------------------------------------------------------------------
    // Script bridging (minimal glue to keep old "el => ..." snippets working)
    // ------------------------------------------------------------------------------------------------

    /**
     * Call an arrow/function expression like "(el)=>expr" with 'this' bound to the element.
     * Return the expression result.
     */
    private WDEvaluateResult callExprOnThis(String arrowExprSource) {
        return callExprOnThis(arrowExprSource, null);
    }

    private WDEvaluateResult callExprOnThis(String arrowExprSource, List<WDLocalValue> args) {
        String trimmed = arrowExprSource == null ? "" : arrowExprSource.trim();
        String body;
        if (trimmed.startsWith("function")) {
            body = "return (" + trimmed + ")(this, ...(arguments||[]));";
        } else {
            body = "return (" + trimmed + ")(this, ...(arguments||[]));";
        }
        return webDriver.script().callFunction(
                "function(){ " + body + " }",
                /* await */ true,
                target,
                args,
                ScriptUtils.sharedRef(remoteValue), // bind 'this' through shared id (works with WDRemoteValue of type 'node')
                WDResultOwnership.ROOT,
                null
        );
    }

    /**
     * Call a statement or arrow function against 'this' that does not need to return a value.
     */
    private WDEvaluateResult callStmtOnThis(String stmtOrArrow) {
        return callStmtOnThis(stmtOrArrow, null);
    }

    private WDEvaluateResult callStmtOnThis(String stmtOrArrow, List<WDLocalValue> args) {
        String t = stmtOrArrow == null ? "" : stmtOrArrow.trim();
        String fn;
        if (t.startsWith("function")) {
            fn = t;
        } else if (t.contains("=>")) {
            fn = "function(){ (" + t + ")(this, ...(arguments||[])); }";
        } else {
            fn = "function(){ " + t + " }";
        }
        return webDriver.script().callFunction(
                fn,
                /* await */ false,
                target,
                args,
                ScriptUtils.sharedRef(remoteValue),
                WDResultOwnership.NONE,
                null
        );
    }
}

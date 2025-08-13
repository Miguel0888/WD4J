package de.bund.zrb.event;

import com.microsoft.playwright.Mouse;
import de.bund.zrb.manager.WDInputManager;
import de.bund.zrb.command.request.parameters.input.sourceActions.PointerSourceAction;
import de.bund.zrb.command.request.parameters.input.sourceActions.SourceActions;
import de.bund.zrb.command.request.parameters.input.sourceActions.WheelSourceAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MouseImpl implements Mouse {
    private final WDInputManager inputManager;
    private final String contextId;

    private static final String POINTER_ID = "mouse";
    private static final String WHEEL_ID   = "wheel";

    public MouseImpl(WDInputManager inputManager, String contextId) {
        this.inputManager = inputManager;
        this.contextId = contextId;
    }

    @Override
    public void click(double x, double y, ClickOptions options) {
        List<PointerSourceAction> pointer = new ArrayList<>();
        pointer.add(new PointerSourceAction.PointerMoveAction(x, y));
        pointer.add(new PointerSourceAction.PointerDownAction(0));
        pointer.add(new PointerSourceAction.PointerUpAction(0));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(
                POINTER_ID,
                new SourceActions.PointerSourceActions.PointerParameters(), // default "mouse"
                pointer));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void dblclick(double x, double y, DblclickOptions options) {
        List<PointerSourceAction> pointer = new ArrayList<>();
        pointer.add(new PointerSourceAction.PointerMoveAction(x, y));
        pointer.add(new PointerSourceAction.PointerDownAction(0));
        pointer.add(new PointerSourceAction.PointerUpAction(0));
        pointer.add(new PointerSourceAction.PointerDownAction(0));
        pointer.add(new PointerSourceAction.PointerUpAction(0));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(
                POINTER_ID,
                new SourceActions.PointerSourceActions.PointerParameters(),
                pointer));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void down(DownOptions options) {
        List<SourceActions> actions = Arrays.asList(
                new SourceActions.PointerSourceActions(
                        POINTER_ID,
                        new SourceActions.PointerSourceActions.PointerParameters(),
                        Arrays.asList(new PointerSourceAction.PointerDownAction(0))));
        inputManager.performActions(contextId, actions);
    }

    @Override
    public void up(UpOptions options) {
        List<SourceActions> actions = Arrays.asList(
                new SourceActions.PointerSourceActions(
                        POINTER_ID,
                        new SourceActions.PointerSourceActions.PointerParameters(),
                        Arrays.asList(new PointerSourceAction.PointerUpAction(0))));
        inputManager.performActions(contextId, actions);
    }

    @Override
    public void move(double x, double y, MoveOptions options) {
        List<SourceActions> actions = Arrays.asList(
                new SourceActions.PointerSourceActions(
                        POINTER_ID,
                        new SourceActions.PointerSourceActions.PointerParameters(),
                        Arrays.asList(new PointerSourceAction.PointerMoveAction(x, y))));
        inputManager.performActions(contextId, actions);
    }

    @Override
    public void wheel(double dx, double dy) {
        List<SourceActions> actions = Arrays.asList(
                new SourceActions.WheelSourceActions(
                        WHEEL_ID,
                        Arrays.asList(new WheelSourceAction.WheelScrollAction(0, 0, (int) dx, (int) dy))));
        inputManager.performActions(contextId, actions);
    }
}


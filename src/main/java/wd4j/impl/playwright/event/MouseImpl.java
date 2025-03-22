package wd4j.impl.playwright.event;

import wd4j.api.Mouse;
import wd4j.impl.manager.WDInputManager;
import wd4j.impl.dto.command.request.parameters.input.sourceActions.PointerSourceAction;
import wd4j.impl.dto.command.request.parameters.input.sourceActions.SourceActions;
import wd4j.impl.dto.command.request.parameters.input.sourceActions.WheelSourceAction;

import java.util.ArrayList;
import java.util.List;

public class MouseImpl implements Mouse {
    WDInputManager inputManager;
    String contextId = "default";
    private static final String POINTER_ID = "mouse-pointer"; // ToDo: Check ID
    private static final String WHEEL_ID = "wheel-scroll"; // ToDo: Check ID

    public MouseImpl(String contextId) {
        inputManager = null; // ToDo: Implement this, how to get the manager? Might be a constructor parameter?
        this.contextId = contextId;
    }

    @Override
    public void click(double x, double y, ClickOptions options) {
        List<PointerSourceAction> pointerActions = new ArrayList<>();
        pointerActions.add(new PointerSourceAction.PointerMoveAction(x, y));  // Maus bewegen
        pointerActions.add(new PointerSourceAction.PointerDownAction(0)); // Linke Maustaste drücken (0 = links)
        pointerActions.add(new PointerSourceAction.PointerUpAction(0));   // Linke Maustaste loslassen

        // PointerSourceActions als Ganzes erstellen
        SourceActions.PointerSourceActions pointerSourceActions =
                new SourceActions.PointerSourceActions(POINTER_ID, new SourceActions.PointerSourceActions.PointerParameters(), pointerActions);

        // Liste von SourceActions erstellen, da performActions eine Liste erwartet
        List<SourceActions> actions = new ArrayList<>();
        actions.add(pointerSourceActions);

        // performActions aufrufen mit Kontext-ID und der Liste von Aktionen
        inputManager.performActions(contextId, actions);
    }


    @Override
    public void dblclick(double x, double y, DblclickOptions options) {
        List<PointerSourceAction> pointerActions = new ArrayList<>();
        pointerActions.add(new PointerSourceAction.PointerMoveAction(x, y));
        pointerActions.add(new PointerSourceAction.PointerDownAction(0));
        pointerActions.add(new PointerSourceAction.PointerUpAction(0));
        pointerActions.add(new PointerSourceAction.PointerDownAction(0));
        pointerActions.add(new PointerSourceAction.PointerUpAction(0));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(POINTER_ID, new SourceActions.PointerSourceActions.PointerParameters(), pointerActions));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void down(DownOptions options) {
        List<PointerSourceAction> pointerActions = new ArrayList<>();
        pointerActions.add(new PointerSourceAction.PointerDownAction(0));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(POINTER_ID, new SourceActions.PointerSourceActions.PointerParameters(), pointerActions));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void up(UpOptions options) {
        List<PointerSourceAction> pointerActions = new ArrayList<>();
        pointerActions.add(new PointerSourceAction.PointerUpAction(0));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(POINTER_ID, new SourceActions.PointerSourceActions.PointerParameters(), pointerActions));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void move(double x, double y, MoveOptions options) {
        List<PointerSourceAction> pointerActions = new ArrayList<>();
        pointerActions.add(new PointerSourceAction.PointerMoveAction(x, y));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.PointerSourceActions(POINTER_ID, new SourceActions.PointerSourceActions.PointerParameters(), pointerActions));

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void wheel(double deltaX, double deltaY) {
        List<WheelSourceAction> wheelActions = new ArrayList<>();
        wheelActions.add(new WheelSourceAction.WheelScrollAction(0, 0, (int) deltaX, (int) deltaY));

        List<SourceActions> actions = new ArrayList<>();
        actions.add(new SourceActions.WheelSourceActions(WHEEL_ID, wheelActions));

        inputManager.performActions(contextId, actions);
    }
}

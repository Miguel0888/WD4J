package wd4j.impl.playwright.event;

import wd4j.api.Mouse;
import wd4j.impl.manager.WDInputManager;
import wd4j.impl.webdriver.command.request.parameters.input.PerformActionsParameters;
import wd4j.impl.webdriver.command.request.parameters.input.sourceActions.PointerSourceAction;

import java.util.ArrayList;
import java.util.List;

public class MouseImpl implements Mouse {
    WDInputManager inputManager;

    public MouseImpl() {
        inputManager = WDInputManager.getInstance();
    }

    @Override
    public void click(double x, double y, ClickOptions options) {
//        List<PerformActionsParameters.SourceActions> actions = new ArrayList<>();
//        actions.add(new PointerSourceAction.PointerMoveAction(x, y));  // Maus bewegen
//        actions.add(new PointerSourceAction.PointerDownAction(options.button)); // Maustaste drücken
//        actions.add(new PointerSourceAction.PointerUpAction(options.button));   // Maustaste loslassen
//
//        inputManager.performActions(contextId, actions);
    }


    @Override
    public void dblclick(double x, double y, DblclickOptions options) {
        System.out.println("Mouse double-clicked at (" + x + ", " + y + ") with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void down(DownOptions options) {
        System.out.println("Mouse button pressed down with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void move(double x, double y, MoveOptions options) {
        System.out.println("Mouse moved to (" + x + ", " + y + ") with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void up(UpOptions options) {
        System.out.println("Mouse button released with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void wheel(double deltaX, double deltaY) {
        System.out.println("Mouse wheel scrolled by (" + deltaX + ", " + deltaY + ")");
        // TODO: Implement actual WebDriver BiDi command
    }
}

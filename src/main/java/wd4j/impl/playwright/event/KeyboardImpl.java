package wd4j.impl.playwright.event;

import wd4j.api.Keyboard;
import wd4j.impl.manager.WDInputManager;
import wd4j.impl.webdriver.command.request.parameters.input.sourceActions.KeySourceAction;
import wd4j.impl.webdriver.command.request.parameters.input.sourceActions.SourceActions;

import java.util.ArrayList;
import java.util.List;

public class KeyboardImpl implements Keyboard {
    WDInputManager inputManager;
    String contextId = "default";
    private static final String KEYBOARD_ID = "keyboard"; // ToDo: Check ID

    public KeyboardImpl(String contextId) {
        inputManager = WDInputManager.getInstance();
        this.contextId = contextId;
    }

    @Override
    public void down(String key) {
        List<KeySourceAction> keyActions = new ArrayList<>();
        keyActions.add(new KeySourceAction.KeyDownAction(key));

        SourceActions.KeySourceActions keySourceActions =
                new SourceActions.KeySourceActions(KEYBOARD_ID, keyActions);

        List<SourceActions> actions = new ArrayList<>();
        actions.add(keySourceActions);

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void up(String key) {
        List<KeySourceAction> keyActions = new ArrayList<>();
        keyActions.add(new KeySourceAction.KeyUpAction(key));

        SourceActions.KeySourceActions keySourceActions =
                new SourceActions.KeySourceActions(KEYBOARD_ID, keyActions);

        List<SourceActions> actions = new ArrayList<>();
        actions.add(keySourceActions);

        inputManager.performActions(contextId, actions);
    }

    @Override
    public void press(String key, PressOptions options) {
        down(key);
        up(key);
    }

    @Override
    public void type(String text, TypeOptions options) {
        for (char c : text.toCharArray()) {
            press(String.valueOf(c), null);
        }
    }

    @Override
    public void insertText(String text) {
        List<KeySourceAction> keyActions = new ArrayList<>();

        for (char c : text.toCharArray()) {
            keyActions.add(new KeySourceAction.KeyDownAction(String.valueOf(c)));
            keyActions.add(new KeySourceAction.KeyUpAction(String.valueOf(c)));
        }

        SourceActions.KeySourceActions keySourceActions =
                new SourceActions.KeySourceActions(KEYBOARD_ID, keyActions);

        List<SourceActions> actions = new ArrayList<>();
        actions.add(keySourceActions);

        inputManager.performActions(contextId, actions);
    }
}

package de.bund.zrb.event;

import com.microsoft.playwright.Keyboard;
import de.bund.zrb.manager.WDInputManager;
import de.bund.zrb.command.request.parameters.input.sourceActions.KeySourceAction;
import de.bund.zrb.command.request.parameters.input.sourceActions.SourceActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyboardImpl implements Keyboard {
    private final WDInputManager inputManager;
    private final String contextId;
    private static final String KEYBOARD_ID = "keyboard";

    public KeyboardImpl(WDInputManager inputManager, String contextId) {
        this.inputManager = inputManager;
        this.contextId = contextId;
    }

    @Override
    public void down(String key) {
        SourceActions.KeySourceActions ks = new SourceActions.KeySourceActions(
                KEYBOARD_ID, Arrays.asList(new KeySourceAction.KeyDownAction(key)));
        inputManager.performActions(contextId, Arrays.asList(ks));
    }

    @Override
    public void up(String key) {
        SourceActions.KeySourceActions ks = new SourceActions.KeySourceActions(
                KEYBOARD_ID, Arrays.asList(new KeySourceAction.KeyUpAction(key)));
        inputManager.performActions(contextId, Arrays.asList(ks));
    }

    @Override
    public void press(String key, PressOptions options) {
        down(key);
        up(key);
    }

    @Override
    public void type(String text, TypeOptions options) {
        // einfache Sequenz: keyDown/Up pro Zeichen
        List<KeySourceAction> seq = new ArrayList<>();
        for (char c : text.toCharArray()) {
            String k = String.valueOf(c);
            seq.add(new KeySourceAction.KeyDownAction(k));
            seq.add(new KeySourceAction.KeyUpAction(k));
        }
        inputManager.performActions(contextId,
                Arrays.asList(new SourceActions.KeySourceActions(KEYBOARD_ID, seq)));
    }

    @Override
    public void insertText(String text) {
        // identisch zu type(), falls kein spezielles „insertText“ vorhanden
        type(text, null);
    }
}


package wd4j.impl.playwright.event;

import wd4j.api.Keyboard;

public class KeyboardImpl implements Keyboard {
    @Override
    public void down(String key) {
        System.out.println("Key pressed down: " + key);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void insertText(String text) {
        System.out.println("Inserted text: " + text);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void press(String key, PressOptions options) {
        System.out.println("Key pressed: " + key + " with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void type(String text, TypeOptions options) {
        System.out.println("Typing text: " + text + " with options: " + options);
        // TODO: Implement actual WebDriver BiDi command
    }

    @Override
    public void up(String key) {
        System.out.println("Key released: " + key);
        // TODO: Implement actual WebDriver BiDi command
    }
}

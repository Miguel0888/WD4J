package de.bund.zrb;

import com.microsoft.playwright.options.BindingCallback;
import com.microsoft.playwright.options.FunctionCallback;

import java.util.HashMap;
import java.util.Map;

public class Callbacks {

    // Additional features
    private final Map<String, FunctionCallback> exposedFunctions = new HashMap<>();
    private final Map<String, BindingCallback> exposedBindings = new HashMap<>();


    public void registerFunction(String name, FunctionCallback callback) {
        exposedFunctions.put(name, callback);
    }

    public void registerBinding(String name, BindingCallback callback) {
        exposedBindings.put(name, callback);
    }

    public FunctionCallback getFunction(String name) {
        return exposedFunctions.get(name);
    }

    public BindingCallback getBinding(String name) {
        return exposedBindings.get(name);
    }
}

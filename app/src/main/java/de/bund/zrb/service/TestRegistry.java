package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.model.TestSuite;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TestRegistry {

    private static final TestRegistry INSTANCE = new TestRegistry();
    private final List<TestSuite> suites = new ArrayList<>();

    private TestRegistry() {
        load();
    }

    public static TestRegistry getInstance() {
        return INSTANCE;
    }

    public List<TestSuite> getAll() {
        return suites;
    }

    public void addSuite(TestSuite suite) {
        suites.add(suite);
    }

    public void save() {
        SettingsService.getInstance().save("tests.json", suites);
    }

    public void load() {
        Type type = new TypeToken<List<TestSuite>>() {}.getType();
        List<TestSuite> loaded = SettingsService.getInstance().load("tests.json", type);
        if (loaded != null) {
            suites.clear();
            suites.addAll(loaded);
        }
    }
}

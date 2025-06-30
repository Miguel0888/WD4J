package de.bund.zrb.service;

import de.bund.zrb.model.TestSuite;

import java.util.ArrayList;
import java.util.List;

public class TestRegistry {

    private static final TestRegistry INSTANCE = new TestRegistry();

    private final List<TestSuite> suites;

    private TestRegistry() {
        this.suites = new ArrayList<>();
        load(); // Direkt laden beim Start!
    }

    public static TestRegistry getInstance() {
        return INSTANCE;
    }

    public List<TestSuite> getAll() {
        return new ArrayList<>(suites);
    }

    public void addSuite(TestSuite suite) {
        suites.add(suite);
    }

    public void removeSuite(TestSuite suite) {
        suites.remove(suite);
    }

    public void save() {
        SettingsService.getInstance().saveTests(suites);
    }

    public void load() {
        List<TestSuite> loaded = SettingsService.getInstance().loadTests(List.class);
        suites.clear();
        if (loaded != null) {
            suites.addAll(loaded);
        }
    }
}

package de.bund.zrb.service;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;

import java.io.File;
import java.util.List;

public interface TestRecorderService {
    void loadTestSuite(File file);
    void saveTestSuite(File file);

    void addTestCase(TestCase testCase);
    void removeTestCase(String testCaseName);
    void renameTestCase(String oldName, String newName);

    List<TestCase> getAllTestCases();

    void addActionToTestCase(String testCaseName, TestAction action);
    void removeActionFromTestCase(String testCaseName, int index);

    void playTestSuite();
}

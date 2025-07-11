package de.bund.zrb.ui;

import de.bund.zrb.model.TestSuite;
import de.bund.zrb.ui.TestNode;

import java.util.List;

public interface TestPlayerUi {
    List<TestSuite> getSelectedSuites();

    TestNode getSelectedNode();

    void updateNodeStatus(TestNode node, boolean passed);

    void updateSuiteStatus(TestNode suite);

    TestNode getRootNode();
}

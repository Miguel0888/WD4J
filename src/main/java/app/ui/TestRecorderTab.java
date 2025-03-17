package app.ui;

import app.controller.MainController;
import app.dto.TestAction;
import app.dto.TestCase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TestRecorderTab implements UIComponent {
    private JPanel panel;
    private JTree testCaseTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTable actionTable;
    private DefaultTableModel tableModel;

    public TestRecorderTab(MainController controller) {
        panel = new JPanel(new BorderLayout());

        // Wurzelknoten für den Baum (Testfälle)
        rootNode = new DefaultMutableTreeNode("Testfälle");
        treeModel = new DefaultTreeModel(rootNode);
        testCaseTree = new JTree(treeModel);
        testCaseTree.setRootVisible(false);

        // UI-Elemente initialisieren
        JScrollPane treeScrollPane = new JScrollPane(testCaseTree);
        JScrollPane tableScrollPane = new JScrollPane(createActionTable());

        // SplitPane für Baum & Tabelle
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, tableScrollPane);
        splitPane.setDividerLocation(200);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(createControlPanel(), BorderLayout.SOUTH);

        // Baum-Klick-Listener hinzufügen
        testCaseTree.addTreeSelectionListener(e -> updateTableForSelection());
    }

    private JTable createActionTable() {
        // Tabellenmodell mit DropDowns
        tableModel = new DefaultTableModel(new Object[]{"Aktion", "Locator-Typ", "Selektor/Text", "Timeout"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        actionTable = new JTable(tableModel);
        setUpComboBoxes();
        return actionTable;
    }

    private void setUpComboBoxes() {
        // Aktionen als DropDown
        String[] actions = {"click", "input", "screenshot"};
        JComboBox<String> actionComboBox = new JComboBox<>(actions);
        TableColumn actionColumn = actionTable.getColumnModel().getColumn(0);
        actionColumn.setCellEditor(new DefaultCellEditor(actionComboBox));

        // Locator-Typen als DropDown
        String[] locatorTypes = {"css", "xpath", "id", "text", "role", "label", "placeholder", "altText"};
        JComboBox<String> locatorTypeComboBox = new JComboBox<>(locatorTypes);
        TableColumn locatorColumn = actionTable.getColumnModel().getColumn(1);
        locatorColumn.setCellEditor(new DefaultCellEditor(locatorTypeComboBox));

        // Selektor/Text als DropDown (kann später mit echten Daten befüllt werden)
        TableColumn selectorColumn = actionTable.getColumnModel().getColumn(2);
        selectorColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>()));
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        JButton addTestCaseButton = new JButton("Testfall hinzufügen");
        JButton removeTestCaseButton = new JButton("Testfall entfernen");
        JButton addActionButton = new JButton("Aktion hinzufügen");
        JButton removeActionButton = new JButton("Aktion entfernen");

        addTestCaseButton.addActionListener(e -> addTestCase());
        removeTestCaseButton.addActionListener(e -> removeTestCase());
        addActionButton.addActionListener(e -> addAction());
        removeActionButton.addActionListener(e -> removeAction());

        panel.add(addTestCaseButton);
        panel.add(removeTestCaseButton);
        panel.add(addActionButton);
        panel.add(removeActionButton);
        return panel;
    }

    private void addTestCase() {
        String testName = JOptionPane.showInputDialog(panel, "Testfall-Name eingeben:");
        if (testName != null && !testName.trim().isEmpty()) {
            DefaultMutableTreeNode testCaseNode = new DefaultMutableTreeNode(testName);
            testCaseNode.add(new DefaultMutableTreeNode("@Given"));
            testCaseNode.add(new DefaultMutableTreeNode("@When"));
            testCaseNode.add(new DefaultMutableTreeNode("@Then"));
            rootNode.add(testCaseNode);
            treeModel.reload();
        }
    }

    private void removeTestCase() {
        TreePath selectedPath = testCaseTree.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getParent() != null) {
                treeModel.removeNodeFromParent(selectedNode);
            }
        }
    }

    private void addAction() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) testCaseTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.toString().equals("@When")) {
            tableModel.addRow(new Object[]{"click", "css", "", 3000});
        }
    }

    private void removeAction() {
        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.removeRow(selectedRow);
        }
    }

    private void updateTableForSelection() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) testCaseTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.toString().equals("@When")) {
            // Falls ein @When-Knoten ausgewählt wurde, zeige die Tabelle
            actionTable.setEnabled(true);
        } else {
            // Andernfalls Tabelle deaktivieren
            actionTable.setEnabled(false);
        }
    }

    public List<TestCase> getTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode testCaseNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            TestCase testCase = new TestCase();
            testCase.setName(testCaseNode.toString());

            List<TestAction> givenActions = new ArrayList<>();
            List<TestAction> whenActions = new ArrayList<>();
            List<TestAction> thenActions = new ArrayList<>();

            for (int j = 0; j < testCaseNode.getChildCount(); j++) {
                DefaultMutableTreeNode phaseNode = (DefaultMutableTreeNode) testCaseNode.getChildAt(j);
                if (phaseNode.toString().equals("@When")) {
                    for (int k = 0; k < tableModel.getRowCount(); k++) {
                        TestAction action = new TestAction();
                        action.setAction((String) tableModel.getValueAt(k, 0));
                        action.setLocatorType((String) tableModel.getValueAt(k, 1));
                        action.setSelectedSelector((String) tableModel.getValueAt(k, 2));
                        action.setTimeout((int) tableModel.getValueAt(k, 3));
                        whenActions.add(action);
                    }
                }
            }
            testCase.setGiven(givenActions);
            testCase.setWhen(whenActions);
            testCase.setThen(thenActions);
            testCases.add(testCase);
        }
        return testCases;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getComponentTitle() {
        return "Recorder";
    }
}

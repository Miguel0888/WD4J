package app.ui;

import app.controller.MainController;
import app.dto.TestAction;
import app.dto.TestCase;
import app.dto.TestSuite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TestRecorderTab implements UIComponent {
    private JPanel panel;
    private JTree testCaseTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JTable actionTable;
    private DefaultTableModel tableModel;
    private JList<String> givenList, thenList;
    private DefaultListModel<String> givenListModel, thenListModel;
    private JPanel dynamicButtonPanel;

    public TestRecorderTab(MainController controller) {
        panel = new JPanel(new BorderLayout());

        // Testfall-Hierarchie (JTree)
        rootNode = new DefaultMutableTreeNode("Testfälle");
        treeModel = new DefaultTreeModel(rootNode);
        testCaseTree = new JTree(treeModel);
        testCaseTree.setRootVisible(false);

        // Panel für Inhalte (CardLayout für dynamische Ansicht)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Panels für @Given, @When, @Then
        contentPanel.add(createGivenPanel(), "@Given");
        contentPanel.add(createWhenPanel(), "@When");
        contentPanel.add(createThenPanel(), "@Then");

        // UI-Elemente
        JScrollPane treeScrollPane = new JScrollPane(testCaseTree);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, contentPanel);
        splitPane.setDividerLocation(200);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(createControlPanel(), BorderLayout.SOUTH);

        // Baum-Klick-Listener
        testCaseTree.addTreeSelectionListener(e -> updateContentForSelection());
    }

    private JPanel createGivenPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        givenListModel = new DefaultListModel<>();
        givenList = new JList<>(givenListModel);
        panel.add(new JScrollPane(givenList), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createWhenPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(createActionTable()), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createThenPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        thenListModel = new DefaultListModel<>();
        thenList = new JList<>(thenListModel);
        panel.add(new JScrollPane(thenList), BorderLayout.CENTER);

        return panel;
    }

    private JTable createActionTable() {
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
        String[] actions = {"click", "input", "screenshot"};
        JComboBox<String> actionComboBox = new JComboBox<>(actions);
        TableColumn actionColumn = actionTable.getColumnModel().getColumn(0);
        actionColumn.setCellEditor(new DefaultCellEditor(actionComboBox));

        String[] locatorTypes = {"css", "xpath", "id", "text", "role", "label", "placeholder", "altText"};
        JComboBox<String> locatorTypeComboBox = new JComboBox<>(locatorTypes);
        TableColumn locatorColumn = actionTable.getColumnModel().getColumn(1);
        locatorColumn.setCellEditor(new DefaultCellEditor(locatorTypeComboBox));

        TableColumn selectorColumn = actionTable.getColumnModel().getColumn(2);
        selectorColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>()));
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        JButton addTestCaseButton = new JButton("Testfall hinzufügen");
        JButton removeTestCaseButton = new JButton("Testfall entfernen");

        addTestCaseButton.addActionListener(e -> addTestCase());
        removeTestCaseButton.addActionListener(e -> removeTestCase());

        panel.add(addTestCaseButton);
        panel.add(removeTestCaseButton);

        // Dynamischer Button-Bereich für @Given, @When, @Then
        dynamicButtonPanel = new JPanel();
        panel.add(dynamicButtonPanel);

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

    private void updateContentForSelection() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) testCaseTree.getLastSelectedPathComponent();
        dynamicButtonPanel.removeAll();

        if (selectedNode != null) {
            switch (selectedNode.toString()) {
                case "@Given":
                    cardLayout.show(contentPanel, "@Given");
                    JButton addGivenButton = new JButton("Vorbedingung hinzufügen");
                    addGivenButton.addActionListener(e -> {
                        String input = JOptionPane.showInputDialog("Neue Vorbedingung:");
                        if (input != null && !input.isEmpty()) {
                            givenListModel.addElement(input);
                        }
                    });
                    dynamicButtonPanel.add(addGivenButton);
                    break;

                case "@When":
                    cardLayout.show(contentPanel, "@When");
                    JButton addActionButton = new JButton("Aktion hinzufügen");
                    addActionButton.addActionListener(e -> tableModel.addRow(new Object[]{"click", "css", "", 3000}));
                    JButton removeActionButton = new JButton("Aktion entfernen");
                    removeActionButton.addActionListener(e -> {
                        int selectedRow = actionTable.getSelectedRow();
                        if (selectedRow != -1) {
                            tableModel.removeRow(selectedRow);
                        }
                    });
                    dynamicButtonPanel.add(addActionButton);
                    dynamicButtonPanel.add(removeActionButton);
                    break;

                case "@Then":
                    cardLayout.show(contentPanel, "@Then");
                    JButton addThenButton = new JButton("Erwartetes Ergebnis hinzufügen");
                    addThenButton.addActionListener(e -> {
                        String input = JOptionPane.showInputDialog("Neue Bedingung (z.B. 'Text sichtbar'):");
                        if (input != null && !input.isEmpty()) {
                            thenListModel.addElement(input);
                        }
                    });
                    dynamicButtonPanel.add(addThenButton);
                    break;
            }
        }
        dynamicButtonPanel.revalidate();
        dynamicButtonPanel.repaint();
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getComponentTitle() {
        return "Recorder";
    }

    @Override
    public JMenuItem getMenuItem() {
        // Erstelle ein Untermenü für die Settings-Optionen
        JMenu settingsMenu = new JMenu("Test");

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveSettings());

        JMenuItem loadItem = new JMenuItem("Load");
        loadItem.addActionListener(e -> loadSettings());

        settingsMenu.add(saveItem);
        settingsMenu.add(loadItem);

        return settingsMenu;  // Das Menü wird dem Hauptmenü hinzugefügt
    }

    private void saveSettings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Speichern als...");
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // DTO für Speicherung füllen
            TestSuite testSuite = new TestSuite();
            testSuite.setTestCases(getTestCases());

            // JSON serialisieren
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(testSuite);

            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write(json);
                JOptionPane.showMessageDialog(null, "Testfälle erfolgreich gespeichert!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Fehler beim Speichern: " + e.getMessage());
            }
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
                String phaseName = phaseNode.toString();

                if (phaseName.equals("@When")) {
                    whenActions.addAll(getActionsFromTable());
                }
            }

            testCase.setGiven(givenActions);
            testCase.setWhen(whenActions);
            testCase.setThen(thenActions);
            testCases.add(testCase);
        }

        return testCases;
    }

    private List<TestAction> getActionsFromTable() {
        List<TestAction> actions = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            TestAction action = new TestAction();
            action.setAction((String) tableModel.getValueAt(i, 0));
            action.setLocatorType((String) tableModel.getValueAt(i, 1));
            action.setSelectedSelector((String) tableModel.getValueAt(i, 2));
            action.setTimeout((int) tableModel.getValueAt(i, 3));

            actions.add(action);
        }

        return actions;
    }

    private void loadSettings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Testfälle laden...");
        int userSelection = fileChooser.showOpenDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();

            try (BufferedReader reader = new BufferedReader(new FileReader(fileToLoad))) {
                Gson gson = new Gson();
                Type testSuiteType = new TypeToken<TestSuite>(){}.getType();
                TestSuite testSuite = gson.fromJson(reader, testSuiteType);

                if (testSuite != null) {
                    loadTestCasesIntoUI(testSuite.getTestCases());
                    JOptionPane.showMessageDialog(null, "Testfälle erfolgreich geladen!");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Fehler beim Laden: " + e.getMessage());
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void loadTestCasesIntoUI(List<TestCase> testCases) {
        rootNode.removeAllChildren();

        for (TestCase testCase : testCases) {
            DefaultMutableTreeNode testCaseNode = new DefaultMutableTreeNode(testCase.getName());
            DefaultMutableTreeNode givenNode = new DefaultMutableTreeNode("@Given");
            DefaultMutableTreeNode whenNode = new DefaultMutableTreeNode("@When");
            DefaultMutableTreeNode thenNode = new DefaultMutableTreeNode("@Then");

            testCaseNode.add(givenNode);
            testCaseNode.add(whenNode);
            testCaseNode.add(thenNode);
            rootNode.add(testCaseNode);

            // @Given Liste füllen
            if (!testCase.getGiven().isEmpty()) {
                givenListModel.clear();
                for (TestAction action : testCase.getGiven()) {
                    givenListModel.addElement(action.getAction()); // Oder ein anderes relevantes Attribut
                }
            }

            // @When Tabelle füllen
            if (!testCase.getWhen().isEmpty()) {
                tableModel.setRowCount(0); // Vorherige Daten leeren
                for (TestAction action : testCase.getWhen()) {
                    tableModel.addRow(new Object[]{
                            action.getAction(),
                            action.getLocatorType(),
                            action.getSelectedSelector(),
                            action.getTimeout()
                    });
                }
            }

            // @Then Liste füllen
            if (!testCase.getThen().isEmpty()) {
                thenListModel.clear();
                for (TestAction action : testCase.getThen()) {
                    thenListModel.addElement(action.getAction()); // Erwartetes Ergebnis hinzufügen
                }
            }
        }

        treeModel.reload();
    }


}

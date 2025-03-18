package app.ui;

import app.controller.MainController;
import app.dto.TestAction;
import app.dto.TestCase;
import app.dto.TestSuite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import wd4j.helper.RecorderService;
import wd4j.helper.dto.RecordedEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TestRecorderTab implements UIComponent {
    public static final String SELECT_ON_CREATE = "@When"; // Alternatives: "@Given", "@When", "@Then"
    private final JPanel panel;
    private final ActionTableModel tableModel;
    private final JTree testCaseTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JPanel contentPanel;
    private final CardLayout cardLayout;
    private JTable actionTable;

    private JList<String> givenList, thenList;
    private DefaultListModel<String> givenListModel, thenListModel;
    private JPanel dynamicButtonPanel;

    private LinkedHashMap<String, TestCase> testCasesMap = new LinkedHashMap<>();

    private JButton addTestCaseButton;
    private JButton removeTestCaseButton;

    public TestRecorderTab(MainController controller) {
        panel = new JPanel(new BorderLayout());
        tableModel = new ActionTableModel(Arrays.asList("✔", "Aktion", "Locator-Typ", "Selektor", "Wert", "Wartezeit"));

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

        // Doppelklick zum Umbenennen eines Testfalls ermöglichen
        // Baum-Klick-Listener für Selektion und Interaktionen
        testCaseTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                TreePath path = testCaseTree.getPathForLocation(evt.getX(), evt.getY());
                if (path == null) return;

                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (evt.getClickCount() == 1) {
                    // 🔥 Einfacher Klick = Auf- oder Zuklappen
                    toggleTreeNode(selectedNode);
                } else if (evt.getClickCount() == 2) {
                    // 🔥 Doppelklick = Testfall umbenennen
                    renameTestCase();
                }
            }
        });
    }

    private void toggleTreeNode(DefaultMutableTreeNode node) {
        if (node == null) return;

        TreePath path = new TreePath(node.getPath());
        if (testCaseTree.isExpanded(path)) {
            testCaseTree.collapsePath(path); // 🔽 Zuklappen
        } else {
            testCaseTree.expandPath(path); // 🔼 Aufklappen
        }
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
        actionTable = new JTable(tableModel);

        // 🟢 Checkbox-Spalte als erste Spalte hinzufügen
        TableColumn checkBoxColumn = actionTable.getColumnModel().getColumn(0);
        checkBoxColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        checkBoxColumn.setCellRenderer(actionTable.getDefaultRenderer(Boolean.class));
        checkBoxColumn.setPreferredWidth(30); // ✔ Feste Breite für die Checkbox-Spalte

        // 🟢 Spalteneditoren setzen
        tableModel.setUpEditors(actionTable);

        return actionTable;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();

        addTestCaseButton = new JButton("Testfall hinzufügen");
        removeTestCaseButton = new JButton("Testfall entfernen");

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
        int counter = 1;
        String defaultName;

        // Suche nach einem freien Namen (Testfall_1, Testfall_2, ...)
        do {
            defaultName = "Testfall_" + counter++;
        } while (testCasesMap.containsKey(defaultName));

        // Erstelle ein Textfeld mit Vorausfüllung
        JTextField textField = new JTextField(defaultName);

        // Timer, um Fokus & Selektion sicher zu setzen
        Timer focusTimer = new Timer(100, e -> {
            textField.requestFocusInWindow(); // Setzt den Fokus ins Eingabefeld
            textField.selectAll(); // Markiert den gesamten Text zum direkten Überschreiben
        });
        focusTimer.setRepeats(false);
        focusTimer.start();

        // Dialog für Testfallnamen
        int option = JOptionPane.showConfirmDialog(
                panel,
                textField,
                "Testfall-Name eingeben:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Falls der Benutzer OK drückt und das Feld nicht leer ist
        String testName = textField.getText().trim();
        if (option == JOptionPane.OK_OPTION && !testName.isEmpty()) {
            if (testCasesMap.containsKey(testName)) {
                JOptionPane.showMessageDialog(panel, "Ein Testfall mit diesem Namen existiert bereits!", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Neuen Testfall erstellen
            TestCase testCase = new TestCase();
            testCase.setName(testName);
            testCase.setGiven(new ArrayList<>());
            testCase.setWhen(new ArrayList<>());
            testCase.setThen(new ArrayList<>());

            testCasesMap.put(testName, testCase);

            // Dem Baum hinzufügen
            DefaultMutableTreeNode testCaseNode = new DefaultMutableTreeNode(testName);
            DefaultMutableTreeNode givenNode = new DefaultMutableTreeNode("@Given");
            DefaultMutableTreeNode whenNode = new DefaultMutableTreeNode("@When");
            DefaultMutableTreeNode thenNode = new DefaultMutableTreeNode("@Then");

            testCaseNode.add(givenNode);
            testCaseNode.add(whenNode);
            testCaseNode.add(thenNode);
            rootNode.add(testCaseNode);
            treeModel.reload();

            DefaultMutableTreeNode jumpTo;
            switch (SELECT_ON_CREATE) {
                case "@Given":
                    jumpTo = givenNode;
                    break;
                case "@When":
                    jumpTo = whenNode;
                    break;
                case "@Then":
                    jumpTo = thenNode;
                    break;
                default:
                    jumpTo = whenNode;
            }

            // 🟢 Den neuen Testfall in der Baumstruktur auswählen
            TreePath thenPath = new TreePath(new Object[]{rootNode, testCaseNode, jumpTo});
            testCaseTree.setSelectionPath(thenPath);
            testCaseTree.scrollPathToVisible(thenPath);

            // 🟢 Direkt zu einem bestimmten Element im CardLayout springen
            cardLayout.show(contentPanel, SELECT_ON_CREATE);
        }
    }

    private void removeTestCase() {
        TreePath selectedPath = testCaseTree.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

            if (parentNode == null) return; // 🔴 Kein übergeordnetes Element? -> Abbruch

            // 🛑 Nur komplette Testfälle entfernen (direkte Kinder von rootNode)
            if (parentNode == rootNode) {
                String testName = selectedNode.toString();
                testCasesMap.remove(testName);
                treeModel.removeNodeFromParent(selectedNode);
            } else {
                // ❌ Falls `@Given`, `@When`, `@Then` ausgewählt ist -> Keine Aktion
                JOptionPane.showMessageDialog(panel, "Nur komplette Testfälle können entfernt werden!", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateContentForSelection() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) testCaseTree.getLastSelectedPathComponent();
        dynamicButtonPanel.removeAll();

        boolean isTestCaseSelected = false;

        if (selectedNode != null) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

            // 🛑 Prüfen, ob die ausgewählte Node eine Testfall-Node ist
            isTestCaseSelected = (parentNode == rootNode);

            // Falls eine Unterkategorie (@Given, @When, @Then) gewählt wurde, Buttons ausblenden
            addTestCaseButton.setVisible(isTestCaseSelected || rootNode.getChildCount() == 0);
            removeTestCaseButton.setVisible(isTestCaseSelected);

            // Falls kein Testfall existiert, muss der "Testfall hinzufügen"-Button immer sichtbar sein
            if (rootNode.getChildCount() == 0) {
                addTestCaseButton.setVisible(true);
                removeTestCaseButton.setVisible(false);
            }

            String testName = (parentNode != null) ? parentNode.toString() : selectedNode.toString();
            TestCase testCase = testCasesMap.get(testName);

            if (testCase == null) {
                return;
            }

            switch (selectedNode.toString()) {
                case "@Given":
                    cardLayout.show(contentPanel, "@Given");
                    givenListModel.clear();
                    for (TestAction action : testCase.getGiven()) {
                        givenListModel.addElement(action.getAction());
                    }

                    JButton addGivenButton = new JButton("Vorbedingung hinzufügen");
                    addGivenButton.addActionListener(e -> {
                        String input = JOptionPane.showInputDialog("Neue Vorbedingung:");
                        if (input != null && !input.isEmpty()) {
                            givenListModel.addElement(input);
                            testCase.getGiven().add(new TestAction(input));
                        }
                    });
                    dynamicButtonPanel.add(addGivenButton);
                    break;

                case "@When":
                    cardLayout.show(contentPanel, "@When");
                    tableModel.setRowData(testCase.getWhen());

                    JButton addActionButton = new JButton("Aktion hinzufügen");
                    addActionButton.addActionListener(e -> {
                        LinkedHashMap<String, String> extractedValues = new LinkedHashMap<>(); // ToDo: Move into TestAction
                        TestAction newAction = new TestAction("click", "css", "", extractedValues, 3000);
                        tableModel.addAction(newAction);

                        // 🟢 Testfall-Datenstruktur aktualisieren
                        if (testCase != null) {
                            testCase.getWhen().add(newAction); // ✅ Aktion dauerhaft speichern
                        }
                    });

                    JButton removeActionButton = new JButton("Aktion entfernen");
                    removeActionButton.addActionListener(e -> {
                        int selectedRow = actionTable.getSelectedRow();
                        if (selectedRow != -1) {
                            tableModel.removeAction(selectedRow);
                        }
                    });

                    JButton importRecordedButton = new JButton("Letzte Aktionen importieren");
                    importRecordedButton.addActionListener(e -> importRecordedActions());

                    dynamicButtonPanel.add(addActionButton);
                    dynamicButtonPanel.add(removeActionButton);
                    dynamicButtonPanel.add(importRecordedButton);
                    break;

                case "@Then":
                    cardLayout.show(contentPanel, "@Then");
                    thenListModel.clear();
                    for (TestAction action : testCase.getThen()) {
                        thenListModel.addElement(action.getAction());
                    }

                    JButton addThenButton = new JButton("Erwartetes Ergebnis hinzufügen");
                    addThenButton.addActionListener(e -> {
                        String input = JOptionPane.showInputDialog("Neue Bedingung (z.B. 'Text sichtbar'):");
                        if (input != null && !input.isEmpty()) {
                            thenListModel.addElement(input);
                            testCase.getThen().add(new TestAction(input));
                        }
                    });

                    dynamicButtonPanel.add(addThenButton);
                    break;
            }
        } else {
            // 🛑 Falls kein Testfall existiert, nur "Hinzufügen" sichtbar lassen
            addTestCaseButton.setVisible(true);
            removeTestCaseButton.setVisible(false);
        }

        // UI aktualisieren
        panel.revalidate();
        panel.repaint();
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
        // Erstelle ein Untermenü für die Test-Optionen
        JMenu settingsMenu = new JMenu("Test");

        JMenuItem loadItem = new JMenuItem("Load Testsuite");
        loadItem.addActionListener(e -> loadSettings());

        JMenuItem saveItem = new JMenuItem("Save Testsuite");
        saveItem.addActionListener(e -> saveSettings());

        settingsMenu.add(loadItem);
        settingsMenu.add(saveItem);

        // 🔹 Trennstrich hinzufügen
        settingsMenu.addSeparator();

        // 🔹 "New Testcase" Menüeintrag hinzufügen
        JMenuItem newTestCaseItem = new JMenuItem("New Testcase");
        newTestCaseItem.addActionListener(e -> addTestCase()); // Ruft dieselbe Methode auf wie der Button

        settingsMenu.add(newTestCaseItem);

        return settingsMenu; // Das Menü wird dem Hauptmenü hinzugefügt
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
        return new ArrayList<>(testCasesMap.values());
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
        testCasesMap.clear();

        for (TestCase testCase : testCases) {
            testCasesMap.put(testCase.getName(), testCase);

            DefaultMutableTreeNode testCaseNode = new DefaultMutableTreeNode(testCase.getName());
            DefaultMutableTreeNode givenNode = new DefaultMutableTreeNode("@Given");
            DefaultMutableTreeNode whenNode = new DefaultMutableTreeNode("@When");
            DefaultMutableTreeNode thenNode = new DefaultMutableTreeNode("@Then");

            testCaseNode.add(givenNode);
            testCaseNode.add(whenNode);
            testCaseNode.add(thenNode);
            rootNode.add(testCaseNode);
        }

        treeModel.reload();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void importRecordedActions() {
        RecorderService.getInstance().mergeInputEvents(); // 🔥 Bereinigung vor Import
        List<RecordedEvent> recordedEvents = RecorderService.getInstance().getRecordedEvents();

        if (recordedEvents.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Keine neuen Aktionen zum Importieren!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) testCaseTree.getLastSelectedPathComponent();
        if (selectedNode == null || !selectedNode.toString().equals("@When")) {
            JOptionPane.showMessageDialog(panel, "Wähle zuerst einen Testfall in @When!", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parentNode == null) return;

        TestCase testCase = testCasesMap.get(parentNode.toString());
        if (testCase == null) return;

        for (RecordedEvent event : recordedEvents) {
            TestAction action = RecorderService.getInstance().convertToTestAction(event);
            tableModel.addAction(action);
            testCase.getWhen().add(action);
        }

        RecorderService.getInstance().clearRecordedEvents();
    }

    private void renameTestCase() {
        TreePath selectedPath = testCaseTree.getSelectionPath();
        if (selectedPath == null) return;

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        if (selectedNode.getParent() == null) return; // Verhindert Umbenennung der Wurzel "Testfälle"

        String oldName = selectedNode.toString();
        TestCase testCase = testCasesMap.get(oldName);
        if (testCase == null) return; // Falls kein Testfall existiert

        // 📝 Eingabefeld für neuen Namen
        JTextField textField = new JTextField(oldName);

        // 🔥 Timer, um Fokus & Selektion sicher zu setzen
        Timer focusTimer = new Timer(100, e -> {
            textField.requestFocusInWindow();
            textField.selectAll();
        });
        focusTimer.setRepeats(false);
        focusTimer.start();

        int option = JOptionPane.showConfirmDialog(
                panel,
                textField,
                "Testfall umbenennen:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        String newName = textField.getText().trim();

        // 🛑 Falls Benutzer abbricht oder leer lässt, nichts tun
        if (option != JOptionPane.OK_OPTION || newName.isEmpty()) return;

        // 🛑 Falls Name bereits existiert, Fehler anzeigen
        if (testCasesMap.containsKey(newName)) {
            JOptionPane.showMessageDialog(panel, "Ein Testfall mit diesem Namen existiert bereits!", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ✅ Namen in der Map aktualisieren
        testCasesMap.remove(oldName);
        testCase.setName(newName);
        testCasesMap.put(newName, testCase);

        // ✅ Namen im JTree aktualisieren
        selectedNode.setUserObject(newName);
        treeModel.nodeChanged(selectedNode);
    }

}

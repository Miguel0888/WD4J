package de.bund.zrb.ui.commands;

import de.bund.zrb.PageImpl;
import de.bund.zrb.UserContextImpl;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.ui.commandframework.MenuCommand;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class SwitchTabCommand implements MenuCommand {

    private final BrowserServiceImpl browserService;

    public SwitchTabCommand(BrowserServiceImpl browserService) {
        this.browserService = browserService;
    }

    @Override
    public String getId() {
        return "browser.switchtab";
    }

    @Override
    public String getLabel() {
        return "Tab wechseln";
    }

    @Override
    public void perform() {
        List<UserContextImpl> userContextsWithPages = browserService.getBrowser().getUserContextImpls().stream()
                .filter(ctx -> !ctx.pages().isEmpty())
                .collect(Collectors.toList());

        if (userContextsWithPages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Keine UserContexts mit offenen Tabs gefunden.");
            return;
        }

        // UI-Komponenten
        JList<UserContextImpl> userContextList = new JList<>(userContextsWithPages.toArray(new UserContextImpl[0]));
        JList<PageImpl> pagesList = new JList<>();
        pagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // UserContextRenderer (optional ausgrauen)
        userContextList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UserContextImpl) {
                    UserContextImpl uc = (UserContextImpl) value;
                    if (uc.pages().isEmpty()) {
                        c.setEnabled(false);
                        c.setForeground(Color.LIGHT_GRAY);
                    } else {
                        c.setEnabled(true);
                        c.setForeground(Color.BLACK);
                    }
                    setText(uc.getUserContext().value());
                }
                return c;
            }
        });

        // PagesRenderer (optional)
        pagesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PageImpl) {
                    setText(((PageImpl) value).getBrowsingContext().value());
                }
                return c;
            }
        });

        // Listener: UserContext → Pages
        userContextList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                UserContextImpl selectedUC = userContextList.getSelectedValue();
                if (selectedUC != null) {
                    List<PageImpl> pages = selectedUC.pages().stream()
                            .map(p -> (PageImpl) p)
                            .collect(Collectors.toList());
                    pagesList.setListData(pages.toArray(new PageImpl[0]));
                } else {
                    pagesList.setListData(new PageImpl[0]);
                }
            }
        });

        // Default Auswahl
        userContextList.setSelectedIndex(0);

        // Dialog
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JScrollPane(userContextList));
        panel.add(new JScrollPane(pagesList));

        int result = JOptionPane.showConfirmDialog(null, panel, "Tab wechseln",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            PageImpl selectedPage = pagesList.getSelectedValue();
            if (selectedPage != null) {
                String contextId = selectedPage.getBrowsingContext().value();
                browserService.getBrowser().getWebDriver().browsingContext().activate(contextId);
                browserService.getBrowser().setActivePageId(contextId, true);
            } else {
                JOptionPane.showMessageDialog(null, "Kein Tab ausgewählt.");
            }
        }
    }
}

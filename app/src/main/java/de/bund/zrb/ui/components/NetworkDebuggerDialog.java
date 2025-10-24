package de.bund.zrb.ui.components;

import de.bund.zrb.BrowserImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.command.request.parameters.network.AddInterceptParameters;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.manager.WDNetworkManager;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.network.WDUrlPattern;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class NetworkDebuggerDialog extends JDialog {

    // ----------- wiring -----------
    private final BrowserImpl browser;
    private final WebDriver   wd;
    private final WDNetworkManager network;

    // ----------- intercept state -----------
    private final java.util.List<String> activeInterceptIds = new ArrayList<>();

    // ----------- UI controls -----------
    private final JCheckBox cbBefore = new JCheckBox("beforeRequestSent", true);
    private final JCheckBox cbResp   = new JCheckBox("responseStarted",   true);
    private final JCheckBox cbAuth   = new JCheckBox("authRequired",      true);

    private final JComboBox<String> cbContext = new JComboBox<>();
    private final JComboBox<String> cbPatternType = new JComboBox<>(new String[]{"string","pattern"});
    private final JTextField  tfPattern = new JTextField(48);

    private final JButton btnAddIntercept     = new JButton("Intercept hinzufügen");
    private final JButton btnRemoveIntercepts = new JButton("Alle Intercepts entfernen");

    private final JButton btnContinueReq   = new JButton("Continue Request");
    private final JButton btnContinueResp  = new JButton("Continue Response");
    private final JButton btnAuth          = new JButton("Continue with Auth…");
    private final JButton btnFail          = new JButton("Fail");
    private final JButton btnProvide       = new JButton("Provide…");
    private final JButton btnClear         = new JButton("Clear");

    private final JTable table;
    private final EventTableModel model = new EventTableModel();

    private final JLabel lbStatus = new JLabel("Bereit.");

    // Subscriptions cleanup
    private final java.util.List<Runnable> unsubscribeOnClose = new ArrayList<>();

    public NetworkDebuggerDialog(Window parent) {
        super(parent, "Network-Debugger", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1250, 640));
        setLocationRelativeTo(parent);

        // resolve driver + managers
        browser = BrowserServiceImpl.getInstance().getBrowser();
        wd = browser.getWebDriver();
        network = wd.network();

        // UI
        setContentPane(buildRoot());
        table = new JTable(model);
        styleTable(table);
        ((JScrollPane)((BorderLayout)getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER))
                .setViewportView(table);

        // wire
        fillContexts();
        wireActions();
        subscribeNetworkEvents();

        // cleanup intercepts on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                try { removeAllIntercepts(); } catch (Throwable ignore) {}
                for (Runnable r : unsubscribeOnClose) try { r.run(); } catch (Throwable ignore) {}
            }
        });
    }

    // ---------- UI building ----------

    private Container buildRoot() {
        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(10,10,10,10));

        // NORTH: Intercept builder
        JPanel north = new JPanel(new BorderLayout(8,8));
        north.setBorder(new TitledBorder("Intercepts"));

        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,6,4,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;

        int r=0;
        gc.gridy=r; gc.gridx=0; left.add(new JLabel("Phasen:"), gc);
        JPanel phases = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        phases.add(cbBefore); phases.add(cbResp); phases.add(cbAuth);
        gc.gridx=1; gc.weightx=1.0; left.add(phases, gc);

        r++; gc.gridy=r; gc.gridx=0; gc.weightx=0; left.add(new JLabel("Context:"), gc);
        gc.gridx=1; gc.weightx=1.0; cbContext.setPrototypeDisplayValue("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        left.add(cbContext, gc);

        r++; gc.gridy=r; gc.gridx=0; gc.weightx=0; left.add(new JLabel("URL-Pattern:"), gc);
        JPanel pat = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        pat.add(cbPatternType); pat.add(tfPattern);
        gc.gridx=1; gc.weightx=1.0; left.add(pat, gc);

        JPanel right = new JPanel(new GridBagLayout());
        GridBagConstraints ac = new GridBagConstraints();
        ac.insets = new Insets(6,6,6,6);
        ac.fill = GridBagConstraints.HORIZONTAL;
        ac.gridx=0; ac.gridy=0; right.add(btnAddIntercept, ac);
        ac.gridy=1; right.add(btnRemoveIntercepts, ac);

        north.add(left, BorderLayout.CENTER);
        north.add(right, BorderLayout.EAST);
        root.add(north, BorderLayout.NORTH);

        // CENTER: table
        JScrollPane sp = new JScrollPane(new JTable(model));
        sp.setBorder(new TitledBorder("Intercepted events"));
        root.add(sp, BorderLayout.CENTER);

        // SOUTH: actions + status
        JPanel south = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        actions.add(btnContinueReq);
        actions.add(btnContinueResp);
        actions.add(btnAuth);
        actions.add(btnFail);
        actions.add(btnProvide);
        actions.add(btnClear);
        south.add(actions, BorderLayout.WEST);

        lbStatus.setBorder(new EmptyBorder(0, 4, 0, 2));
        south.add(lbStatus, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(22);
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
    }

    // ---------- wiring ----------

    private void wireActions() {
        btnAddIntercept.addActionListener(e -> addInterceptFromUi());
        btnRemoveIntercepts.addActionListener(e -> {
            removeAllIntercepts();
            setStatus("Alle Intercepts entfernt.");
        });

        btnContinueReq.addActionListener(e -> withSelected(ev -> {
            if (ev.phase == Phase.BEFORE && ev.requestId != null) {
                network.continueRequest(ev.requestId);
                setStatus("continueRequest(" + ev.requestId + ")");
            }
        }));

        btnContinueResp.addActionListener(e -> withSelected(ev -> {
            if (ev.phase == Phase.RESPONSE && ev.requestId != null) {
                network.continueResponse(ev.requestId);
                setStatus("continueResponse(" + ev.requestId + ")");
            }
        }));

        btnAuth.addActionListener(e -> withSelected(ev -> {
            if (ev.phase == Phase.AUTH && ev.requestId != null) {
                // TODO: zeige kleinen Dialog für Credentials (username/password)
                // network.continueWithAuth(ev.requestId, new WDAuthCredentials("user","pass", null));
                setStatus("continueWithAuth requested (Dialog TODO)");
            }
        }));

        btnFail.addActionListener(e -> withSelected(ev -> {
            if (ev.requestId != null) {
                network.failRequest(ev.requestId);
                setStatus("failRequest(" + ev.requestId + ")");
            }
        }));

        btnProvide.addActionListener(e -> withSelected(ev -> {
            // TODO: kleiner Editor zum Setzen von Status/Headers/Body
            // network.provideResponse(ev.requestId, ...)
            setStatus("provideResponse requested (Editor TODO)");
        }));

        btnClear.addActionListener(e -> { model.clear(); setStatus("Liste geleert."); });

        // ESC zum Schließen
        getRootPane().registerKeyboardAction(
                ae -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void fillContexts() {
        cbContext.addItem("<ALL>");
        try {
            // hole aktuelle BCTs
            de.bund.zrb.command.response.WDBrowsingContextResult.GetTreeResult tree =
                    wd.browsingContext().getTree(null, 0L);
            for (de.bund.zrb.type.browsingContext.WDInfo info : tree.getContexts()) {
                if (info.getContext() != null)
                    cbContext.addItem(info.getContext().value());
            }
        } catch (Throwable ignore) {}
        cbContext.setSelectedIndex(0);
    }

    private void subscribeNetworkEvents() {
        // beforeRequestSent
        Runnable u1 = subscribe(WDEventNames.BEFORE_REQUEST_SENT.getName(), ev -> {
            if (!(ev instanceof WDNetworkEvent.BeforeRequestSent)) return;
            WDNetworkEvent.BeforeRequestSent e = (WDNetworkEvent.BeforeRequestSent) ev;
            WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD p = e.getParams();
            if (p == null || p.getRequest() == null) return;

            addRow(EventRow.fromBefore(p));
        });

        // responseStarted
        Runnable u2 = subscribe(WDEventNames.RESPONSE_STARTED.getName(), ev -> {
            if (!(ev instanceof WDNetworkEvent.ResponseStarted)) return;
            WDNetworkEvent.ResponseStarted e = (WDNetworkEvent.ResponseStarted) ev;
            WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p = e.getParams();
            if (p == null || p.getRequest() == null) return;

            addRow(EventRow.fromResponse(p));
        });

        // authRequired
        Runnable u3 = subscribe(WDEventNames.AUTH_REQUIRED.getName(), ev -> {
            if (!(ev instanceof WDNetworkEvent.AuthRequired)) return;
            WDNetworkEvent.AuthRequired e = (WDNetworkEvent.AuthRequired) ev;
            WDNetworkEvent.AuthRequired.AuthRequiredParametersWD p = e.getParams();
            if (p == null || p.getRequest() == null) return;

            addRow(EventRow.fromAuth(p));
        });

        unsubscribeOnClose.add(u1);
        unsubscribeOnClose.add(u2);
        unsubscribeOnClose.add(u3);
    }

    private Runnable subscribe(String eventName, Consumer<Object> handler) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, null, null);
        wd.addEventListener(req, handler);
        return () -> { try { wd.removeEventListener(eventName, (String) null, handler); } catch(Throwable ignore){} };
    }

    private void addRow(EventRow row) {
        SwingUtilities.invokeLater(() -> {
            model.add(row);
            setStatus("Event: " + row.phase + " " + row.method + " " + (row.blocked ? "[BLOCKED]" : ""));
        });
    }

    // ---------- Intercepts ----------

    private void addInterceptFromUi() {
        List<AddInterceptParameters.InterceptPhase> phases = new ArrayList<>();
        if (cbBefore.isSelected()) phases.add(AddInterceptParameters.InterceptPhase.BEFORE_REQUEST_SENT);
        if (cbResp.isSelected())   phases.add(AddInterceptParameters.InterceptPhase.RESPONSE_STARTED);
        if (cbAuth.isSelected())   phases.add(AddInterceptParameters.InterceptPhase.AUTH_REQUIRED);
        if (phases.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mindestens eine Phase wählen.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Contexts
        List<WDBrowsingContext> ctxs = null;
        String selCtx = (String) cbContext.getSelectedItem();
        if (selCtx != null && !"<ALL>".equals(selCtx)) {
            ctxs = Collections.singletonList(new WDBrowsingContext(selCtx));
        }

        // URL pattern
        List<WDUrlPattern> patterns = null;
        String type = (String) cbPatternType.getSelectedItem();
        String pat = tfPattern.getText();
        if (pat != null && !pat.trim().isEmpty()) {
            if ("pattern".equals(type)) {
                // simple: nur pathname als wildcards übernehmen
                WDUrlPattern.WDUrlPatternPattern p = new WDUrlPattern.WDUrlPatternPattern(null,null,null, pat.trim(), null);
                patterns = Collections.<WDUrlPattern>singletonList(p);
            } else {
                WDUrlPattern.WDUrlPatternString s = new WDUrlPattern.WDUrlPatternString(pat.trim());
                patterns = Collections.<WDUrlPattern>singletonList(s);
            }
        }

        String id = wd.network().addIntercept(phases, ctxs, patterns()).getIntercept().value();
        activeInterceptIds.add(id);
        setStatus("Intercept gesetzt: " + id);
    }

    private List<WDUrlPattern> patterns() {
        String type = (String) cbPatternType.getSelectedItem();
        String pat = tfPattern.getText();
        if (pat == null || pat.trim().isEmpty()) return null;
        if ("pattern".equals(type)) {
            return Collections.<WDUrlPattern>singletonList(new WDUrlPattern.WDUrlPatternPattern(null,null,null, pat.trim(), null));
        } else {
            return Collections.<WDUrlPattern>singletonList(new WDUrlPattern.WDUrlPatternString(pat.trim()));
        }
    }

    private void removeAllIntercepts() {
        for (String id : activeInterceptIds) {
            try { network.removeIntercept(id); } catch (Throwable ignore) {}
        }
        activeInterceptIds.clear();
    }

    // ---------- helpers ----------

    private void withSelected(Consumer<EventRow> c) {
        int r = table.getSelectedRow();
        if (r < 0) return;
        r = table.convertRowIndexToModel(r);
        EventRow row = model.get(r);
        if (row != null) c.accept(row);
    }

    private void setStatus(String s) { lbStatus.setText(s); }

    // ---------- model ----------

    enum Phase { BEFORE, RESPONSE, AUTH }

    static final class EventRow {
        final long   ts;
        final Phase  phase;
        final String contextId;
        final String requestId;
        final String method;
        final String url;
        final Integer status; // for response
        final boolean blocked;

        EventRow(long ts, Phase phase, String contextId, String requestId, String method, String url, Integer status, boolean blocked) {
            this.ts = ts; this.phase = phase; this.contextId = contextId; this.requestId = requestId;
            this.method = method; this.url = url; this.status = status; this.blocked = blocked;
        }

        static EventRow fromBefore(WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD p) {
            String ctx = (p.getContext()!=null) ? p.getContext().value() : null;
            String reqId = (p.getRequest()!=null && p.getRequest().getRequest()!=null) ? p.getRequest().getRequest().value() : null;
            String method = p.getRequest()!=null ? p.getRequest().getMethod() : null;
            String url = p.getRequest()!=null ? p.getRequest().getUrl() : null;
            return new EventRow(System.currentTimeMillis(), Phase.BEFORE, ctx, reqId, method, url, null, p.isBlocked());
        }

        static EventRow fromResponse(WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p) {
            String ctx = (p.getContext()!=null) ? p.getContext().value() : null;
            String reqId = (p.getRequest()!=null && p.getRequest().getRequest()!=null) ? p.getRequest().getRequest().value() : null;
            String method = p.getRequest()!=null ? p.getRequest().getMethod() : null;
            String url = (p.getResponse()!=null) ? p.getResponse().getUrl() : (p.getRequest()!=null ? p.getRequest().getUrl() : null);
            Integer status = (p.getResponse()!=null) ? (int)p.getResponse().getStatus() : null;
            return new EventRow(System.currentTimeMillis(), Phase.RESPONSE, ctx, reqId, method, url, status, p.isBlocked());
        }

        static EventRow fromAuth(WDNetworkEvent.AuthRequired.AuthRequiredParametersWD p) {
            String ctx = (p.getContext()!=null) ? p.getContext().value() : null;
            String reqId = (p.getRequest()!=null && p.getRequest().getRequest()!=null) ? p.getRequest().getRequest().value() : null;
            String method = p.getRequest()!=null ? p.getRequest().getMethod() : null;
            String url = p.getRequest()!=null ? p.getRequest().getUrl() : null;
            return new EventRow(System.currentTimeMillis(), Phase.AUTH, ctx, reqId, method, url, null, p.isBlocked());
        }
    }

    static final class EventTableModel extends AbstractTableModel {
        private final String[] cols = {"Zeit","Phase","Context","ReqId","Method","URL","Status","Blocked"};
        private final java.util.List<EventRow> rows = new CopyOnWriteArrayList<>();

        void add(EventRow r){ rows.add(r); int i=rows.size()-1; fireTableRowsInserted(i,i); }
        void clear(){ int n=rows.size(); rows.clear(); if(n>0) fireTableRowsDeleted(0,n-1); }
        EventRow get(int i){ return rows.get(i); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r, int c){
            EventRow e = rows.get(r);
            switch(c){
                case 0: return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(e.ts));
                case 1: return e.phase;
                case 2: return e.contextId;
                case 3: return e.requestId;
                case 4: return e.method;
                case 5: return e.url;
                case 6: return e.status;
                case 7: return e.blocked ? "yes" : "";
                default: return "";
            }
        }
        @Override public boolean isCellEditable(int r,int c){ return false; }
    }
}

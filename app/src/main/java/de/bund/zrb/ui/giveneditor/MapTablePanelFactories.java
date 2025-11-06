package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.service.TestRegistry;

import java.util.*;
import java.util.function.Supplier;

/** Provide suppliers for assertions editors (reuse logic of MapTablePanel). */
final class MapTablePanelFactories {

    private MapTablePanelFactories() {}

    static Supplier<List<String>> varSupplierForAssertions() {
        return new Supplier<List<String>>() {
            @Override public List<String> get() {
                Set<String> all = new LinkedHashSet<String>();
                de.bund.zrb.model.RootNode root = TestRegistry.getInstance().getRoot();
                if (root != null) {
                    if (root.getBeforeAll()   != null) all.addAll(root.getBeforeAll().keySet());
                    if (root.getBeforeEach()  != null) all.addAll(root.getBeforeEach().keySet());
                    if (root.getTemplates()   != null) all.addAll(root.getTemplates().keySet());
                    if (root.getAfterEach()   != null) all.addAll(root.getAfterEach().keySet());
                }
                List<de.bund.zrb.model.TestSuite> suites = TestRegistry.getInstance().getAll();
                if (suites != null) {
                    for (int si = 0; si < suites.size(); si++) {
                        de.bund.zrb.model.TestSuite s = suites.get(si);
                        if (s.getBeforeAll()   != null) all.addAll(s.getBeforeAll().keySet());
                        if (s.getBeforeEach()  != null) all.addAll(s.getBeforeEach().keySet());
                        if (s.getTemplates()   != null) all.addAll(s.getTemplates().keySet());
                        if (s.getAfterAll()    != null) all.addAll(s.getAfterAll().keySet());
                        List<de.bund.zrb.model.TestCase> cases = s.getTestCases();
                        if (cases != null) {
                            for (int ci = 0; ci < cases.size(); ci++) {
                                de.bund.zrb.model.TestCase tc = cases.get(ci);
                                if (tc.getBefore()    != null) all.addAll(tc.getBefore().keySet());
                                if (tc.getTemplates() != null) all.addAll(tc.getTemplates().keySet());
                                if (tc.getAfter()     != null) all.addAll(tc.getAfter().keySet());
                            }
                        }
                    }
                }
                List<String> sorted = new ArrayList<String>(all);
                Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                return sorted;
            }
        };
    }

    static Supplier<Map<String, DescribedItem>> fnSupplier() {
        return new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                ExpressionRegistryImpl reg = ExpressionRegistryImpl.getInstance();
                Set<String> keys = reg.getKeys();
                List<String> sorted = new ArrayList<String>(keys);
                Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < sorted.size(); i++) {
                    final String name = sorted.get(i);
                    ExpressionFunction builtin = reg.get(name);
                    if (builtin != null) { out.put(name, builtin); continue; }
                    final de.bund.zrb.expressions.domain.FunctionMetadata m = reg.getMetadata(name);
                    out.put(name, new DescribedItem() {
                        public String getDescription() { return m != null && m.getDescription() != null ? m.getDescription() : ""; }
                        public java.util.List<String> getParamNames() {
                            return m != null && m.getParameterNames() != null ? m.getParameterNames()
                                    : java.util.Collections.<String>emptyList();
                        }
                        public java.util.List<String> getParamDescriptions() {
                            return m != null && m.getParameterDescriptions() != null ? m.getParameterDescriptions()
                                    : java.util.Collections.<String>emptyList();
                        }
                        public Object getMetadata() { return m; }
                    });
                }
                return out;
            }
        };
    }

    static Supplier<Map<String, DescribedItem>> rxSupplier() {
        return new Supplier<Map<String, DescribedItem>>() {
            @Override public Map<String, DescribedItem> get() {
                Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                RegexPatternRegistry rx = RegexPatternRegistry.getInstance();
                List<String> titles = rx.getTitlePresets();
                for (int i = 0; i < titles.size(); i++) {
                    final String val = titles.get(i);
                    out.put(val, new de.bund.zrb.ui.celleditors.DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Title)"; }
                    });
                }
                List<String> msgs = rx.getMessagePresets();
                for (int i = 0; i < msgs.size(); i++) {
                    final String val = msgs.get(i);
                    out.put(val, new de.bund.zrb.ui.celleditors.DescribedItem() {
                        @Override public String getDescription() { return "Regex-Preset (Message)"; }
                    });
                }
                return out;
            }
        };
    }
}

package de.bund.zrb.webdriver.command.response;

import de.bund.zrb.markerInterfaces.WDResultData;
import de.bund.zrb.webdriver.type.browsingContext.WDInfo;
import de.bund.zrb.webdriver.type.script.WDRemoteValue;

import java.util.List;

public interface WDBrowsingContextResult extends WDResultData {
    class CaptureScreenshotResult implements WDBrowsingContextResult {
        private String data;

        public CaptureScreenshotResult(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            return "CaptureScreenshotResult{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }

    class CreateResult implements WDBrowsingContextResult {
        private String context;

        public CreateResult(String context) {
            this.context = context;
        }

        public String getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "CreateResult{" +
                    "context='" + context + '\'' +
                    '}';
        }
    }

    class GetTreeResult implements WDBrowsingContextResult {
        private List<WDInfo> contexts;

        public GetTreeResult(List<WDInfo> contexts) {
            this.contexts = contexts;
        }

        public List<WDInfo> getContexts() {
            return contexts;
        }

        @Override
        public String toString() {
            return "GetTreeResult{" +
                    "contexts=" + contexts +
                    '}';
        }
    }

    class LocateNodesResult implements WDBrowsingContextResult {
        private List<WDRemoteValue.NodeRemoteValue> nodes;

        public LocateNodesResult(List<WDRemoteValue.NodeRemoteValue> nodes) {
            this.nodes = nodes;
        }

        public List<WDRemoteValue.NodeRemoteValue> getNodes() {
            return nodes;
        }

        @Override
        public String toString() {
            return "LocateNodesResult{" +
                    "nodes=" + nodes +
                    '}';
        }
    }

    class NavigateResult implements WDBrowsingContextResult {
        private String navigation; // Optional: May be null either!
        private String url;

        public NavigateResult(String navigation, String url) {
            this.navigation = navigation;
            this.url = url;
        }

        public String getNavigation() {
            return navigation;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "NavigateResult{" +
                    "navigation='" + navigation + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    class PrintResult implements WDBrowsingContextResult {
        private String data;

        public PrintResult(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            return "PrintResult{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }

    /**
     * @deprecated since unclear documentation
     */
    @Deprecated
    class TraverseHistoryResult implements WDBrowsingContextResult {
        private final String navigation;
        private final List<HistoryEntry> entries;
        private final int currentEntryIndex;

        public TraverseHistoryResult(String navigation, List<HistoryEntry> entries, int currentEntryIndex) {
            this.navigation = navigation;
            this.entries = entries;
            this.currentEntryIndex = currentEntryIndex;
        }

        public String getNavigation() {
            return navigation;
        }

        public List<HistoryEntry> getEntries() {
            return entries;
        }

        public int getCurrentEntryIndex() {
            return currentEntryIndex;
        }

        @Override
        public String toString() {
            return "TraverseHistoryResult{" +
                    "navigation='" + navigation + '\'' +
                    ", entries=" + entries +
                    ", currentEntryIndex=" + currentEntryIndex +
                    '}';
        }

        // Inner class for history entries
        public static class HistoryEntry {
            private final String url;
            private final boolean isActive;

            public HistoryEntry(String url, boolean isActive) {
                this.url = url;
                this.isActive = isActive;
            }

            public String getUrl() {
                return url;
            }

            public boolean isActive() {
                return isActive;
            }

            @Override
            public String toString() {
                return "HistoryEntry{" +
                        "url='" + url + '\'' +
                        ", isActive=" + isActive +
                        '}';
            }
        }
    }
}

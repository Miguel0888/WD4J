package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.browsingContext.WDInfo;
import wd4j.impl.webdriver.type.script.WDRemoteValue;

import java.util.List;

public interface WDBrowsingContextResult extends WDResultData {
    class CaptureScreenshotWDBrowsingContextResult implements WDBrowsingContextResult {
        private String data;

        public CaptureScreenshotWDBrowsingContextResult(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "CaptureScreenshotResult{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }

    class CreateWDBrowsingContextResult implements WDBrowsingContextResult {
        private String context;

        public CreateWDBrowsingContextResult(String context) {
            this.context = context;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        @Override
        public String toString() {
            return "CreateResult{" +
                    "context='" + context + '\'' +
                    '}';
        }
    }

    class GetTreeWDBrowsingContextResult implements WDBrowsingContextResult {
        private List<WDInfo> contexts;

        public GetTreeWDBrowsingContextResult(List<WDInfo> contexts) {
            this.contexts = contexts;
        }

        public List<WDInfo> getContexts() {
            return contexts;
        }

        public void setContexts(List<WDInfo> contexts) {
            this.contexts = contexts;
        }

        @Override
        public String toString() {
            return "GetTreeResult{" +
                    "contexts=" + contexts +
                    '}';
        }
    }

    class LocateNodesWDBrowsingContextResult implements WDBrowsingContextResult {
        private List<WDRemoteValue.NodeWDRemoteValue> nodes;

        public LocateNodesWDBrowsingContextResult(List<WDRemoteValue.NodeWDRemoteValue> nodes) {
            this.nodes = nodes;
        }

        public List<WDRemoteValue.NodeWDRemoteValue> getNodes() {
            return nodes;
        }

        public void setNodes(List<WDRemoteValue.NodeWDRemoteValue> nodes) {
            this.nodes = nodes;
        }

        @Override
        public String toString() {
            return "LocateNodesResult{" +
                    "nodes=" + nodes +
                    '}';
        }
    }

    class NavigateWDBrowsingContextResult implements WDBrowsingContextResult {
        private String navigation;
        private String url;

        public NavigateWDBrowsingContextResult(String navigation, String url) {
            this.navigation = navigation;
            this.url = url;
        }

        public String getNavigation() {
            return navigation;
        }

        public void setNavigation(String navigation) {
            this.navigation = navigation;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "NavigateResult{" +
                    "navigation='" + navigation + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    class PrintWDBrowsingContextResult implements WDBrowsingContextResult {
        private String data;

        public PrintWDBrowsingContextResult(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "PrintResult{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }

    class TraverseHistoryWDBrowsingContextResult implements WDBrowsingContextResult {
        @Override
        public String toString() {
            return "TraverseHistoryResult{}";
        }
    }
}

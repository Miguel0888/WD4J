package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.browsingContext.Info;
import wd4j.impl.webdriver.type.script.RemoteValue;

import java.util.List;

public interface BrowsingContextResult extends ResultData {
    class CaptureScreenshotResult implements BrowsingContextResult {
        private String data;

        public CaptureScreenshotResult(String data) {
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

    class CreateResult implements BrowsingContextResult {
        private String context;

        public CreateResult(String context) {
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

    class GetTreeResult implements BrowsingContextResult {
        private List<Info> contexts;

        public GetTreeResult(List<Info> contexts) {
            this.contexts = contexts;
        }

        public List<Info> getContexts() {
            return contexts;
        }

        public void setContexts(List<Info> contexts) {
            this.contexts = contexts;
        }

        @Override
        public String toString() {
            return "GetTreeResult{" +
                    "contexts=" + contexts +
                    '}';
        }
    }

    class LocateNodesResult implements BrowsingContextResult {
        private List<RemoteValue.NodeRemoteValue> nodes;

        public LocateNodesResult(List<RemoteValue.NodeRemoteValue> nodes) {
            this.nodes = nodes;
        }

        public List<RemoteValue.NodeRemoteValue> getNodes() {
            return nodes;
        }

        public void setNodes(List<RemoteValue.NodeRemoteValue> nodes) {
            this.nodes = nodes;
        }

        @Override
        public String toString() {
            return "LocateNodesResult{" +
                    "nodes=" + nodes +
                    '}';
        }
    }

    class NavigateResult implements BrowsingContextResult {
        private String navigation;
        private String url;

        public NavigateResult(String navigation, String url) {
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

    class PrintResult implements BrowsingContextResult {
        private String data;

        public PrintResult(String data) {
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

    class TraverseHistoryResult implements BrowsingContextResult {
        @Override
        public String toString() {
            return "TraverseHistoryResult{}";
        }
    }
}

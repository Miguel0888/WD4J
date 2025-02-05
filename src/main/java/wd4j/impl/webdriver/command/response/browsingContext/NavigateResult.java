package wd4j.impl.webdriver.command.response.browsingContext;

import wd4j.impl.markerInterfaces.resultData.BrowsingContextResult;

public class NavigateResult implements BrowsingContextResult {
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
package wd4j.impl.webdriver.type.network;

public class UrlPatternPattern implements UrlPattern{
    private final String type = "pattern";
    private final String protocol; // optional
    private final String hostnames; // optional
    private final String port; // optional
    private final String pathname; // optional
    private final String search; // optional

    public UrlPatternPattern() {
        this.protocol = null;
        this.hostnames = null;
        this.port = null;
        this.pathname = null;
        this.search = null;
    }

    public UrlPatternPattern(String protocol, String hostnames, String port, String pathname, String search) {
        this.protocol = protocol;
        this.hostnames = hostnames;
        this.port = port;
        this.pathname = pathname;
        this.search = search;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHostnames() {
        return hostnames;
    }

    public String getPort() {
        return port;
    }

    public String getPathname() {
        return pathname;
    }

    public String getSearch() {
        return search;
    }
}

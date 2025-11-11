package de.bund.zrb.service;

public class BrowserConfig {
    private String browserType; // z.B. "Chrome"
    private int port;
    private boolean useProfile;
    private String profilePath;
    private boolean headless;
    private boolean disableGpu;
    private boolean noRemote;
    private boolean startMaximized;
    private String extraArgs; // zusätzliche Startargumente (space-separiert)

    // Getter und Setter
    public String getBrowserType() {
        return browserType;
    }

    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isUseProfile() {
        return useProfile;
    }

    public void setUseProfile(boolean useProfile) {
        this.useProfile = useProfile;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isDisableGpu() {
        return disableGpu;
    }

    public void setDisableGpu(boolean disableGpu) {
        this.disableGpu = disableGpu;
    }

    public boolean isNoRemote() {
        return noRemote;
    }

    public void setNoRemote(boolean noRemote) {
        this.noRemote = noRemote;
    }

    public boolean isStartMaximized() {
        return startMaximized;
    }

    public void setStartMaximized(boolean startMaximized) {
        this.startMaximized = startMaximized;
    }

    public String getExtraArgs() {
        return extraArgs;
    }

    public void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
    }
}

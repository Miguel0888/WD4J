package wd4j.impl.modules;

public class Browser implements Module {

    public ClientWindow clientWindow;
    public ClientWindowInfo clientWindowInfo;
    public UserContext userContext;
    public UserContextInfo userContextInfo;

    public void close()
    {}
    public void createUserContext()
    {}
    public void getClientWindows()
    {}
    public void getUserContexts()
    {}
    public void removeUserContext()
    {}
    public void setClientWindowState()
    {}

    public static class ClientWindow implements Type {
        // ToDo
    }
    public static class ClientWindowInfo implements Type {
        // ToDo
    }
    public static class UserContext implements Type {
        // ToDo
    }
    public static class UserContextInfo implements Type {
        // ToDo
    }
}


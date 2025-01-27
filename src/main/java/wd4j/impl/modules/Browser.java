package wd4j.impl.modules;

import wd4j.core.CommandImpl;
import wd4j.impl.generic.Module;
import wd4j.impl.generic.Type;

public class Browser implements Module {

    public ClientWindow clientWindow;
    public ClientWindowInfo clientWindowInfo;
    public UserContext userContext;
    public UserContextInfo userContextInfo;

    public void close() {
    }

    public void createUserContext() {
    }

    public void getClientWindows() {
    }

    public void getUserContexts() {
    }

    public void removeUserContext() {
    }

    public void setClientWindowState() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Types (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Close extends CommandImpl<Browser.Close.ParamsImpl> {

    }

    public static class CreateUserContext  extends CommandImpl<Browser.CreateUserContext.ParamsImpl> {

    }

    public static class GetClientWindows   extends CommandImpl<Browser.GetClientWindows.ParamsImpl> {

    }

    public static class RemoveUserContext    extends CommandImpl<Browser.RemoveUserContext .ParamsImpl> {

    }

    public static class SetClientWindowState    extends CommandImpl<Browser.SetClientWindowState .ParamsImpl> {

    }


}


package wd4j.impl.modules;

import wd4j.core.CommandImpl;
import wd4j.impl.generic.Module;

public class Network implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddIntercept extends CommandImpl<Network.AddIntercept.ParamsImpl> {

    }

    public static class ContinueRequest extends CommandImpl<Network.ContinueRequest.ParamsImpl> {

    }

    public static class ContinueResponse extends CommandImpl<Network.ContinueResponse.ParamsImpl> {

    }

    public static class ContinueWithAuth extends CommandImpl<Network.ContinueWithAuth.ParamsImpl> {

    }

    public static class FailRequest extends CommandImpl<Network.FailRequest.ParamsImpl> {

    }

    public static class ProvideResponse extends CommandImpl<Network.ProvideResponse.ParamsImpl> {

    }

    public static class RemoveIntercept extends CommandImpl<Network.RemoveIntercept.ParamsImpl> {

    }

    public static class SetCacheBehavior extends CommandImpl<Network.SetCacheBehavior.ParamsImpl> {

    }

}
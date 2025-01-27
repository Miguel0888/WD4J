package wd4j.impl.modules;

import wd4j.core.CommandImpl;
import wd4j.impl.generic.Module;

public class Script implements Module {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class AddPreloadScript extends CommandImpl<Script.AddPreloadScript.ParamsImpl> {

    }

    public static class Disown extends CommandImpl<Script.Disown.ParamsImpl> {

    }

    public static class CallFunction extends CommandImpl<Script.CallFunction.ParamsImpl> {

    }

    public static class Evaluate extends CommandImpl<Script.Evaluate.ParamsImpl> {

    }

    public static class GetRealms extends CommandImpl<Script.GetRealms.ParamsImpl> {

    }

    public static class RemovePreloadScript extends CommandImpl<Script.RemovePreloadScript.ParamsImpl> {

    }

}
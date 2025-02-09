package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.type.script.PreloadScript;
import wd4j.impl.webdriver.type.script.RealmInfo;

import java.util.List;

public interface ScriptResult extends ResultData {
    class AddPreloadScritpResult implements ScriptResult {
        private PreloadScript script;

        public AddPreloadScritpResult(PreloadScript script) {
            this.script = script;
        }

        public PreloadScript getScript() {
            return script;
        }
    }

    class GetRealmsResult {
        private List<RealmInfo> realms;

        public GetRealmsResult(List<RealmInfo> realms) {
            this.realms = realms;
        }

        public List<RealmInfo> getRealms() {
            return realms;
        }
    }
}

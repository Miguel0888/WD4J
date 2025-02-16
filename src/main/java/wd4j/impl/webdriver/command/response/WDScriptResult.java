package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.type.script.WDPreloadScript;
import wd4j.impl.webdriver.type.script.WDRealmInfo;

import java.util.List;

public interface WDScriptResult extends WDResultData {
    class AddPreloadScritpWDScriptResult implements WDScriptResult {
        private WDPreloadScript script;

        public AddPreloadScritpWDScriptResult(WDPreloadScript script) {
            this.script = script;
        }

        public WDPreloadScript getScript() {
            return script;
        }
    }

    class GetRealmsResult {
        private List<WDRealmInfo> realms;

        public GetRealmsResult(List<WDRealmInfo> realms) {
            this.realms = realms;
        }

        public List<WDRealmInfo> getRealms() {
            return realms;
        }
    }
}

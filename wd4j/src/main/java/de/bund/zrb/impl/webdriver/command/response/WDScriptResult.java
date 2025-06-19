package de.bund.zrb.impl.webdriver.command.response;

import de.bund.zrb.impl.markerInterfaces.WDResultData;
import de.bund.zrb.impl.webdriver.type.script.WDPreloadScript;
import de.bund.zrb.impl.webdriver.type.script.WDRealmInfo;

import java.util.List;

public interface WDScriptResult extends WDResultData {
    class AddPreloadScriptResult implements WDScriptResult {
        private WDPreloadScript script;

        public AddPreloadScriptResult(WDPreloadScript script) {
            this.script = script;
        }

        public WDPreloadScript getScript() {
            return script;
        }

        @Override
        public String toString() {
            return "AddPreloadScriptResult{" +
                    "script=" + script +
                    '}';
        }
    }

    class GetRealmsResult implements WDScriptResult {
        private final List<WDRealmInfo> realms;

        public GetRealmsResult(List<WDRealmInfo> realms) {
            this.realms = realms;
        }

        public List<WDRealmInfo> getRealms() {
            return realms;
        }

        @Override
        public String toString() {
            return "GetRealmsResult{" +
                    "realms=" + realms +
                    '}';
        }
    }
}

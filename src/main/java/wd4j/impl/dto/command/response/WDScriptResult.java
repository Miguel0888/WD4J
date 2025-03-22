package wd4j.impl.dto.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.dto.type.script.WDPreloadScript;
import wd4j.impl.dto.type.script.WDRealmInfo;

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

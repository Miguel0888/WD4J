package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;
import wd4j.impl.webdriver.type.network.WDCookie;

import java.util.List;

public interface WDStorageResult extends WDResultData {
    class DeleteCookiesWDStorageResult implements WDStorageResult {
        private final SetCookieParameters.PartitionKey partitionKey;

        public DeleteCookiesWDStorageResult(SetCookieParameters.PartitionKey partitionKey) {
            this.partitionKey = partitionKey;
        }

        public SetCookieParameters.PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }

    class GetCookieWDStorageResult implements WDStorageResult {
        List<WDCookie> WDCookies;
        SetCookieParameters.PartitionKey partitionKey;

        public GetCookieWDStorageResult(List<WDCookie> WDCookies, SetCookieParameters.PartitionKey partitionKey) {
            this.WDCookies = WDCookies;
            this.partitionKey = partitionKey;
        }

        public List<WDCookie> getCookies() {
            return WDCookies;
        }

        public SetCookieParameters.PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }
}

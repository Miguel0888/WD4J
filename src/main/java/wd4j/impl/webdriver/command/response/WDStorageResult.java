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
        List<WDCookie> cookies;
        SetCookieParameters.PartitionKey partitionKey;

        public GetCookieWDStorageResult(List<WDCookie> cookies, SetCookieParameters.PartitionKey partitionKey) {
            this.cookies = cookies;
            this.partitionKey = partitionKey;
        }

        public List<WDCookie> getCookies() {
            return cookies;
        }

        public SetCookieParameters.PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }
}

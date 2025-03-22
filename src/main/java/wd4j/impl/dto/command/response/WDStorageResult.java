package wd4j.impl.dto.command.response;

import wd4j.impl.markerInterfaces.WDResultData;
import wd4j.impl.dto.command.request.parameters.storage.SetCookieParameters;
import wd4j.impl.dto.type.network.WDCookie;

import java.util.List;

public interface WDStorageResult extends WDResultData {

    class GetCookieResult implements WDStorageResult {
        List<WDCookie> cookies;
        SetCookieParameters.PartitionKey partitionKey;

        public GetCookieResult(List<WDCookie> cookies, SetCookieParameters.PartitionKey partitionKey) {
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

    class SetCookieResult implements WDStorageResult {
        private final SetCookieParameters.PartitionKey partitionKey;

        public SetCookieResult(SetCookieParameters.PartitionKey partitionKey) {
            this.partitionKey = partitionKey;
        }

        public SetCookieParameters.PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }

    class DeleteCookiesResult implements WDStorageResult {
        private final SetCookieParameters.PartitionKey partitionKey;

        public DeleteCookiesResult(SetCookieParameters.PartitionKey partitionKey) {
            this.partitionKey = partitionKey;
        }

        public SetCookieParameters.PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }
}

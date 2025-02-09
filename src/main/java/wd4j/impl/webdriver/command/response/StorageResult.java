package wd4j.impl.webdriver.command.response;

import wd4j.impl.markerInterfaces.ResultData;
import wd4j.impl.webdriver.command.request.parameters.storage.PartitionKey;
import wd4j.impl.webdriver.type.network.Cookie;

import java.util.List;

public interface StorageResult extends ResultData {
    class DeleteCookiesResult implements StorageResult {
        private final PartitionKey partitionKey;

        public DeleteCookiesResult(PartitionKey partitionKey) {
            this.partitionKey = partitionKey;
        }

        public PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }

    class GetCookieResult implements StorageResult {
        List<Cookie> cookies;
        PartitionKey partitionKey;

        public GetCookieResult(List<Cookie> cookies, PartitionKey partitionKey) {
            this.cookies = cookies;
            this.partitionKey = partitionKey;
        }

        public List<Cookie> getCookies() {
            return cookies;
        }

        public PartitionKey getPartitionKey() {
            return partitionKey;
        }
    }
}

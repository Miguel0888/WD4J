package wd4j.impl.webdriver.command.response.storage;

import wd4j.impl.markerInterfaces.resultData.StorageResult;
import wd4j.impl.webdriver.type.network.Cookie;
import wd4j.impl.webdriver.command.request.storage.parameters.PartitionKey;

import java.util.List;

public class GetCookieResult implements StorageResult {
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

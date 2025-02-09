package wd4j.impl.webdriver.command.response.storage;

import wd4j.impl.markerInterfaces.resultData.StorageResult;
import wd4j.impl.webdriver.command.request.storage.parameters.PartitionKey;

public class DeleteCookiesResult implements StorageResult {
    private final PartitionKey partitionKey;

    public DeleteCookiesResult(PartitionKey partitionKey) {
        this.partitionKey = partitionKey;
    }

    public PartitionKey getPartitionKey() {
        return partitionKey;
    }
}

package wd4j.impl.webdriver.command.response.script;

import wd4j.impl.webdriver.type.script.RealmInfo;

import java.util.List;

public class GetRealmsResult {
    private List<RealmInfo> realms;

    public GetRealmsResult(List<RealmInfo> realms) {
        this.realms = realms;
    }

    public List<RealmInfo> getRealms() {
        return realms;
    }
}

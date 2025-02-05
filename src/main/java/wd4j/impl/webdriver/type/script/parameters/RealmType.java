package wd4j.impl.webdriver.type.script.parameters;

import wd4j.impl.webdriver.type.script.PreloadScript;
import wd4j.impl.webdriver.type.script.RealmInfo;

import java.util.List;

public  class RealmType {
    public final List<RealmInfo> realms;

    public RealmType(List<RealmInfo> realms) {
        this.realms = realms;
    }

    public List<RealmInfo> getRealms() {
        return realms;
    }
}
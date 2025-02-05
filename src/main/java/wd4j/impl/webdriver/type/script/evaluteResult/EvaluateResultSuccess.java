package wd4j.impl.webdriver.type.script.evaluteResult;

import wd4j.impl.webdriver.type.script.EvaluateResult;
import wd4j.impl.webdriver.type.script.Realm;
import wd4j.impl.webdriver.type.script.RemoteValue;

public class EvaluateResultSuccess extends EvaluateResult {
    private RemoteValue result;

    public EvaluateResultSuccess(String type, RemoteValue result, Realm realm) {
        super(type, realm);
        this.result = result;
    }

    public RemoteValue getResult() {
        return result;
    }

}

package wd4j.impl.webdriver.type.script.evaluteResult;

import wd4j.impl.webdriver.type.script.EvaluateResult;
import wd4j.impl.webdriver.type.script.ExceptionDetails;
import wd4j.impl.webdriver.type.script.Realm;
import wd4j.impl.webdriver.type.script.RemoteValue;

public class EvaluateResultException extends EvaluateResult {
    private RemoteValue result;
    private ExceptionDetails exceptionDetails;

    public EvaluateResultException(String type, ExceptionDetails exceptionDetails, Realm realm) {
        super(type, realm);
        this.exceptionDetails = exceptionDetails;
    }

    public RemoteValue getResult() {
        return result;
    }

}

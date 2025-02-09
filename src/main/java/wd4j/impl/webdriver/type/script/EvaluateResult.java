package wd4j.impl.webdriver.type.script;

public interface EvaluateResult {
    String getType();


    class EvaluateResultSuccess implements EvaluateResult {
        private final String type = "success";
        private final RemoteValue result;
        private final Realm realm;

        public EvaluateResultSuccess(RemoteValue result, Realm realm) {
            this.result = result;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public RemoteValue getResult() {
            return result;
        }

        public Realm getRealm() {
            return realm;
        }
    }

    class EvaluateResultError implements EvaluateResult {
        private final String type = "exception";
        private final ExceptionDetails exceptionDetails;
        private final Realm realm;

        public EvaluateResultError(ExceptionDetails exceptionDetails, Realm realm) {
            this.exceptionDetails = exceptionDetails;
            this.realm = realm;
        }

        @Override
        public String getType() {
            return type;
        }

        public ExceptionDetails getExceptionDetails() {
            return exceptionDetails;
        }

        public Realm getRealm() {
            return realm;
        }
    }

}
package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;
import wd4j.impl.webdriver.type.network.Request;

public class ContinueWithAuthNoCredentials extends ContinueWithAuthParameters
{
    private final Action action;

    public ContinueWithAuthNoCredentials(Request request, Action action) {
        super(request);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public enum Action implements ContinueWithAuthParameters.Action, EnumWrapper {
        DEFAULT( "default" ),
        CANCEL( "cancel" );

        private final String value;

        Action( String value ) {
            this.value = value;
        }

        @Override // confirmed design
        public String value() {
            return value;
        }
    }
}

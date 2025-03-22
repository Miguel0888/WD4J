package wd4j.impl.dto.command.request.parameters.network;

import wd4j.impl.dto.mapping.EnumWrapper;
import wd4j.impl.dto.type.network.WDRequest;

public class ContinueWithAuthNoCredentials extends ContinueWithAuthParameters
{
    private final Action action;

    public ContinueWithAuthNoCredentials(WDRequest WDRequest, Action action) {
        super(WDRequest);
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

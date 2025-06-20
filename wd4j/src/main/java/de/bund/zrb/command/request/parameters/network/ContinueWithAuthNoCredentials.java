package de.bund.zrb.command.request.parameters.network;

import de.bund.zrb.support.mapping.EnumWrapper;
import de.bund.zrb.type.network.WDRequest;

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

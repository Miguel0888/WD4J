package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.storage.DeleteCookiesParameters;
import wd4j.impl.webdriver.command.request.parameters.storage.GetCookiesParameters;
import wd4j.impl.webdriver.command.request.parameters.storage.SetCookieParameters;

public class StorageRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class GetCookies extends CommandImpl<GetCookiesParameters> implements CommandData {
        public GetCookies(String contextId) {
            super("storage.getCookies", new GetCookiesParameters(contextId));
        }
    }

    public static class SetCookie extends CommandImpl<SetCookieParameters> implements CommandData {
        public SetCookie(String contextId, String name, String value) {
            super("storage.setCookie", new SetCookieParameters(contextId, name, value));
        }
    }

    public static class DeleteCookies extends CommandImpl<DeleteCookiesParameters> implements CommandData {
        public DeleteCookies(String contextId, String name) {
            super("storage.deleteCookies", new DeleteCookiesParameters(contextId, name));
        }
    }

}
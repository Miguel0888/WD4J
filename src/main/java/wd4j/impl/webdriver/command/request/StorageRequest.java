package wd4j.impl.webdriver.command.request;

import wd4j.impl.markerInterfaces.CommandData;
import wd4j.impl.webdriver.command.request.helper.CommandImpl;
import wd4j.impl.webdriver.command.request.parameters.storage.*;
import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

public class StorageRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The storage.getCookies command retrieves zero or more cookies which match a set of provided parameters.
     */
    public static class GetCookies extends CommandImpl<GetCookiesParameters> implements CommandData {
        public GetCookies(String contextId) {
            super("storage.getCookies", new GetCookiesParameters(new PartitionDescriptor.BrowsingContextPartitionDescriptor(
                    new BrowsingContext(contextId)
            )));
        }
        public GetCookies(BrowsingContext context) {
            super("storage.getCookies", new GetCookiesParameters(new PartitionDescriptor.BrowsingContextPartitionDescriptor(context)));
        }
        public GetCookies(CookieFilter filter) {
            super("storage.getCookies", new GetCookiesParameters(filter));
        }
        public GetCookies(PartitionDescriptor partition) {
            super("storage.getCookies", new GetCookiesParameters(partition));
        }
        public GetCookies(CookieFilter filter, PartitionDescriptor partition) {
            super("storage.getCookies", new GetCookiesParameters(filter, partition));
        }
    }

    /**
     * The storage.setCookie command creates a new cookie in a cookie store, replacing any cookie in that store which
     * matches according to {@link https://httpwg.org/specs/rfc6265.html [COOKIES]}.
     */
    public static class SetCookie extends CommandImpl<SetCookieParameters> implements CommandData {
        public SetCookie(String contextId, SetCookieParameters.PartialCookie cookie) {
            super("storage.setCookie", new SetCookieParameters(cookie,
                    new PartitionDescriptor.BrowsingContextPartitionDescriptor(new BrowsingContext(contextId))));
        }
        public SetCookie(SetCookieParameters.PartialCookie cookie, PartitionDescriptor partition) {
            super("storage.setCookie", new SetCookieParameters(cookie, partition));
        }
    }

    /**
     * The storage.deleteCookies command removes zero or more cookies which match a set of provided parameters.
     */
    public static class DeleteCookies extends CommandImpl<DeleteCookiesParameters> implements CommandData {
        public DeleteCookies(String contextId, CookieFilter filter) {
            super("storage.deleteCookies", new DeleteCookiesParameters(filter,
                    new PartitionDescriptor.BrowsingContextPartitionDescriptor(new BrowsingContext(contextId))));
        }
        public DeleteCookies(CookieFilter filter, PartitionDescriptor partition) {
            super("storage.deleteCookies", new DeleteCookiesParameters(filter, partition));
        }
    }

}
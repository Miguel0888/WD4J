package wd4j.impl.dto.command.request;

import wd4j.impl.markerInterfaces.WDCommandData;
import wd4j.impl.dto.command.request.helper.WDCommandImpl;
import wd4j.impl.dto.command.request.parameters.storage.*;
import wd4j.impl.dto.type.browsingContext.WDBrowsingContext;

public class WDStorageRequest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The storage.getCookies command retrieves zero or more cookies which match a set of provided parameters.
     */
    public static class GetCookies extends WDCommandImpl<GetCookiesParameters> implements WDCommandData {
        public GetCookies(String contextId) {
            super("storage.getCookies", new GetCookiesParameters(new PartitionDescriptor.BrowsingContextPartitionDescriptor(
                    new WDBrowsingContext(contextId)
            )));
        }
        public GetCookies(WDBrowsingContext context) {
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
    public static class SetCookie extends WDCommandImpl<SetCookieParameters> implements WDCommandData {
        public SetCookie(String contextId, SetCookieParameters.PartialCookie cookie) {
            super("storage.setCookie", new SetCookieParameters(cookie,
                    new PartitionDescriptor.BrowsingContextPartitionDescriptor(new WDBrowsingContext(contextId))));
        }
        public SetCookie(SetCookieParameters.PartialCookie cookie, PartitionDescriptor partition) {
            super("storage.setCookie", new SetCookieParameters(cookie, partition));
        }
    }

    /**
     * The storage.deleteCookies command removes zero or more cookies which match a set of provided parameters.
     */
    public static class DeleteCookies extends WDCommandImpl<DeleteCookiesParameters> implements WDCommandData {
        public DeleteCookies(String contextId, CookieFilter filter) {
            super("storage.deleteCookies", new DeleteCookiesParameters(filter,
                    new PartitionDescriptor.BrowsingContextPartitionDescriptor(new WDBrowsingContext(contextId))));
        }
        public DeleteCookies(CookieFilter filter, PartitionDescriptor partition) {
            super("storage.deleteCookies", new DeleteCookiesParameters(filter, partition));
        }
    }

}
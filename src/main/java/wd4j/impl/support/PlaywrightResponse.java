package wd4j.impl.support;

import com.microsoft.Frame;
import com.microsoft.Request;
import com.microsoft.Response;
import com.microsoft.options.HttpHeader;
import com.microsoft.options.SecurityDetails;
import com.microsoft.options.ServerAddr;
import wd4j.impl.markerInterfaces.WDResultData;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generische Wrapper-Klasse für WebDriver BiDi Responses im Playwright Response-Format.
 *
 * @param <T> Der konkrete WebDriver BiDi Response-Typ, der gewrappt wird.
 */
@Deprecated // since all responses are mapped on WebDriver BiDi result types. Thus, this class is just a wrapper!
public class PlaywrightResponse<T extends WDResultData> implements Response {
    private final T resultData;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Playwright Response Interface, not implemented
//    private final String url = null;
//    private final int status = 0;
//    private final String statusText  = null;
//    private final Map<String, String> headers  = null;
//    private final byte[] body = null;
//    private final Request request = null;
//    private final Frame frame = null;
//    private final SecurityDetails securityDetails = null;
//    private final ServerAddr serverAddr = null;
//    private final boolean fromServiceWorker = false;
//    private final boolean ok = true;
//    private final List<HttpHeader> headersArray = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public PlaywrightResponse(T resultData) {
        this.resultData = resultData;
    }

    @Override
    public Map<String, String> allHeaders() {
        return Collections.emptyMap(); // WebDriver BiDi liefert keine HTTP-Header zurück
    }

    @Override
    public byte[] body() {
        return new byte[0]; // WebDriver BiDi gibt keine direkte Response-Body zurück
    }

    @Override
    public String finished() {
        return null; // WebDriver BiDi verfolgt das nicht explizit
    }

    @Override
    public Frame frame() {
        return null; // WebDriver BiDi liefert keine Frame-Informationen
    }

    @Override
    public boolean fromServiceWorker() {
        return false; // WebDriver BiDi gibt keine Info über Service Worker
    }

    @Override
    public Map<String, String> headers() {
        return Collections.emptyMap(); // Keine Header verfügbar
    }

    @Override
    public List<HttpHeader> headersArray() {
        return Collections.emptyList();
    }

    @Override
    public String headerValue(String name) {
        return "";
    }

    @Override
    public List<String> headerValues(String name) {
        return Collections.emptyList(); // Keine Header verfügbar
    }

    @Override
    public boolean ok() {
        return resultData != null; // Solange `resultData` nicht null ist, war der Request erfolgreich
    }

    @Override
    public Request request() {
        return null;
    }

    @Override
    public SecurityDetails securityDetails() {
        return null;
    }

    @Override
    public ServerAddr serverAddr() {
        return null;
    }

    @Override
    public long status() {
        return 200; // WebDriver BiDi gibt keinen HTTP-Status zurück, daher Default-Wert
    }

    @Override
    public String statusText() {
        return "OK"; // Standard-Status
    }

    @Override
    public String url() {
        if (resultData instanceof wd4j.impl.webdriver.command.response.WDBrowsingContextResult.NavigateResult) {
            return ((wd4j.impl.webdriver.command.response.WDBrowsingContextResult.NavigateResult) resultData).getUrl();
        }
        return ""; // Falls kein URL-Feld existiert, leere String zurückgeben
    }

    @Override
    public String text() {
        return resultData.toString(); // Default: `toString()` des DTOs nutzen
    }

    public T getResultData() {
        return resultData;
    }

    @Override
    public String toString() {
        return "PlaywrightResponse{" +
                "resultData=" + resultData +
                '}';
    }
}

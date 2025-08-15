package de.bund.zrb.meta;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/** Format meta events into a single log line for the UI. */
public final class MetaEventFormatter {

    private static final ThreadLocal<SimpleDateFormat> TS =
            new ThreadLocal<SimpleDateFormat>() {
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("HH:mm:ss.SSS");
                }
            };

    private MetaEventFormatter() { }

    public static String format(MetaEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(TS.get().format(new Date(event.getTimestampMillis()))).append("] ");
        sb.append(event.getKind().name());

        Map<String, String> d = event.getDetails();
        if (d != null && !d.isEmpty()) {
            sb.append(" { ");
            boolean first = true;
            for (Map.Entry<String, String> e : d.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
            sb.append(" }");
        }
        return sb.toString();
    }
}

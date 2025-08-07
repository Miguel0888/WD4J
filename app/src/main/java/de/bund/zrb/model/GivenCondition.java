package de.bund.zrb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GivenCondition {
    private String type;
    private String value;

    public GivenCondition() {}
    public GivenCondition(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    @Override
    public String toString() {
        return "Given[" + (type != null ? type : "unbekannt") + "]";
    }

    /**
     * Parses `value` als key=value&... Map
     */
    public Map<String, Object> getParameterMap() {
        if (value == null || value.trim().isEmpty()) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        String[] pairs = value.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    /**
     * Serialisiert die Parameter-Map zurück ins key=value&... Format.
     */
    public void setParameterMap(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue().toString() : "");
        }
        this.value = sb.toString();
    }
}


package de.bund.zrb.api;

import com.google.gson.JsonObject;
import java.util.function.BiFunction;

@FunctionalInterface
public interface EventMapper extends BiFunction<String, JsonObject, Object> {
    // Already extends BiFunction<String, JsonObject, Object>
}
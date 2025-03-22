package wd4j.impl.support;

import com.google.gson.JsonObject;
import java.util.function.BiFunction;

@FunctionalInterface
public interface EventMapper extends BiFunction<String, JsonObject, Object> {
    // Already extends BiFunction<String, JsonObject, Object>
}
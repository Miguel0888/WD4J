package de.bund.zrb.support.mapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.reflections.Reflections;

import java.util.Set;

public class GsonMapperFactory {

    private static final Gson GSON_INSTANCE = createGson();
    public static final String PACKAGE = "wd4j.impl.webdriver";

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        registerAllWrappers(builder);
        return builder.create(); // IMPORTANT: Do not serialize null values, they mark optional parameters!
    }

    private static void registerAllWrappers(GsonBuilder builder) {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Register new Wrapper Interfaces, here:
        registerStringWrappers(builder);
        registerEnumWrappers(builder);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private static void registerStringWrappers(GsonBuilder builder) {
        Reflections reflections = new Reflections(PACKAGE); // Paketnamen ggf. anpassen
        Set<Class<? extends StringWrapper>> stringWrapperClasses = reflections.getSubTypesOf(StringWrapper.class);

        for (Class<? extends StringWrapper> clazz : stringWrapperClasses) {
            builder.registerTypeAdapter(clazz, new StringWrapperAdapter<>(clazz));
        }
    }

    private static void registerEnumWrappers(GsonBuilder builder) {
        Reflections reflections = new Reflections(PACKAGE); // Paketnamen ggf. anpassen
        Set<Class<? extends EnumWrapper>> enumWrapperClasses = reflections.getSubTypesOf(EnumWrapper.class);

        for (Class<? extends EnumWrapper> clazz : enumWrapperClasses) {
            builder.registerTypeAdapter(clazz, new EnumWrapperAdapter<>(clazz));
        }
    }

    public static Gson getGson() {
        return GSON_INSTANCE;
    }
}

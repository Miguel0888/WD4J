package wd4j.impl.support;

import app.Main;
import app.controller.MainController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ScriptHelper {
    public static String loadScript(String resourcePath) {
        try (InputStream inputStream = ScriptHelper.class.getClassLoader().getResourceAsStream(resourcePath);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8); // ✅ UTF-8 explizit setzen
             BufferedReader reader = new BufferedReader(streamReader)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Script file not found: " + resourcePath);
            }
            return reader.lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("Error loading script file: " + resourcePath, e);
        }
    }
}


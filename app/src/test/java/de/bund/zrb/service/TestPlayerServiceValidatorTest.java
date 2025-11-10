package de.bund.zrb.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für die interne Validator-Logik in TestPlayerService.
 * Wir testen die private Methode validateValue(..) über Reflection,
 * um ohne öffentliche API eine schnelle Regression-Sicherheit zu bekommen.
 */
public class TestPlayerServiceValidatorTest {

    private final TestPlayerService svc = TestPlayerService.getInstance();

    private boolean invoke(String type, String expected, String actual) {
        try {
            Method m = TestPlayerService.class.getDeclaredMethod("validateValue", String.class, String.class, String.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(svc, type, expected, actual);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("regex: find-Match")
    void testRegexFind() {
        assertTrue(invoke("regex", "foo", "xxfooxx"));
        assertFalse(invoke("regex", "bar", "xxfooxx"));
        // leeres Pattern -> immer PASS laut Implementierung
        assertTrue(invoke("regex", "", "irgendwas"));
    }

    @Test
    @DisplayName("fullregex: vollständiger Match")
    void testFullRegex() {
        assertTrue(invoke("fullregex", "^foo$", "foo"));
        assertFalse(invoke("fullregex", "^foo$", "foobar"));
        // leeres expected => act muss leer sein
        assertTrue(invoke("fullregex", "", ""));
        assertFalse(invoke("fullregex", "", "abc"));
    }

    @Test
    @DisplayName("contains")
    void testContains() {
        assertTrue(invoke("contains", "Teil", "Das ist ein Teilstring"));
        assertFalse(invoke("contains", "XYZ", "Das ist ein Teilstring"));
    }

    @Test
    @DisplayName("equals")
    void testEquals() {
        assertTrue(invoke("equals", "abc", "abc"));
        assertFalse(invoke("equals", "abc", "abcd"));
    }

    @Test
    @DisplayName("starts / ends")
    void testStartsEnds() {
        assertTrue(invoke("starts", "Hello", "Hello World"));
        assertFalse(invoke("starts", "World", "Hello World"));
        assertTrue(invoke("ends", "World", "Hello World"));
        assertFalse(invoke("ends", "Hello", "Hello World"));
    }

    @Test
    @DisplayName("range numerisch")
    void testRange() {
        assertTrue(invoke("range", "1:10", "5"));
        assertTrue(invoke("range", "1:10", "1"));
        assertTrue(invoke("range", "1:10", "10"));
        assertFalse(invoke("range", "1:10", "0"));
        assertFalse(invoke("range", "1:10", "11"));
        assertFalse(invoke("range", "1:10", "abc")); // parse error
        assertFalse(invoke("range", "bad", "5"));    // parse error expected
    }

    @Test
    @DisplayName("len Varianten")
    void testLen() {
        assertTrue(invoke("len", "3", "abc"));
        assertFalse(invoke("len", "3", "ab"));
        assertTrue(invoke("len", ">=3", "abcdef"));
        assertTrue(invoke("len", "<=3", "abc"));
        assertFalse(invoke("len", "<=3", "abcd"));
        assertFalse(invoke("len", ">=3", "ab"));
        assertFalse(invoke("len", "xyz", "abc")); // parse error
    }

    @Test
    @DisplayName("unknown validator type liefert fail")
    void testUnknown() {
        assertFalse(invoke("unbekannt", "x", "x"));
    }
}


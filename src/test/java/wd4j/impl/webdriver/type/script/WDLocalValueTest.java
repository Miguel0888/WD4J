package wd4j.impl.webdriver.type.script;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WDLocalValueTest {

    @Disabled
    @Test
    public void testFromObject() {
        Map<String, Object> args = new HashMap<>();
        args.put("name", "John");
        args.put("active", true);
        args.put("score", 42);
        // Java 8 compatible: args.put("tags", List.of("one", "two"));
        args.put("tags", new String[]{"one", "two"});
        args.put("created", Date.from(Instant.parse("2023-01-01T00:00:00Z")));

        WDLocalValue local = WDLocalValue.fromObject(args);
        assertEquals("object", local.getType());
    }
}
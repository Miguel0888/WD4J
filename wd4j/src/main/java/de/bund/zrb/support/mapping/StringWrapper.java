package de.bund.zrb.support.mapping;

/**
 * Indicates that the class should be handled as wrapper by GSON
 */
public interface StringWrapper {
    String value(); // Java 14+ (Record Type) compliant
}

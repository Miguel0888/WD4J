package de.bund.zrb.service;

/**
 * Declare a Runnable that may throw checked Exceptions.
 * Keep it minimal to stay Java 8 compatible.
 */
@FunctionalInterface
interface ThrowingRunnable {
    void run() throws Exception;
}

package org.buildobjects.process;

/**
 * Signals a problem starting an external process (missing binary)
 */
public class StartupException extends RuntimeException {
    public StartupException(String s, Throwable throwable) {
        super(s, throwable);
    }
}

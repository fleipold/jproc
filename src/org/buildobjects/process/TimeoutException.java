package org.buildobjects.process;


/** Signals a timeout */
public class TimeoutException extends RuntimeException{
    TimeoutException(String s, long timeout) {
        super("Process '" + s + "' timed out after " + timeout + "ms.");
    }
}

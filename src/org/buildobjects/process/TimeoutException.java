package org.buildobjects.process;


public class TimeoutException extends RuntimeException{
    public TimeoutException(String s, long timeout) {
        super("Process '" + s + "' timed out after " + timeout + "ms.");
    }
}

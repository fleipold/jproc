package org.buildobjects.process;

/**
 * This class ${end}
 */
public class ExceptionEvent implements ExecutionEvent {
    public final Throwable t;

    public ExceptionEvent(Throwable t) {
        this.t = t;
    }
}

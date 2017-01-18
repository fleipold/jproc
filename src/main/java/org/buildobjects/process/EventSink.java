package org.buildobjects.process;

interface EventSink {
    void dispatch(ExecutionEvent event);
}

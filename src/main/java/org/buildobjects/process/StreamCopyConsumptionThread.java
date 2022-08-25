package org.buildobjects.process;

import java.io.InputStream;
import java.io.OutputStream;

import static org.buildobjects.process.ExecutionEvent.EXCEPTION_IN_STREAM_HANDLING;

/**
 *
 */
class StreamCopyConsumptionThread implements OutputConsumptionThread {
    private final OutputStream stdout;
    private Thread thread;
    private Throwable throwable;

    private final EventSink eventSink;

    public StreamCopyConsumptionThread(OutputStream stdout, EventSink eventSink) {
        this.stdout = stdout;
        this.eventSink = eventSink;
    }

    public void startConsumption(final InputStream inputStream) {
        this.thread = new Thread(new Runnable() {
            public void run() {
                try {
                    new StreamCopyRunner(inputStream, stdout, false).run();
                } catch (Throwable t) {
                    if (!thread.isInterrupted()) {
                        StreamCopyConsumptionThread.this.throwable = t;
                        eventSink.dispatch(EXCEPTION_IN_STREAM_HANDLING);
                    }
                }
            }
        });
        this.thread.start();
    }

    public void join() throws InterruptedException {
        thread.join();

    }

    public void interrupt() {
        thread.interrupt();
    }

    public Throwable getThrowable() {
        return throwable;
    }
}

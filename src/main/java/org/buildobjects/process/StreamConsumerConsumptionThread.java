package org.buildobjects.process;

import java.io.IOException;
import java.io.InputStream;

import static org.buildobjects.process.ExecutionEvent.EXCEPTION_IN_STREAM_HANDLING;

/**
 * This class ${end}
 */
class StreamConsumerConsumptionThread implements OutputConsumptionThread {
    private final EventSink eventSink;
    private final StreamConsumer stdout;
    private Thread thread;
    private Throwable throwable;

    public StreamConsumerConsumptionThread(EventSink eventSink, StreamConsumer stdout) {
        this.eventSink = eventSink;
        this.stdout = stdout;
    }

    public void startConsumption(final InputStream inputStream) {
        this.thread = new Thread(new Runnable() {


            public void run() {
                try {
                    stdout.consume(inputStream);

                } catch (Throwable t) {
                    StreamConsumerConsumptionThread.this.throwable = t;
                    eventSink.dispatch(EXCEPTION_IN_STREAM_HANDLING);
                }
            }
        });
        thread.start();
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

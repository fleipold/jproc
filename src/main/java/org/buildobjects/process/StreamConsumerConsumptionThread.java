package org.buildobjects.process;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class ${end}
 */
public class StreamConsumerConsumptionThread implements OutputConsumptionThread {
    private final EventSink eventSink;
    private final StreamConsumer stdout;
    private Thread thread;

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
                    eventSink.dispatch(new ExceptionEvent(t));
                }
            }
        });
        thread.start();
    }

    public void join() throws InterruptedException {

        thread.join();
    }
}

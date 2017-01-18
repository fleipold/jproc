package org.buildobjects.process;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import static org.buildobjects.process.ExecutionEvent.EXCEPTION_IN_STREAM_HANDLING;

class ByteArrayConsumptionThread implements OutputConsumptionThread {

    private Thread thread;
    private Throwable throwable;

    private byte[] bytes;
    private final EventSink eventSink;

    ByteArrayConsumptionThread(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    public byte[] getBytes() {
        return bytes;
    }


    public void startConsumption(final InputStream inputStream) {
        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    bytes = IOUtils.toByteArray(inputStream);
                } catch (Throwable t) {
                    ByteArrayConsumptionThread.this.throwable = t;
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

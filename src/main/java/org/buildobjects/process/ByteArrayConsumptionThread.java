package org.buildobjects.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.buildobjects.process.ExecutionEvent.EXCEPTION_IN_STREAM_HANDLING;

class ByteArrayConsumptionThread implements OutputConsumptionThread {

    private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

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
                    bytes = toByteArray(inputStream);
                } catch (Throwable t) {
                    ByteArrayConsumptionThread.this.throwable = t;
                    eventSink.dispatch(EXCEPTION_IN_STREAM_HANDLING);
                }
            }
        });
        thread.start();
    }

    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int n;
        while (-1 != (n = inputStream.read(buffer))) {
            output.write(buffer, 0, n);
        }

        return output.toByteArray();
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

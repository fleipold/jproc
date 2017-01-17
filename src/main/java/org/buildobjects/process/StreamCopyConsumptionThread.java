package org.buildobjects.process;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class ${end}
 */
public class StreamCopyConsumptionThread implements OutputConsumptionThread {
    private final OutputStream stdout;
    private Thread thread;

    public StreamCopyConsumptionThread(OutputStream stdout) {
        this.stdout = stdout;
    }

    public void startConsumption(InputStream inputStream) {
        this.thread = new Thread(new StreamCopyRunner(inputStream, stdout, false));
        this.thread.start();
    }

    public void join() throws InterruptedException {
        thread.join();

    }
}

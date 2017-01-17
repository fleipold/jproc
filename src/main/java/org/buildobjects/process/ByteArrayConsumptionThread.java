package org.buildobjects.process;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class ByteArrayConsumptionThread implements OutputConsumptionThread {

    private Thread thread;

    public byte[] getBytes() {
        return bytes;
    }

    private byte[] bytes;

    public void startConsumption(final InputStream inputStream) {
        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    bytes = IOUtils.toByteArray(inputStream);
                } catch (IOException e) {
                    throw new RuntimeException("", e);
                }
            }
        });
        thread.start();
    }


    public void join() throws InterruptedException {
        thread.join();
    }
}

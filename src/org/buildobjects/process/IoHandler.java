package org.buildobjects.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class IoHandler {
    Thread errConsumer;
    Thread inFeeder;
    Thread outConsumer;


    IoHandler(InputStream stdin, OutputStream stdout, OutputStream stderr, Process process) {
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();
        OutputStream in = process.getOutputStream();

        outConsumer = startConsumption(stdout, out, false);
        errConsumer = startConsumption(stderr, err, false);
        inFeeder = startConsumption(in, stdin, true);
    }

    void joinConsumption() throws InterruptedException {
        outConsumer.join();
        errConsumer.join();
        inFeeder.join();
    }

    void cancelConsumption() {
        outConsumer.stop();
        errConsumer.stop();
        inFeeder.stop();
    }

    Thread startConsumption(OutputStream stdout, InputStream out, boolean closeAfterWriting) {
        Thread consumer;
        consumer = new Thread(new StreamCopyRunner(out, stdout, closeAfterWriting));
        consumer.start();
        return consumer;
    }

    private class StreamCopyRunner implements Runnable {
        InputStream in;
        OutputStream out;
        private boolean closeWriter;
        private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

        private StreamCopyRunner(InputStream in, OutputStream out, boolean closeWriter) {
            this.in = in;
            this.out = out;
            this.closeWriter = closeWriter;
        }

        public void run() {
            if (in == null || out == null) {
                return;
            }

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            try {

                while (-1 != (n = in.read(buffer))) {
                    out.write(buffer, 0, n);
                }
                if (closeWriter) {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("", e);
            }
        }
    }

}
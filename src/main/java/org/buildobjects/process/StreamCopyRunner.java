package org.buildobjects.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class ${end}
 */
class StreamCopyRunner implements Runnable {
    InputStream in;
    OutputStream out;
    private boolean closeStreamAfterConsumingInput;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    StreamCopyRunner(InputStream in, OutputStream out, boolean closeStreamAfterConsumingInput) {
        this.in = in;
        this.out = out;
        this.closeStreamAfterConsumingInput = closeStreamAfterConsumingInput;
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
            if (closeStreamAfterConsumingInput) {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }
}

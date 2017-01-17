package org.buildobjects.process;

import java.io.IOException;
import java.io.InputStream;

public interface StreamConsumer {

    /**
     * Consume something until the bitter end.
     *
     * @param stream stream to be consumed.
     */
    void consume(InputStream stream) throws IOException;
}

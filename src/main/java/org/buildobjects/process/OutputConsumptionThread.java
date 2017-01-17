package org.buildobjects.process;

import java.io.Closeable;
import java.io.InputStream;


public interface OutputConsumptionThread {

    void startConsumption(InputStream inputStream);

    void join() throws InterruptedException;
}

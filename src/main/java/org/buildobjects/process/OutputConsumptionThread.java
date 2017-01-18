package org.buildobjects.process;

import java.io.InputStream;


interface OutputConsumptionThread {

    void startConsumption(InputStream inputStream);

    void join() throws InterruptedException;

    void interrupt();

    Throwable getThrowable();
}

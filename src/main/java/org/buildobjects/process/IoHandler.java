package org.buildobjects.process;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

class IoHandler {
    private final OutputConsumptionThread stdout;
    private final OutputConsumptionThread stderr;
    Thread inFeeder;


    IoHandler(InputStream stdin, OutputConsumptionThread stdout, OutputConsumptionThread stderr, Process process) {
        this.stdout = stdout;
        this.stderr = stderr;
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();
        OutputStream in = process.getOutputStream();

        stdout.startConsumption(out);
        stderr.startConsumption(err);
        inFeeder = startConsumption(in, stdin, true);
    }

    List<Throwable> joinConsumption() throws InterruptedException {
        inFeeder.join();
        stdout.join();
        stderr.join();

        List<Throwable> exceptions = new ArrayList<Throwable>();

        if (stdout.getThrowable() != null) {
            exceptions.add(stdout.getThrowable());
        }

        if (stderr.getThrowable() != null) {
            exceptions.add(stderr.getThrowable());
        }

        return exceptions;

    }

    void cancelConsumption() {
        inFeeder.interrupt();
        stdout.interrupt();
        stderr.interrupt();
    }

    Thread startConsumption(OutputStream stdout, InputStream out, boolean closeAfterWriting) {
        Thread consumer;
        consumer = new Thread(new StreamCopyRunner(out, stdout, closeAfterWriting));
        consumer.start();
        return consumer;
    }

}
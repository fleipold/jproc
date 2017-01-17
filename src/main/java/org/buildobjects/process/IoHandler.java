package org.buildobjects.process;

import java.io.*;

class IoHandler {
    Thread inFeeder;


    IoHandler(InputStream stdin, OutputConsumptionThread    stdout, OutputConsumptionThread stderr, Process process) {
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();
        OutputStream in = process.getOutputStream();

        stdout.startConsumption(out);
        stderr.startConsumption(err);
        inFeeder = startConsumption(in, stdin, true);
    }

    void joinConsumption() throws InterruptedException {
        inFeeder.join();
    }

    void cancelConsumption() {
        inFeeder.stop();
    }

    Thread startConsumption(OutputStream stdout, InputStream out, boolean closeAfterWriting) {
        Thread consumer;
        consumer = new Thread(new StreamCopyRunner(out, stdout, closeAfterWriting));
        consumer.start();
        return consumer;
    }

}
package org.buildobjects.process;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class Proc {

    private Thread errConsumer;
    private Thread inFeeder;
    private Thread outConsumer;

    private Process process;
    private int exitValue;

    private long executionTime;

    private ByteArrayOutputStream err = new ByteArrayOutputStream();
    private String command;
    private List<String> args;

    public Proc(String command, List<String> args, Map<String, String> env, InputStream stdin, OutputStream stdout, File directory, long timeout) {
        this.command = command;
        this.args = args;
        String[] envArray = getEnv(env);

        String[] cmdArray = concatenateCmdArgs();
        long t1 = System.currentTimeMillis();
        try {
            process = Runtime.getRuntime().exec(cmdArray, envArray, directory);
            initializeConsumption(stdin, stdout, err);
        } catch (IOException e) {
            throw new RuntimeException("Could not startup process '"+ toString() + "'.", e);
        }


        try {
            final Semaphore done = new Semaphore(1);
            done.acquire();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        exitValue = process.waitFor();
                        done.release();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("", e);
                    }
                }
            }).start();


            boolean success = done.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (!success){
                process.destroy();
                outConsumer.stop();
                errConsumer.stop();
                inFeeder.stop();
                throw new TimeoutException(toString(), timeout);
            }

            executionTime = System.currentTimeMillis() - t1;
            if (exitValue != 0){
                throw new ExternalProcessFailureException(toString(), exitValue, err.toString(), executionTime);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }

    }

    private String[] getEnv(Map<String, String> env) {
        if (env == null || env.isEmpty()){
            return null;
        }
        String[] retValue = new String[env.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : env.entrySet()){
            retValue[i++] = entry.getKey() + "=" + entry.getValue();

        }
        return retValue;

    }

    private String[] concatenateCmdArgs() {
        List<String> cmd = new ArrayList<String>();
        cmd.add(command);
        cmd.addAll(args);
        return cmd.toArray(new String[cmd.size()]);
    }

    private void initializeConsumption(InputStream stdin, OutputStream stdout, OutputStream stderr) {
        InputStream out = process.getInputStream();
        InputStream err = process.getErrorStream();
        OutputStream in = process.getOutputStream();

        outConsumer = startConsumption(stdout, out, false);
        errConsumer = startConsumption(stderr, err, false);
        inFeeder = startConsumption(in, stdin, true);
    }


    private Thread startConsumption(OutputStream stdout, InputStream out, boolean closeAfterWriting) {
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
            if (in == null || out == null){
                return;
            }
            
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            try {

                while (-1 != (n = in.read(buffer))) {
                    out.write(buffer, 0, n);
                }
                if (closeWriter){
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("", e);
            }
        }
    }

    @Override
    public String toString() {
        return command + " " + StringUtils.join(
                    CollectionUtils.collect(args, new Transformer() {
                        public Object transform(Object o) {
                            if (((String) o).contains(" ")){
                                return "\"" + o + "\"";
                            }
                            else {
                                return o;
                            }
                        }
                    })," ");
    }

    public int getExitValue() {
        return exitValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
